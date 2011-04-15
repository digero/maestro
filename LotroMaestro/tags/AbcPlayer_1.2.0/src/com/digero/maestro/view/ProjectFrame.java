package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.prefs.Preferences;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.digero.common.abc.TimingInfo;
import com.digero.common.icons.IconLoader;
import com.digero.common.midi.DrumFilterTransceiver;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.PanGenerator;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.midi.SynthesizerFactory;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.FileFilterDropListener;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.ParseException;
import com.digero.common.util.Util;
import com.digero.common.view.PlayControlPanel;
import com.digero.common.view.SongPositionBar;
import com.digero.maestro.MaestroMain;
import com.digero.maestro.abc.AbcConversionException;
import com.digero.maestro.abc.AbcMetadataSource;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.abc.PartAutoNumberer;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.util.ListModelWrapper;

@SuppressWarnings("serial")
public class ProjectFrame extends JFrame implements TableLayoutConstants, AbcMetadataSource, ICompileConstants {
	private static final int HGAP = 4, VGAP = 4;
	private static final double[] LAYOUT_COLS = new double[] {
			180, FILL
	};
	private static final double[] LAYOUT_ROWS = new double[] {
		FILL
	};

	private Preferences prefs = Preferences.userNodeForPackage(MaestroMain.class);

	private File saveFile;
	private SequenceInfo sequenceInfo;
	private SequencerWrapper sequencer;
	private SequencerWrapper abcSequencer;
	private DefaultListModel parts = new DefaultListModel();
	private ListModelWrapper<AbcPart> partsWrapper = new ListModelWrapper<AbcPart>(parts);
	private PartAutoNumberer partAutoNumberer;

	private JPanel content;
	private JTextField songTitleField;
	private JTextField titleTagField;
	private JTextField composerField;
	private JTextField transcriberField;
	private JSpinner transposeSpinner;
	private JSpinner tempoSpinner;
	private JFormattedTextField keySignatureField;
	private JFormattedTextField timeSignatureField;
	private JButton exportButton;

	private JList partsList;
	private JButton newPartButton;
	private JButton deletePartButton;

	private PartPanel partPanel;

	private SongPositionBar abcPositionBar;
	private JButton abcPlayButton;
	private JButton abcStopButton;
	private Icon abcPlayIcon;
	private Icon abcPauseIcon;

	private long abcPreviewStartMicros = 0;
	private boolean echoingPosition = false;

	private MainSequencerListener mainSequencerListener;
	private AbcSequencerListener abcSequencerListener;

	public ProjectFrame() {
		super("LOTRO Maestro");
		setMinimumSize(new Dimension(512, 384));
		Util.initWinBounds(this, prefs.node("window"), 800, 600);

		partAutoNumberer = new PartAutoNumberer(prefs.node("partAutoNumberer"), partsWrapper);

		try {
			this.sequencer = new SequencerWrapper(new DrumFilterTransceiver());

			Sequencer abcSeq = MidiSystem.getSequencer(false);
			Synthesizer lotroSynth = null;
			try {
				lotroSynth = SynthesizerFactory.getLotroSynthesizer();
			}
			catch (InvalidMidiDataException e) {
				JOptionPane.showMessageDialog(null, "Failed to load LotRO Instrument sounds.\n"
						+ "ABC Preview will not use LotRO instruments.\n\n" + "Error details:\n" + e.getMessage(),
						"Failed to initialize MIDI sequencer.", JOptionPane.ERROR_MESSAGE);
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Failed to load LotRO Instrument sounds.\n"
						+ "ABC Preview will not use LotRO instruments.\n\n" + "Error details:\n" + e.getMessage(),
						"Failed to initialize MIDI sequencer.", JOptionPane.ERROR_MESSAGE);
			}

			Transmitter transmitter = abcSeq.getTransmitter();
			Receiver receiver = (lotroSynth == null) ? MidiSystem.getReceiver() : lotroSynth.getReceiver();
			transmitter.setReceiver(receiver);
			abcSeq.open();
			this.abcSequencer = new SequencerWrapper(abcSeq, transmitter, receiver);
		}
		catch (MidiUnavailableException e) {
			JOptionPane.showMessageDialog(null, "Failed to initialize MIDI sequencer.\nThe program will now exit.",
					"Failed to initialize MIDI sequencer", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		partPanel = new PartPanel(sequencer, partAutoNumberer);

		TableLayout tableLayout = new TableLayout(LAYOUT_COLS, LAYOUT_ROWS);
		tableLayout.setHGap(HGAP);
		tableLayout.setVGap(VGAP);

		content = new JPanel(tableLayout, false);
		setContentPane(content);

		songTitleField = new JTextField();
		composerField = new JTextField();
		titleTagField = new JTextField(prefs.get("titleTag", ""));
		titleTagField.getDocument().addDocumentListener(new PrefsDocumentListener(prefs, "titleTag"));
		transcriberField = new JTextField(prefs.get("transcriber", ""));
		transcriberField.getDocument().addDocumentListener(new PrefsDocumentListener(prefs, "transcriber"));

		keySignatureField = new MyFormattedTextField(KeySignature.C_MAJOR, 5);
		keySignatureField.setToolTipText("<html>Adjust the key signature of the ABC file. "
				+ "This only affects the display, not the sound of the exported file.<br>"
				+ "Examples: C maj, Eb maj, F# min</html>");

		timeSignatureField = new MyFormattedTextField(TimeSignature.FOUR_FOUR, 5);
		timeSignatureField.setToolTipText("<html>Adjust the time signature of the ABC file. "
				+ "This only affects the display, not the sound of the exported file.<br>"
				+ "Examples: 4/4, 3/8, 2/2</html>");

		transposeSpinner = new JSpinner(new SpinnerNumberModel(0, -48, 48, 1));
		transposeSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int transpose = getTranspose();
				for (int i = 0; i < parts.getSize(); i++) {
					AbcPart part = (AbcPart) parts.getElementAt(i);
					part.setBaseTranspose(transpose);
				}
			}
		});

		tempoSpinner = new JSpinner(new SpinnerNumberModel(120, 8, 960, 2));

		exportButton = new JButton("Export ABC");
		exportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportAbc();
			}
		});

		partsList = new JList(parts);
		partsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		partsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				AbcPart abcPart = (AbcPart) partsList.getSelectedValue();
				sequencer.getDrumFilter().setAbcPart(abcPart);
				partPanel.setAbcPart(abcPart);
			}
		});

		JScrollPane partsListScrollPane = new JScrollPane(partsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		newPartButton = new JButton("New Part");
		newPartButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbcPart newPart = new AbcPart(ProjectFrame.this.sequenceInfo, getTranspose(), ProjectFrame.this);
				newPart.addAbcListener(abcPartListener);
				parts.addElement(newPart);
				partAutoNumberer.onPartAdded(newPart);
				Collections.sort(partsWrapper, partNumberComparator);
				partsList.clearSelection();
				partsList.setSelectedValue(newPart, true);
				updateAbcButtons();
			}
		});

		deletePartButton = new JButton("Delete");
		deletePartButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = partsList.getSelectedIndex();
				AbcPart oldPart = (AbcPart) parts.remove(idx);
				if (idx > 0)
					partsList.setSelectedIndex(idx - 1);
				else if (parts.size() > 0)
					partsList.setSelectedIndex(0);
				partAutoNumberer.onPartDeleted(oldPart);
				oldPart.dispose();
				updateAbcButtons();
				refreshPreviewSequence(false);
			}
		});

		TableLayout songInfoLayout = new TableLayout(new double[] {
				PREFERRED, FILL
		}, new double[] {
				PREFERRED, PREFERRED, PREFERRED, PREFERRED
		});
		songInfoLayout.setHGap(HGAP);
		songInfoLayout.setVGap(VGAP);
		JPanel songInfoPanel = new JPanel(songInfoLayout);
		songInfoPanel.add(new JLabel("T:"), "0, 0");
		songInfoPanel.add(new JLabel("[]"), "0, 1");
		songInfoPanel.add(new JLabel("C:"), "0, 2");
		songInfoPanel.add(new JLabel("Z:"), "0, 3");
		songInfoPanel.add(songTitleField, "1, 0");
		songInfoPanel.add(titleTagField, "1, 1");
		songInfoPanel.add(composerField, "1, 2");
		songInfoPanel.add(transcriberField, "1, 3");
		songInfoPanel.setBorder(BorderFactory.createTitledBorder("Song Info"));

		JPanel partsButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
		partsButtonPanel.add(newPartButton);
		partsButtonPanel.add(deletePartButton);

		JPanel partsListPanel = new JPanel(new BorderLayout(HGAP, VGAP));
		partsListPanel.setBorder(BorderFactory.createTitledBorder("Song Parts"));
		partsListPanel.add(partsButtonPanel, BorderLayout.NORTH);
		partsListPanel.add(partsListScrollPane, BorderLayout.CENTER);

		abcPositionBar = new SongPositionBar(abcSequencer);
		abcPlayIcon = new ImageIcon(IconLoader.class.getResource("play_yellow.png"));
		abcPauseIcon = new ImageIcon(IconLoader.class.getResource("pause.png"));
		Icon abcStopIcon = new ImageIcon(IconLoader.class.getResource("stop.png"));
		abcPlayButton = new JButton(abcPlayIcon);
		abcPlayButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (abcSequencer.isRunning()) {
					abcSequencer.stop();
				}
				else {
					refreshPreviewSequence(true);
					abcSequencer.start();
				}
			}
		});
		abcStopButton = new JButton(abcStopIcon);
		abcStopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abcSequencer.reset(false);
				sequencer.reset(false);
			}
		});
		JPanel abcPreviewControls = new JPanel(new TableLayout(new double[] {
				HGAP, 0.50, PREFERRED, PREFERRED, 0.50, HGAP
		}, new double[] {
				VGAP, PREFERRED, VGAP, PREFERRED, VGAP
		}));
		abcPreviewControls.add(abcPositionBar, "1, 1, 4, 1");
		abcPreviewControls.add(abcPlayButton, "2, 3");
		abcPreviewControls.add(abcStopButton, "3, 3");
		updateAbcButtons();

		TableLayout settingsLayout = new TableLayout(new double[] {
				/* Cols */PREFERRED, PREFERRED, FILL
		}, new double[] {
				/* Rows */PREFERRED, PREFERRED, PREFERRED, PREFERRED, PREFERRED, PREFERRED
		});
		settingsLayout.setVGap(VGAP);
		settingsLayout.setHGap(HGAP);

		JPanel settingsPanel = new JPanel(settingsLayout);
		settingsPanel.setBorder(BorderFactory.createTitledBorder("Export Settings"));
		settingsPanel.add(new JLabel("Transpose:"), "0, 0");
		settingsPanel.add(new JLabel("Tempo:"), "0, 1");
		settingsPanel.add(new JLabel("Meter:"), "0, 2");
		if (SHOW_KEY_FIELD)
			settingsPanel.add(new JLabel("Key:"), "0, 3");
		settingsPanel.add(transposeSpinner, "1, 0");
		settingsPanel.add(tempoSpinner, "1, 1");
		settingsPanel.add(timeSignatureField, "1, 2, 2, 2, L, F");
		if (SHOW_KEY_FIELD)
			settingsPanel.add(keySignatureField, "1, 3, 2, 3, L, F");
		settingsPanel.add(exportButton, "0, 4, 2, 4, C, F");
		settingsPanel.add(abcPreviewControls, "0, 5, 2, 5");

		if (!SHOW_TEMPO_SPINNER)
			tempoSpinner.setEnabled(false);
		if (!SHOW_METER_TEXTBOX)
			timeSignatureField.setEnabled(false);
		if (!SHOW_KEY_FIELD)
			keySignatureField.setEnabled(false);

		PlayControlPanel playControlPanel = new PlayControlPanel(sequencer, "play_blue", "pause_blue", "stop");

		JPanel abcPartsAndSettings = new JPanel(new BorderLayout(HGAP, VGAP));
		abcPartsAndSettings.add(songInfoPanel, BorderLayout.NORTH);
		abcPartsAndSettings.add(partsListPanel, BorderLayout.CENTER);
		abcPartsAndSettings.add(settingsPanel, BorderLayout.SOUTH);

		JPanel midiPartsAndControls = new JPanel(new BorderLayout(HGAP, VGAP));
		midiPartsAndControls.add(partPanel, BorderLayout.CENTER);
		midiPartsAndControls.add(playControlPanel, BorderLayout.SOUTH);
		midiPartsAndControls.setBorder(BorderFactory.createTitledBorder("Part Settings"));

		add(abcPartsAndSettings, "0, 0");
		add(midiPartsAndControls, "1, 0");

		final FileFilterDropListener dropListener = new FileFilterDropListener(false, "mid", "midi", "abc", "txt");
		dropListener.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openSong(dropListener.getDroppedFile());
			}
		});
		new DropTarget(this, dropListener);

		mainSequencerListener = new MainSequencerListener();
		sequencer.addChangeListener(mainSequencerListener);

		abcSequencerListener = new AbcSequencerListener();
		abcSequencer.addChangeListener(abcSequencerListener);

		parts.addListDataListener(new ListDataListener() {
			public void intervalRemoved(ListDataEvent e) {
				updateAbcButtons();
			}

			public void intervalAdded(ListDataEvent e) {
				updateAbcButtons();
			}

			public void contentsChanged(ListDataEvent e) {
				updateAbcButtons();
			}
		});

		initMenu();
	}

	private void initMenu() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu toolsMenu = menuBar.add(new JMenu(" Tools "));

		JMenuItem settingsItem = toolsMenu.add(new JMenuItem("Settings..."));
		settingsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SettingsDialog dialog = new SettingsDialog(ProjectFrame.this, partAutoNumberer.getSettingsCopy());
				dialog.setVisible(true);
				if (dialog.isSuccess()) {
					if (dialog.isNumbererSettingsChanged()) {
						partAutoNumberer.setSettings(dialog.getNumbererSettings());
						partAutoNumberer.renumberAllParts();
					}
					partPanel.settingsChanged();
				}
				dialog.dispose();
			}
		});
	}

	private class MainSequencerListener implements SequencerListener {
		public void propertyChanged(SequencerEvent evt) {
			if (evt.getProperty() == SequencerProperty.IS_RUNNING) {
				if (sequencer.isRunning())
					abcSequencer.stop();
			}
			else if (!echoingPosition) {
				try {
					echoingPosition = true;
					if (evt.getProperty() == SequencerProperty.POSITION) {
						long pos = sequencer.getPosition() - abcPreviewStartMicros;
						abcSequencer.setPosition(Util.clamp(pos, 0, abcSequencer.getLength()));
					}
					else if (evt.getProperty() == SequencerProperty.DRAG_POSITION) {
						long pos = sequencer.getDragPosition() - abcPreviewStartMicros;
						abcSequencer.setDragPosition(Util.clamp(pos, 0, abcSequencer.getLength()));
					}
					else if (evt.getProperty() == SequencerProperty.IS_DRAGGING) {
						abcSequencer.setDragging(sequencer.isDragging());
					}
				}
				finally {
					echoingPosition = false;
				}
			}
		}
	}

	private class AbcSequencerListener implements SequencerListener {
		public void propertyChanged(SequencerEvent evt) {
			updateAbcButtons();
			if (evt.getProperty() == SequencerProperty.IS_RUNNING) {
				if (abcSequencer.isRunning())
					sequencer.stop();
			}
			else if (!echoingPosition) {
				try {
					echoingPosition = true;
					if (evt.getProperty() == SequencerProperty.POSITION) {
						long pos = abcSequencer.getPosition() + abcPreviewStartMicros;
						sequencer.setPosition(Util.clamp(pos, 0, sequencer.getLength()));
					}
					else if (evt.getProperty() == SequencerProperty.DRAG_POSITION) {
						long pos = abcSequencer.getDragPosition() + abcPreviewStartMicros;
						sequencer.setDragPosition(Util.clamp(pos, 0, sequencer.getLength()));
					}
					else if (evt.getProperty() == SequencerProperty.IS_DRAGGING) {
						sequencer.setDragging(abcSequencer.isDragging());
					}
				}
				finally {
					echoingPosition = false;
				}
			}
		}
	}

	private static class PrefsDocumentListener implements DocumentListener {
		private Preferences prefs;
		private String prefName;

		public PrefsDocumentListener(Preferences prefs, String prefName) {
			this.prefs = prefs;
			this.prefName = prefName;
		}

		private void updatePrefs(Document doc) {
			String txt;
			try {
				txt = doc.getText(0, doc.getLength());
			}
			catch (BadLocationException e) {
				txt = "";
			}
			prefs.put(prefName, txt);
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			updatePrefs(e.getDocument());
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			updatePrefs(e.getDocument());
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			updatePrefs(e.getDocument());
		}
	}

	private void updateAbcButtons() {
		abcPlayButton.setEnabled(abcSequencer.isLoaded() || parts.size() > 0);
		abcPlayButton.setIcon(abcSequencer.isRunning() ? abcPauseIcon : abcPlayIcon);
		abcStopButton.setEnabled(abcSequencer.isLoaded()
				&& (abcSequencer.isRunning() || abcSequencer.getPosition() != 0));

		newPartButton.setEnabled(sequenceInfo != null && parts.getSize() < 15);
		deletePartButton.setEnabled(partsList.getSelectedIndex() != -1);
		exportButton.setEnabled(sequenceInfo != null && parts.size() > 0);
	}

	private AbcPartListener abcPartListener = new AbcPartListener() {
		public void abcPartChanged(AbcPartEvent e) {
			if (e.isPartNumber()) {
				int idx;
				Object selected = partsList.getSelectedValue();
				Collections.sort(partsWrapper, partNumberComparator);
				if (selected != null) {
					idx = parts.indexOf(selected);
					if (idx >= 0)
						partsList.setSelectedIndex(idx);
				}
			}

			partsList.repaint();

			if (e.isPreviewRelated() && abcSequencer.isRunning()) {
				refreshPreviewSequence(false);
			}
		}
	};

	public int getTranspose() {
		return (Integer) transposeSpinner.getValue();
	}

	public int getTempo() {
		return (Integer) tempoSpinner.getValue();
	}

	public KeySignature getKeySignature() {
		if (SHOW_KEY_FIELD)
			return (KeySignature) keySignatureField.getValue();
		else
			return KeySignature.C_MAJOR;
	}

	public TimeSignature getTimeSignature() {
		return (TimeSignature) timeSignatureField.getValue();
	}

	public int calcNextPartNumber() {
		int size = parts.getSize();
		outerLoop: for (int n = 1; n <= size; n++) {
			for (int i = 0; i < size; i++) {
				if (n == ((AbcPart) parts.getElementAt(i)).getPartNumber())
					continue outerLoop;
			}
			return n;
		}
		return size + 1;
	}

	private Comparator<AbcPart> partNumberComparator = new Comparator<AbcPart>() {
		public int compare(AbcPart p1, AbcPart p2) {
			int base1 = partAutoNumberer.getFirstNumber(p1.getInstrument());
			int base2 = partAutoNumberer.getFirstNumber(p2.getInstrument());

			if (base1 != base2)
				return base1 - base2;
			return p1.getPartNumber() - p2.getPartNumber();
		}
	};

	@SuppressWarnings("unused")
	private void close() {
		for (int i = 0; i < parts.size(); i++) {
			AbcPart part = (AbcPart) parts.get(i);
			part.dispose();
		}
		parts.clear();

		saveFile = null;
		sequencer.clearSequence();
		abcSequencer.clearSequence();
		sequencer.reset(false);
		abcSequencer.reset(false);
		abcPreviewStartMicros = 0;
	}

	private void openSong(File midiFile) {
		for (int i = 0; i < parts.size(); i++) {
			AbcPart part = (AbcPart) parts.get(i);
			part.dispose();
		}
		parts.clear();

		saveFile = null;
		sequencer.stop();
		abcSequencer.reset(false);
		abcPreviewStartMicros = 0;

		try {
			String fileName = midiFile.getName().toLowerCase();
			boolean isAbc = fileName.endsWith(".abc") || fileName.endsWith(".txt");
			sequenceInfo = new SequenceInfo(midiFile, isAbc);

			sequencer.setSequence(null);
			sequencer.reset(true);
			sequencer.setSequence(sequenceInfo.getSequence());
			sequencer.setTickPosition(sequenceInfo.calcFirstNoteTick());
			songTitleField.setText(sequenceInfo.getTitle());
			composerField.setText("");
			transposeSpinner.setValue(0);
			tempoSpinner.setValue(sequenceInfo.getTempoBPM());
			keySignatureField.setValue(sequenceInfo.getKeySignature());
			timeSignatureField.setValue(sequenceInfo.getTimeSignature());

			updateAbcButtons();
			newPartButton.doClick();

			sequencer.start();
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, "Failed to open " + midiFile.getName() + ":\n" + e.getMessage(),
					"Error opening MIDI file", JOptionPane.ERROR_MESSAGE);
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Failed to open " + midiFile.getName() + ":\n" + e.getMessage(),
					"Error opening file", JOptionPane.ERROR_MESSAGE);
		}
		catch (ParseException e) {
			JOptionPane.showMessageDialog(this, "Failed to open " + midiFile.getName() + ":\n" + e.getMessage(),
					"Error opening ABC file", JOptionPane.ERROR_MESSAGE);
		}
	}

	// TODO save
//	private void save() {
//		SaveData data = new SaveData();
//		data.setString("project.midiFile", sequenceInfo.getMidiFile().getPath());
//		data.setInt("project.transpose", getTranspose());
//		data.setInt("project.tempo", getTempo());
//		data.setKeySignature("project.keySignature", getKeySignature());
//		data.setTimeSignature("project.timeSignature", getTimeSignature());
//		data.setInt("project.partCount", parts.getSize());
//
//		for (int i = 0; i < parts.getSize(); i++) {
//			AbcPart part = (AbcPart) parts.getElementAt(i);
//
//		}
//	}

	private boolean refreshPreviewPending = false;

	private class RefreshPreviewTask implements Runnable {
		public void run() {
			if (refreshPreviewPending)
				refreshPreviewSequence(true);
		}
	}

	private void refreshPreviewSequence(boolean immediate) {
		if (!immediate) {
			if (!refreshPreviewPending) {
				refreshPreviewPending = true;
				SwingUtilities.invokeLater(new RefreshPreviewTask());
			}
			return;
		}

		refreshPreviewPending = false;
		try {
			TimingInfo tm = new TimingInfo(getTempo(), getTimeSignature());
			Sequence song = new Sequence(Sequence.PPQ, tm.getMidiResolution());

			long startMicros = Long.MAX_VALUE;
			for (int i = 0; i < parts.getSize(); i++) {
				AbcPart part = (AbcPart) parts.get(i);

				long firstNoteStart = part.firstNoteStart();
				if (firstNoteStart < startMicros)
					startMicros = firstNoteStart;
			}

			// Track 0: Title and meta info
			Track track = song.createTrack();
			track.add(MidiFactory.createTrackNameEvent(sequenceInfo.getTitle()));
			track.add(MidiFactory.createTempoEvent(tm.getMPQN(), 0));

			PanGenerator panner = new PanGenerator();
			for (int i = 0; i < parts.getSize(); i++) {
				AbcPart part = (AbcPart) parts.get(i);
				int pan = (parts.getSize() > 1) ? panner.get(part.getInstrument(), part.getTitle())
						: PanGenerator.CENTER;

				part.exportToMidi(song, tm, startMicros, Long.MAX_VALUE, 0, pan);
			}

			long position = abcSequencer.getPosition() + abcPreviewStartMicros - startMicros;
			abcPreviewStartMicros = startMicros;

			boolean running = abcSequencer.isRunning();
			abcSequencer.reset(false);
			abcSequencer.setSequence(song);

			if (position < 0)
				position = 0;

			if (position >= abcSequencer.getLength()) {
				position = 0;
				running = false;
			}

			abcSequencer.setPosition(position);
			abcSequencer.setRunning(running);
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(ProjectFrame.this, e.getMessage(), "Error exporting ABC",
					JOptionPane.WARNING_MESSAGE);
		}
		catch (AbcConversionException e) {
			JOptionPane.showMessageDialog(ProjectFrame.this, e.getMessage(), "Error exporting ABC",
					JOptionPane.WARNING_MESSAGE);
		}
	}

	private void exportAbc() {
		JFileChooser jfc = new JFileChooser();
		String fileName;
		int dot;
		File origSaveFile = saveFile;

		if (saveFile == null) {
			fileName = this.sequenceInfo.getFile().getName();
			dot = fileName.lastIndexOf('.');
			if (dot > 0)
				fileName = fileName.substring(0, dot);
			fileName = fileName.replace(' ', '_') + ".abc";

			saveFile = new File(Util.getLotroMusicPath(false).getAbsolutePath() + "/" + fileName);
		}
		jfc.setSelectedFile(saveFile);

		int result = jfc.showSaveDialog(this);
		if (result != JFileChooser.APPROVE_OPTION || jfc.getSelectedFile() == null)
			return;

		fileName = jfc.getSelectedFile().getName();
		dot = fileName.lastIndexOf('.');
		if (dot <= 0 || !fileName.substring(dot).equalsIgnoreCase(".abc"))
			fileName += ".abc";

		File saveFileTmp = new File(jfc.getSelectedFile().getParent(), fileName);
		if (!saveFileTmp.equals(origSaveFile) && saveFileTmp.exists()) {
			int res = JOptionPane.showConfirmDialog(this, "File " + fileName + " already exists. Overwrite?",
					"Confirm Overwrite", JOptionPane.YES_NO_OPTION);
			if (res != JOptionPane.YES_OPTION)
				return;
		}
		saveFile = saveFileTmp;

		FileOutputStream out;
		try {
			out = new FileOutputStream(saveFile);
		}
		catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(this, "Failed to create file!\n" + e.getMessage(), "Failed to create file",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (parts.getSize() > 1) {
			PrintStream outWriter = new PrintStream(out);
			outWriter.println("% " + parts.getSize() + " parts");
			outWriter.println();
		}
		try {
			TimingInfo tm = new TimingInfo(getTempo(), getTimeSignature());

			// Remove silent bars before the song starts
			long startMicros = Long.MAX_VALUE;
			long endMicros = Long.MIN_VALUE;
			for (int i = 0; i < parts.size(); i++) {
				AbcPart part = (AbcPart) parts.get(i);

				long firstNoteStart = part.firstNoteStart();
				if (firstNoteStart < startMicros) {
					// Remove integral number of bars
					startMicros = tm.barLength * (firstNoteStart / tm.barLength);
				}
				long lastNoteEnd = part.lastNoteEnd();
				if (lastNoteEnd > endMicros) {
					// Lengthen to an integral number of bars
					endMicros = tm.barLength * ((lastNoteEnd + tm.barLength - 1) / tm.barLength);
				}
			}

			for (int i = 0; i < parts.getSize(); i++) {
				AbcPart part = (AbcPart) parts.get(i);
				part.exportToAbc(tm, getKeySignature(), startMicros, endMicros, 0, out);
			}
		}
		catch (AbcConversionException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		finally {
			try {
				out.close();
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
	}

	/**
	 * Slight modification to JFormattedTextField to select the contents when it
	 * receives focus.
	 */
	private static class MyFormattedTextField extends JFormattedTextField {
		public MyFormattedTextField(Object value, int columns) {
			super(value);
			setColumns(columns);
		}

		@Override
		protected void processFocusEvent(FocusEvent e) {
			super.processFocusEvent(e);
			if (e.getID() == FocusEvent.FOCUS_GAINED)
				selectAll();
		}
	}

	@Override
	public String getComposer() {
		return composerField.getText();
	}

	@Override
	public String getSongTitle() {
		return songTitleField.getText();
	}

	@Override
	public String getTitleTag() {
		return titleTagField.getText();
	}

	@Override
	public String getTranscriber() {
		return transcriberField.getText();
	}
}

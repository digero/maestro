package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import com.digero.common.abc.AbcField;
import com.digero.common.abc.LotroInstrument;
import com.digero.common.abctomidi.AbcToMidi;
import com.digero.common.abctomidi.AbcToMidi.AbcInfo;
import com.digero.common.icons.IconLoader;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.midi.SynthesizerFactory;
import com.digero.common.midi.TimeSignature;
import com.digero.common.midi.VolumeTransceiver;
import com.digero.common.util.FileFilterDropListener;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.ParseException;
import com.digero.common.util.Util;
import com.digero.common.view.AboutDialog;
import com.digero.common.view.NativeVolumeBar;
import com.digero.common.view.PlayControlPanel;
import com.digero.common.view.SongPositionBar;
import com.digero.maestro.MaestroMain;
import com.digero.maestro.abc.AbcConversionException;
import com.digero.maestro.abc.AbcMetadataSource;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.abc.AbcPartProperty;
import com.digero.maestro.abc.PartAutoNumberer;
import com.digero.maestro.abc.TimingInfo;
import com.digero.maestro.midi.NoteFilterSequencerWrapper;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.TrackInfo;
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
	private boolean allowOverwriteSaveFile = false;
	private SequenceInfo sequenceInfo;
	private NoteFilterSequencerWrapper sequencer;
	private VolumeTransceiver volumeTransceiver;
	private SequencerWrapper abcSequencer;
	private VolumeTransceiver abcVolumeTransceiver;
	private ListModelWrapper<AbcPart> parts = new ListModelWrapper<AbcPart>(new DefaultListModel<AbcPart>());
	private PartAutoNumberer partAutoNumberer;
	private boolean usingNativeVolume;

	private JPanel content;
	private JTextField songTitleField;
	private JTextField titleTagField;
	private JTextField composerField;
	private JTextField transcriberField;
	private JSpinner transposeSpinner;
	private JSpinner tempoSpinner;
	private JFormattedTextField keySignatureField;
	private JFormattedTextField timeSignatureField;
	private JCheckBox tripletCheckBox;
	private JButton exportButton;

	private JList<AbcPart> partsList;
	private JButton newPartButton;
	private JButton deletePartButton;

	private PartPanel partPanel;
	private PlayControlPanel playControlPanel;

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

		partAutoNumberer = new PartAutoNumberer(prefs.node("partAutoNumberer"), parts);

		usingNativeVolume = MaestroMain.isNativeVolumeSupported();
		if (usingNativeVolume) {
			volumeTransceiver = null;
			abcVolumeTransceiver = null;
		}
		else {
			volumeTransceiver = new VolumeTransceiver();
			volumeTransceiver.setVolume(prefs.getInt("volumizer", NativeVolumeBar.MAX_VOLUME));

			abcVolumeTransceiver = new VolumeTransceiver();
			abcVolumeTransceiver.setVolume(volumeTransceiver.getVolume());
		}

		try {
			this.sequencer = new NoteFilterSequencerWrapper();
			if (volumeTransceiver != null)
				this.sequencer.addTransceiver(volumeTransceiver);

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
			if (abcVolumeTransceiver == null) {
				transmitter.setReceiver(receiver);
			}
			else {
				transmitter.setReceiver(abcVolumeTransceiver);
				abcVolumeTransceiver.setReceiver(receiver);
			}
			abcSeq.open();
			this.abcSequencer = new SequencerWrapper(abcSeq, transmitter, receiver);
		}
		catch (MidiUnavailableException e) {
			JOptionPane.showMessageDialog(null, "Failed to initialize MIDI sequencer.\nThe program will now exit.",
					"Failed to initialize MIDI sequencer", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}

		try {
			List<Image> icons = new ArrayList<Image>();
			icons.add(ImageIO.read(IconLoader.class.getResourceAsStream("maestro_16.png")));
			icons.add(ImageIO.read(IconLoader.class.getResourceAsStream("maestro_32.png")));
			setIconImages(icons);
		}
		catch (Exception ex) {
			// Ignore
			ex.printStackTrace();
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
		if (SHOW_TITLE_TAG_TEXTBOX) {
			titleTagField = new JTextField(prefs.get("titleTag", ""));
			titleTagField.getDocument().addDocumentListener(new PrefsDocumentListener(prefs, "titleTag"));
		}
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
		transposeSpinner.setToolTipText("<html>Transpose the entire song by semitones.<br>"
				+ "12 semitones = 1 octave</html>");
		transposeSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int transpose = getTranspose();
				for (AbcPart part : parts) {
					part.setBaseTranspose(transpose);
				}
			}
		});

		tempoSpinner = new JSpinner(new SpinnerNumberModel(120 /* value */, 8 /* min */, 960 /* max */, 2 /* step */));

		tripletCheckBox = new JCheckBox("Use triplets/swing rhythm");
		tripletCheckBox.setToolTipText("<html>Tweak the timing to allow for triplets or a swing rhythm.<br><br>"
				+ "This can cause short/fast notes to incorrectly be detected as triplets.<br>"
				+ "Leave it unchecked unless the song has triplets or a swing rhythm.</html>");
		tripletCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (abcSequencer.isRunning())
					refreshPreviewSequence(false);
			}
		});

		exportButton = new JButton("Export ABC");
		exportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportAbc();
			}
		});

		partsList = new JList<AbcPart>(parts.getListModel());
		partsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		partsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				AbcPart abcPart = partsList.getSelectedValue();
				sequencer.getFilter().setAbcPart(abcPart);
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
				parts.add(newPart);
				partAutoNumberer.onPartAdded(newPart);
				Collections.sort(parts, partNumberComparator);
				partsList.clearSelection();
				partsList.setSelectedValue(newPart, true);
				updateAbcButtons();
			}
		});

		deletePartButton = new JButton("Delete");
		deletePartButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int idx = partsList.getSelectedIndex();
				AbcPart oldPart = parts.remove(idx);
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
				PREFERRED, PREFERRED, PREFERRED
		});
		songInfoLayout.setHGap(HGAP);
		songInfoLayout.setVGap(VGAP);
		JPanel songInfoPanel = new JPanel(songInfoLayout);
		{
			int row = 0;
			songInfoPanel.add(new JLabel("T:"), "0, " + row);
			songInfoPanel.add(songTitleField, "1, " + row);
			row++;
			if (SHOW_TITLE_TAG_TEXTBOX) {
				songInfoLayout.insertRow(row, PREFERRED);
				songInfoPanel.add(new JLabel("[]"), "0, " + row);
				songInfoPanel.add(titleTagField, "1, " + row);
				row++;
			}
			songInfoPanel.add(new JLabel("C:"), "0, " + row);
			songInfoPanel.add(composerField, "1, " + row);
			row++;
			songInfoPanel.add(new JLabel("Z:"), "0, " + row);
			songInfoPanel.add(transcriberField, "1, " + row);

			songInfoPanel.setBorder(BorderFactory.createTitledBorder("Song Info"));
		}

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
					if (refreshPreviewSequence(true))
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

		{
			int row = 0;
			settingsPanel.add(new JLabel("Transpose:"), "0, " + row);
			settingsPanel.add(transposeSpinner, "1, " + row);

			row++;
			settingsPanel.add(new JLabel("Tempo:"), "0, " + row);
			settingsPanel.add(tempoSpinner, "1, " + row);

			row++;
			settingsPanel.add(new JLabel("Meter:"), "0, " + row);
			settingsPanel.add(timeSignatureField, "1, " + row + ", 2, " + row + ", L, F");

			if (SHOW_KEY_FIELD) {
				row++;
				settingsLayout.insertRow(row, PREFERRED);
				settingsPanel.add(new JLabel("Key:"), "0, " + row);
				settingsPanel.add(keySignatureField, "1, " + row + ", 2, " + row + ", L, F");
			}

			row++;
			settingsPanel.add(tripletCheckBox, "0, " + row + ", 2, " + row + ", L, C");

			row++;
			settingsPanel.add(exportButton, "0, " + row + ", 2, " + row + ", C, F");

			row++;
			settingsPanel.add(abcPreviewControls, "0, " + row + ", 2, " + row);
		}

		if (!SHOW_TEMPO_SPINNER)
			tempoSpinner.setEnabled(false);
		if (!SHOW_METER_TEXTBOX)
			timeSignatureField.setEnabled(false);
		if (!SHOW_KEY_FIELD)
			keySignatureField.setEnabled(false);

		playControlPanel = new PlayControlPanel(sequencer, new VolumeManager(), "play_blue", "pause_blue", "stop");

		JPanel abcPartsAndSettings = new JPanel(new BorderLayout(HGAP, VGAP));
		abcPartsAndSettings.add(songInfoPanel, BorderLayout.NORTH);
		JPanel partsListAndColorizer = new JPanel(new BorderLayout(HGAP, VGAP));
		partsListAndColorizer.add(partsListPanel, BorderLayout.CENTER);
		if (SHOW_COLORIZER)
			partsListAndColorizer.add(new Colorizer(partPanel), BorderLayout.SOUTH);
		abcPartsAndSettings.add(partsListAndColorizer, BorderLayout.CENTER);
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
				final File file = dropListener.getDroppedFile();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						openSong(file);
					}
				});
			}
		});
		new DropTarget(this, dropListener);

		mainSequencerListener = new MainSequencerListener();
		sequencer.addChangeListener(mainSequencerListener);

		abcSequencerListener = new AbcSequencerListener();
		abcSequencer.addChangeListener(abcSequencerListener);

		parts.getListModel().addListDataListener(new ListDataListener() {
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

	@Override
	public void dispose() {
		for (AbcPart part : parts) {
			part.dispose();
		}
		parts.clear();

		sequencer.dispose();
		abcSequencer.dispose();

		super.dispose();
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

		JMenuItem aboutItem = toolsMenu.add(new JMenuItem("About " + MaestroMain.APP_NAME + "..."));
		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AboutDialog.show(ProjectFrame.this, MaestroMain.APP_NAME, MaestroMain.APP_VERSION, MaestroMain.APP_URL,
						"maestro_64.png");
			}
		});
	}

	public void onVolumeChanged() {
		if (playControlPanel != null)
			playControlPanel.onVolumeChanged();
	}

	private class VolumeManager implements NativeVolumeBar.Callback {
		@Override
		public void setVolume(int volume) {
			if (usingNativeVolume) {
				MaestroMain.setVolume((float) volume / NativeVolumeBar.MAX_VOLUME);
			}
			else {
				if (volumeTransceiver != null)
					volumeTransceiver.setVolume(volume);
				if (abcVolumeTransceiver != null)
					abcVolumeTransceiver.setVolume(volume);
				prefs.putInt("volumizer", volume);
			}
		}

		@Override
		public int getVolume() {
			if (usingNativeVolume) {
				return (int) (MaestroMain.getVolume() * NativeVolumeBar.MAX_VOLUME);
			}
			else {
				if (volumeTransceiver != null)
					return volumeTransceiver.getVolume();
				if (abcVolumeTransceiver != null)
					return abcVolumeTransceiver.getVolume();
				return NativeVolumeBar.MAX_VOLUME;
			}
		}
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

		newPartButton.setEnabled(sequenceInfo != null && parts.size() < 15);
		deletePartButton.setEnabled(partsList.getSelectedIndex() != -1);
		exportButton.setEnabled(sequenceInfo != null && parts.size() > 0);
	}

	private AbcPartListener abcPartListener = new AbcPartListener() {
		public void abcPartChanged(AbcPartEvent e) {
			if (e.getProperty() == AbcPartProperty.PART_NUMBER) {
				int idx;
				AbcPart selected = partsList.getSelectedValue();
				Collections.sort(parts, partNumberComparator);
				if (selected != null) {
					idx = parts.indexOf(selected);
					if (idx >= 0)
						partsList.setSelectedIndex(idx);
				}
			}

			partsList.repaint();

			if (e.isAbcPreviewRelated() && abcSequencer.isRunning()) {
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
		for (AbcPart part : parts) {
			part.dispose();
		}
		parts.clear();

		saveFile = null;
		allowOverwriteSaveFile = false;
		sequencer.clearSequence();
		abcSequencer.clearSequence();
		sequencer.reset(false);
		abcSequencer.reset(false);
		abcPreviewStartMicros = 0;
	}

	public void openSong(File midiFile) {
		midiFile = Util.resolveShortcut(midiFile);

		for (AbcPart part : parts) {
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

			AbcInfo abcInfo = new AbcInfo();

			if (isAbc) {
				AbcToMidi.Params params = new AbcToMidi.Params(midiFile);
				params.abcInfo = abcInfo;
				params.useLotroInstruments = false;
				sequenceInfo = SequenceInfo.fromAbc(params);
				saveFile = midiFile;
				allowOverwriteSaveFile = false;
			}
			else {
				sequenceInfo = SequenceInfo.fromMidi(midiFile);
			}

			sequencer.setSequence(null);
			sequencer.reset(true);
			sequencer.setSequence(sequenceInfo.getSequence());
			sequencer.setTickPosition(sequenceInfo.calcFirstNoteTick());
			songTitleField.setText(sequenceInfo.getTitle());
			composerField.setText(sequenceInfo.getComposer());
			transposeSpinner.setValue(0);
			tempoSpinner.setValue(sequenceInfo.getTempoBPM());
			keySignatureField.setValue(sequenceInfo.getKeySignature());
			timeSignatureField.setValue(sequenceInfo.getTimeSignature());
			tripletCheckBox.setSelected(false);

			if (isAbc) {
				int t = 0;
				for (TrackInfo trackInfo : sequenceInfo.getTrackList()) {
					if (!trackInfo.hasEvents()) {
						t++;
						continue;
					}

					AbcPart newPart = new AbcPart(ProjectFrame.this.sequenceInfo, getTranspose(), ProjectFrame.this);

					newPart.setTitle(abcInfo.getPartName(t));
					newPart.setPartNumber(abcInfo.getPartNumber(t));
					newPart.setTrackEnabled(t, true);

					Set<Integer> midiInstruments = trackInfo.getInstruments();
					for (LotroInstrument lotroInst : LotroInstrument.values()) {
						if (midiInstruments.contains(lotroInst.midiProgramId)) {
							newPart.setInstrument(lotroInst);
							break;
						}
					}

					parts.add(newPart);
					newPart.addAbcListener(abcPartListener);
					t++;
				}

				updateAbcButtons();

				tripletCheckBox.setSelected(abcInfo.hasTriplets());

				if (parts.isEmpty()) {
					newPartButton.doClick();
				}
				else {
					Collections.sort(parts, partNumberComparator);
					partsList.clearSelection();
					partsList.setSelectedValue(parts.get(0), true);
					abcPlayButton.doClick();
				}
			}
			else {
				updateAbcButtons();
				newPartButton.doClick();
				sequencer.start();
			}
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
			if (refreshPreviewPending) {
				if (!refreshPreviewSequence(true))
					abcSequencer.stop();
			}
		}
	}

	private boolean refreshPreviewSequence(boolean immediate) {
		if (!immediate) {
			if (!refreshPreviewPending) {
				refreshPreviewPending = true;
				SwingUtilities.invokeLater(new RefreshPreviewTask());
			}
			return true;
		}

		refreshPreviewPending = false;
		try {
			TimingInfo tm = new TimingInfo(getTempo(), getTimeSignature(), tripletCheckBox.isSelected());

			long startMicros = Long.MAX_VALUE;
			for (AbcPart part : parts) {
				long firstNoteStart = part.firstNoteStart();
				if (firstNoteStart < startMicros)
					startMicros = firstNoteStart;
			}

			SequenceInfo previewSequenceInfo = SequenceInfo.fromAbcParts(parts, this, tm, getKeySignature(),
					startMicros, Long.MAX_VALUE);

			long position = abcSequencer.getPosition() + abcPreviewStartMicros - startMicros;
			abcPreviewStartMicros = startMicros;

			boolean running = abcSequencer.isRunning();
			abcSequencer.reset(false);
			abcSequencer.setSequence(previewSequenceInfo.getSequence());

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
			JOptionPane.showMessageDialog(ProjectFrame.this, e.getMessage(), "Error previewing ABC",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}
		catch (AbcConversionException e) {
			JOptionPane.showMessageDialog(ProjectFrame.this, e.getMessage(), "Error previewing ABC",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}

		return true;
	}

	private void exportAbc() {
		JFileChooser jfc = new JFileChooser();
		String fileName;
		int dot;
		File origSaveFile = saveFile;

		if (saveFile == null) {
			fileName = this.sequenceInfo.getFileName();
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
		if (saveFileTmp.exists() && (!saveFileTmp.equals(origSaveFile) || !allowOverwriteSaveFile)) {
			int res = JOptionPane.showConfirmDialog(this, "File " + fileName + " already exists. Overwrite?",
					"Confirm Overwrite", JOptionPane.YES_NO_OPTION);
			if (res != JOptionPane.YES_OPTION)
				return;
		}
		saveFile = saveFileTmp;
		allowOverwriteSaveFile = true;

		FileOutputStream out;
		try {
			out = new FileOutputStream(saveFile);
		}
		catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(this, "Failed to create file!\n" + e.getMessage(), "Failed to create file",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			TimingInfo tm = new TimingInfo(getTempo(), getTimeSignature(), tripletCheckBox.isSelected());

			// Remove silent bars before the song starts
			long startMicros = Long.MAX_VALUE;
			long endMicros = Long.MIN_VALUE;
			for (AbcPart part : parts) {
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

			if (parts.size() > 0) {
				PrintStream outWriter = new PrintStream(out);
				AbcMetadataSource meta = this;
				outWriter.println(AbcField.SONG_TITLE + meta.getSongTitle());
				outWriter.println(AbcField.SONG_COMPOSER + meta.getComposer());
				outWriter.println(AbcField.SONG_DURATION + Util.formatDuration(endMicros - startMicros));
				outWriter.println(AbcField.SONG_TRANSCRIBER + meta.getTranscriber());
				outWriter.println();
				outWriter.println(AbcField.ABC_CREATOR + MaestroMain.APP_NAME + " v" + MaestroMain.APP_VERSION);
				outWriter.println(AbcField.ABC_VERSION + "2.0");
				outWriter.println();
			}

			for (AbcPart part : parts) {
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
		if (SHOW_TITLE_TAG_TEXTBOX)
			return titleTagField.getText();
		else
			return "";
	}

	@Override
	public String getTranscriber() {
		return transcriberField.getText();
	}
}

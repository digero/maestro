package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.digero.common.abc.TimingInfo;
import com.digero.common.icons.IconLoader;
import com.digero.common.midi.DrumFilterTransceiver;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.midi.SynthesizerFactory;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.FileFilterDropListener;
import com.digero.common.util.ParseException;
import com.digero.common.util.Util;
import com.digero.common.view.PlayControlPanel;
import com.digero.common.view.SongPositionBar;
import com.digero.maestro.MaestroMain;
import com.digero.maestro.abc.AbcConversionException;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.util.ListModelWrapper;

@SuppressWarnings("serial")
public class ProjectFrame extends JFrame implements TableLayoutConstants {
	private static final int HGAP = 4, VGAP = 4;
	private static final double[] LAYOUT_COLS = new double[] {
			250, FILL
	};
	private static final double[] LAYOUT_ROWS = new double[] {
		FILL, //MINIMUM, PREFERRED
	};

	private File saveFile;
	private SequenceInfo sequenceInfo;
	private SequencerWrapper sequencer;
	private SequencerWrapper abcSequencer;
	private DefaultListModel parts = new DefaultListModel();
	private ListModelWrapper<AbcPart> partsWrapper = new ListModelWrapper<AbcPart>(parts);

	private JPanel content;
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

	private Preferences prefs = Preferences.userNodeForPackage(MaestroMain.class);
	private Preferences windowPrefs = prefs.node("window");

	public ProjectFrame() {
		super("LotRO Maestro");
		initializeWindowBounds();

//		this.sequenceInfo = sequenceInfo;
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

//		try {
//			this.sequencer.setSequence(this.sequenceInfo.getSequence());
//		}
//		catch (InvalidMidiDataException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		partPanel = new PartPanel(sequencer);

		TableLayout tableLayout = new TableLayout(LAYOUT_COLS, LAYOUT_ROWS);
		tableLayout.setHGap(HGAP);
		tableLayout.setVGap(VGAP);

		content = new JPanel(tableLayout, false);
		setContentPane(content);

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
				AbcPart newPart = new AbcPart(ProjectFrame.this.sequenceInfo, getTranspose(), calcNextPartNumber());
				newPart.addAbcListener(abcPartListener);
				parts.addElement(newPart);
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
				oldPart.dispose();
				updateAbcButtons();
			}
		});

		JPanel partsButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
		partsButtonPanel.add(newPartButton);
		partsButtonPanel.add(deletePartButton);

		JPanel partsListPanel = new JPanel(new BorderLayout(HGAP, VGAP));
		partsListPanel.setBorder(BorderFactory.createTitledBorder("Song Parts"));
		partsListPanel.add(partsListScrollPane, BorderLayout.CENTER);
		partsListPanel.add(partsButtonPanel, BorderLayout.SOUTH);

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
				abcSequencer.reset();
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
		settingsPanel.add(new JLabel("Key:"), "0, 3");
		settingsPanel.add(transposeSpinner, "1, 0");
		settingsPanel.add(tempoSpinner, "1, 1");
		settingsPanel.add(timeSignatureField, "1, 2, 2, 2, L, F");
		settingsPanel.add(keySignatureField, "1, 3, 2, 3, L, F");
		settingsPanel.add(exportButton, "0, 4, 2, 4, C, F");
		settingsPanel.add(abcPreviewControls, "0, 5, 2, 5");

		PlayControlPanel playControlPanel = new PlayControlPanel(sequencer, "play_blue", "pause_blue", "stop");

		JPanel abcPartsAndSettings = new JPanel(new BorderLayout(HGAP, VGAP));
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
//		newPartButton.doClick();

		sequencer.addChangeListener(new SequencerListener() {
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
		});

		abcSequencer.addChangeListener(new SequencerListener() {
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
		});

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
	}

	private void initializeWindowBounds() {
		setMinimumSize(new Dimension(512, 384));

		Dimension mainScreen = Toolkit.getDefaultToolkit().getScreenSize();

		int width = windowPrefs.getInt("width", 800);
		int height = windowPrefs.getInt("height", 600);
		int x = windowPrefs.getInt("x", (mainScreen.width - width) / 2);
		int y = windowPrefs.getInt("y", (mainScreen.height - height) / 2);

		// Handle the case where the window was last saved on
		// a screen that is no longer connected
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		Rectangle onScreen = null;
		for (int i = 0; i < gs.length; i++) {
			Rectangle monitorBounds = gs[i].getDefaultConfiguration().getBounds();
			if (monitorBounds.intersects(x, y, width, height)) {
				onScreen = monitorBounds;
				break;
			}
		}
		if (onScreen == null) {
			x = (mainScreen.width - width) / 2;
			y = (mainScreen.height - height) / 2;
		}
		else {
			if (x < onScreen.x)
				x = onScreen.x;
			else if (x + width > onScreen.x + onScreen.width)
				x = onScreen.x + onScreen.width - width;

			if (y < onScreen.y)
				y = onScreen.y;
			else if (y + height > onScreen.y + onScreen.height)
				y = onScreen.y + onScreen.height - height;
		}

		setBounds(x, y, width, height);

		int maximized = windowPrefs.getInt("maximized", 0);
		setExtendedState((getExtendedState() & ~JFrame.MAXIMIZED_BOTH) | maximized);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0) {
					windowPrefs.putInt("width", getWidth());
					windowPrefs.putInt("height", getHeight());
				}
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0) {
					windowPrefs.putInt("x", getX());
					windowPrefs.putInt("y", getY());
				}
			}
		});

		addWindowStateListener(new WindowStateListener() {
			@Override
			public void windowStateChanged(WindowEvent e) {
				windowPrefs.putInt("maximized", e.getNewState() & JFrame.MAXIMIZED_BOTH);
			}
		});
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
		return (KeySignature) keySignatureField.getValue();
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
			return p1.getPartNumber() - p2.getPartNumber();
		}
	};

	private void close() {
		for (int i = 0; i < parts.size(); i++) {
			AbcPart part = (AbcPart) parts.get(i);
			part.dispose();
		}
		parts.clear();

		saveFile = null;
		sequencer.clearSequence();
		abcSequencer.clearSequence();
		sequencer.reset();
		abcSequencer.reset();
		abcPreviewStartMicros = 0;
	}

	private void openSong(File midiFile) {
		for (int i = 0; i < parts.size(); i++) {
			AbcPart part = (AbcPart) parts.get(i);
			part.dispose();
		}
		parts.clear();

		saveFile = null;
		sequencer.reset();
		abcSequencer.reset();
		abcPreviewStartMicros = 0;

		try {
			String fileName = midiFile.getName().toLowerCase();
			boolean isAbc = fileName.endsWith(".abc") || fileName.endsWith(".txt");
			sequenceInfo = new SequenceInfo(midiFile, isAbc);

			sequencer.setSequence(sequenceInfo.getSequence());
			transposeSpinner.setValue(0);
			tempoSpinner.setValue(sequenceInfo.getTempoBPM());
			keySignatureField.setValue(sequenceInfo.getKeySignature());
			timeSignatureField.setValue(sequenceInfo.getTimeSignature());

			updateAbcButtons();
			newPartButton.doClick();
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

			for (int i = 0; i < parts.getSize(); i++) {
				AbcPart part = (AbcPart) parts.get(i);
				part.exportToMidi(song, tm, startMicros, Long.MAX_VALUE, 0);
			}

			long position = abcSequencer.getPosition() + abcPreviewStartMicros - startMicros;
			abcPreviewStartMicros = startMicros;

			boolean running = abcSequencer.isRunning();
			abcSequencer.reset();
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

		saveFile = new File(jfc.getSelectedFile().getParent(), fileName);

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
			e.printStackTrace();
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

}

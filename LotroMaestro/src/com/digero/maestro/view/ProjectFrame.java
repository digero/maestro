package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.digero.maestro.abc.AbcConversionException;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.TimingInfo;
import com.digero.maestro.midi.DrumFilterTransceiver;
import com.digero.maestro.midi.KeySignature;
import com.digero.maestro.midi.MidiFactory;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.SequencerEvent;
import com.digero.maestro.midi.SequencerListener;
import com.digero.maestro.midi.SequencerProperty;
import com.digero.maestro.midi.SequencerWrapper;
import com.digero.maestro.midi.TimeSignature;
import com.digero.maestro.midi.synth.SynthesizerFactory;
import com.digero.maestro.util.SaveData;
import com.digero.maestro.util.Util;

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

	public ProjectFrame(SequenceInfo sequenceInfo) {
		this(sequenceInfo, 0, sequenceInfo.getTempoBPM(), sequenceInfo.getTimeSignature(), sequenceInfo
				.getKeySignature());
	}

	public ProjectFrame(SequenceInfo sequenceInfo, int transpose, int tempoBPM, TimeSignature timeSignature,
			KeySignature keySignature) {
		super("LotRO Maestro");
		setBounds(200, 200, 800, 600);

		this.sequenceInfo = sequenceInfo;

		try {
			this.sequencer = new SequencerWrapper(new DrumFilterTransceiver());
		}
		catch (MidiUnavailableException e) {
			JOptionPane.showMessageDialog(null, "Failed to initialize MIDI sequencer.\nThe program will now exit.",
					"Failed to initialize MIDI sequencer.", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}

		try {
			Sequencer seq = MidiSystem.getSequencer(false);
			Synthesizer synth = SynthesizerFactory.getLotroSynthesizer();
			Transmitter transmitter = seq.getTransmitter();
			Receiver receiver = synth.getReceiver();
			transmitter.setReceiver(receiver);
			seq.open();

			this.abcSequencer = new SequencerWrapper(seq, transmitter, receiver);
		}
		catch (MidiUnavailableException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		catch (InvalidMidiDataException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		catch (IOException e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}

		try {
			this.sequencer.setSequence(this.sequenceInfo.getSequence());
		}
		catch (InvalidMidiDataException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		partPanel = new PartPanel(sequencer);

		TableLayout tableLayout = new TableLayout(LAYOUT_COLS, LAYOUT_ROWS);
		tableLayout.setHGap(HGAP);
		tableLayout.setVGap(VGAP);

		content = new JPanel(tableLayout, false);
		setContentPane(content);

		keySignatureField = new MyFormattedTextField(keySignature, 5);
		keySignatureField.setToolTipText("<html>Adjust the key signature of the ABC file. "
				+ "This only affects the display, not the sound of the exported file.<br>"
				+ "Examples: C maj, Eb maj, F# min</html>");

		timeSignatureField = new MyFormattedTextField(timeSignature, 5);
		timeSignatureField.setToolTipText("<html>Adjust the time signature of the ABC file. "
				+ "This only affects the display, not the sound of the exported file.<br>"
				+ "Examples: 4/4, 3/8, 2/2</html>");

		transposeSpinner = new JSpinner(new SpinnerNumberModel(transpose, -48, 48, 1));
		transposeSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int transpose = getTranspose();
				for (int i = 0; i < parts.getSize(); i++) {
					AbcPart part = (AbcPart) parts.getElementAt(i);
					part.setBaseTranspose(transpose);
				}
			}
		});

		tempoSpinner = new JSpinner(new SpinnerNumberModel(tempoBPM, 8, 960, 2));

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
				int idx = partsList.getSelectedIndex();
				if (idx != -1) {
					AbcPart abcPart = (AbcPart) parts.getElementAt(idx);
					sequencer.getDrumFilter().setAbcPart(abcPart);
					partPanel.setAbcPart(abcPart);
				}
				else {
					sequencer.getDrumFilter().setAbcPart(null);
					partPanel.setAbcPart(null);
				}
			}
		});

		JScrollPane partsListScrollPane = new JScrollPane(partsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		newPartButton = new JButton("New Part");
		newPartButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbcPart newPart = new AbcPart(ProjectFrame.this.sequenceInfo, getTranspose(), calcNextPartNumber());
				newPart.addChangeListener(partChangeListener);
				parts.addElement(newPart);
				partsList.setSelectedIndex(parts.indexOf(newPart));
				newPartButton.setEnabled(parts.getSize() < 15);
			}
		});

		deletePartButton = new JButton("Delete");
		deletePartButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String title = "Delete Part";
				String msg = "Are you sure you want to delete the selected part?\nThis cannot be undone.";
				int r = JOptionPane.showConfirmDialog(ProjectFrame.this, msg, title, JOptionPane.YES_NO_OPTION);
				if (r == JOptionPane.YES_OPTION) {
					AbcPart oldPart = (AbcPart) parts.remove(partsList.getSelectedIndex());
					oldPart.removeChangeListener(partChangeListener);
				}
				newPartButton.setEnabled(parts.getSize() < 15);
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
		abcPlayIcon = new ImageIcon(ProjectFrame.class.getResource("icons/play_yellow.png"));
		abcPauseIcon = new ImageIcon(ProjectFrame.class.getResource("icons/pause.png"));
		Icon abcStopIcon = new ImageIcon(ProjectFrame.class.getResource("icons/stop.png"));
		abcPlayButton = new JButton(abcPlayIcon);
		abcPlayButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (abcSequencer.isRunning()) {
					abcSequencer.stop();
				}
				else {
					refreshPreviewSequence();
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

		final FileFilterDropListener dropListener = new FileFilterDropListener(false, "mid", "midi");
		dropListener.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openSong(dropListener.getDroppedFile());
			}
		});
		new DropTarget(this, dropListener);
		newPartButton.doClick();

		sequencer.addChangeListener(new SequencerListener() {
			public void propertyChanged(SequencerEvent evt) {
				if (evt.getProperty() == SequencerProperty.IS_RUNNING) {
					if (sequencer.isRunning())
						abcSequencer.stop();
				}
			}
		});

		abcSequencer.addChangeListener(new SequencerListener() {
			public void propertyChanged(SequencerEvent evt) {
				if (evt.getProperty() == SequencerProperty.IS_RUNNING) {
					updateAbcPlayButtons();
					if (abcSequencer.isRunning())
						sequencer.stop();
				}
				else if (evt.getProperty() == SequencerProperty.POSITION) {
					updateAbcPlayButtons();
				}
			}
		});
	}

	private void updateAbcPlayButtons() {
		abcPlayButton.setEnabled(abcSequencer.isLoaded() || parts.size() > 0);
		abcPlayButton.setIcon(abcSequencer.isRunning() ? abcPauseIcon : abcPlayIcon);
		abcStopButton.setEnabled(abcSequencer.isLoaded()
				&& (abcSequencer.isRunning() || abcSequencer.getPosition() != 0));
	}

	private ChangeListener partChangeListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
			int idx = parts.indexOf(e.getSource());
			if (idx >= 0) {
				// Make the list model fire the contentsChanged event, 
				// which will cause a repaint
				parts.set(idx, e.getSource());
			}

			if (abcSequencer.isRunning()) {
				refreshPreviewSequence();
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

	private void close() {
		for (int i = 0; i < parts.size(); i++) {
			AbcPart part = (AbcPart) parts.get(i);
			part.removeChangeListener(partChangeListener);
		}
		parts.clear();

		try {
			sequencer.setSequence(null);
		}
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	private void openSong(File midiFile) {
		saveFile = null;
		sequencer.reset();
		abcSequencer.reset();
		abcPreviewStartMicros = 0;

		try {
			for (int i = 0; i < parts.size(); i++) {
				AbcPart part = (AbcPart) parts.get(i);
				part.dispose();
			}
			parts.clear();

			sequenceInfo = new SequenceInfo(midiFile);

			sequencer.setSequence(sequenceInfo.getSequence());
			transposeSpinner.setValue(0);
			tempoSpinner.setValue(sequenceInfo.getTempoBPM());
			keySignatureField.setValue(sequenceInfo.getKeySignature());
			timeSignatureField.setValue(sequenceInfo.getTimeSignature());

			newPartButton.doClick();
		}
		catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void save() {
		SaveData data = new SaveData();
		data.setString("project.midiFile", sequenceInfo.getMidiFile().getPath());
		data.setInt("project.transpose", getTranspose());
		data.setInt("project.tempo", getTempo());
		data.setKeySignature("project.keySignature", getKeySignature());
		data.setTimeSignature("project.timeSignature", getTimeSignature());
		data.setInt("project.partCount", parts.getSize());

		for (int i = 0; i < parts.getSize(); i++) {
			AbcPart part = (AbcPart) parts.getElementAt(i);

		}
	}

	private void refreshPreviewSequence() {
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
				part.exportToMidi(song, tm, startMicros, Long.MAX_VALUE);
			}

			long position = abcSequencer.getPosition() + abcPreviewStartMicros - startMicros;
			abcPreviewStartMicros = startMicros;

			abcSequencer.setSequence(song);

			if (position < 0 || position > abcSequencer.getLength())
				position = 0;

			abcSequencer.setPosition(position);
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error exporting ABC", JOptionPane.WARNING_MESSAGE);
		}
		catch (AbcConversionException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error exporting ABC", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void exportAbc() {
		JFileChooser jfc = new JFileChooser();
		String fileName;
		int dot;
		if (saveFile == null) {
			fileName = this.sequenceInfo.getMidiFile().getName();
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
			for (int i = 0; i < parts.size(); i++) {
				AbcPart part = (AbcPart) parts.get(i);

				long firstNoteStart = part.firstNoteStart();
				if (firstNoteStart < startMicros) {
					// Remove integral number of bars
					startMicros = tm.barLength * (firstNoteStart / tm.barLength);
				}
			}

			for (int i = 0; i < parts.getSize(); i++) {
				AbcPart part = (AbcPart) parts.get(i);
				part.exportToAbc(tm, getKeySignature(), startMicros, Long.MAX_VALUE, out);
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

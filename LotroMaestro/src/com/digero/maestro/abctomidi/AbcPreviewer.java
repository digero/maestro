package com.digero.maestro.abctomidi;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import com.digero.maestro.MaestroMain;
import com.digero.maestro.abc.LotroInstrument;
import com.digero.maestro.midi.IMidiConstants;
import com.digero.maestro.midi.SequencerEvent;
import com.digero.maestro.midi.SequencerListener;
import com.digero.maestro.midi.SequencerProperty;
import com.digero.maestro.midi.SequencerWrapper;
import com.digero.maestro.util.ParseException;
import com.digero.maestro.view.FileTypeDropListener;
import com.digero.maestro.view.SongPositionBar;
import com.digero.maestro.view.SongPositionLabel;

public class AbcPreviewer extends JFrame implements TableLayoutConstants, IMidiConstants {
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {}

		AbcPreviewer frame = new AbcPreviewer();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setBounds(200, 200, 400, 250);
		frame.setVisible(true);
	}

	private SequencerWrapper sequencer;
	private Synthesizer synth;
	private Transmitter transmitter;
	private Receiver receiver;
	private boolean useLotroInstruments = true;

	private JPanel content;
	private TrackListPanel trackListPanel;

	private SongPositionBar songPositionBar;
	private SongPositionLabel songPositionLabel;

	private ImageIcon playIcon, pauseIcon, stopIcon;
	private JButton playButton, stopButton;

	private Map<Integer, LotroInstrument> instrumentOverrideMap = new HashMap<Integer, LotroInstrument>();

	private List<String> abcLines;

	public AbcPreviewer() {
		super("LotRO ABC Previewer");

		final FileTypeDropListener dropListener = new FileTypeDropListener("abc", "txt");
		dropListener.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openSong(dropListener.getDroppedFile());
			}
		});
		new DropTarget(this, dropListener);

		try {
			sequencer = new SequencerWrapper(MidiSystem.getSequencer(false));
			sequencer.open();
			transmitter = sequencer.getTransmitter();
			synth = null;

			if (useLotroInstruments) {
				try {
					Soundbank lotroSoundbank = MidiSystem.getSoundbank(MaestroMain.class
							.getResourceAsStream("midi/synth/LotroInstruments.sf2"));
					Soundbank lotroDrumbank = MidiSystem.getSoundbank(MaestroMain.class
							.getResourceAsStream("midi/synth/LotroDrums.sf2"));
					synth = MidiSystem.getSynthesizer();
					synth.open();
					synth.unloadAllInstruments(lotroSoundbank);
					synth.loadAllInstruments(lotroSoundbank);
					synth.unloadAllInstruments(lotroDrumbank);
					synth.loadAllInstruments(lotroDrumbank);
					receiver = synth.getReceiver();
				}
				catch (IOException e) {
					if (synth != null)
						synth.close();
					if (receiver != null)
						receiver.close();
					synth = null;
					useLotroInstruments = false;
				}
			}

			if (!useLotroInstruments) {
				receiver = MidiSystem.getReceiver();
			}
			transmitter.setReceiver(receiver);
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		catch (MidiUnavailableException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		content = new JPanel(new TableLayout(new double[] { // Columns
						4, FILL, 4
				}, new double[] { // Rows
						FILL, 8, PREFERRED, PREFERRED
				}));
		setContentPane(content);

		trackListPanel = new TrackListPanel();
		JScrollPane trackListScroller = new JScrollPane(trackListPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		trackListScroller.getVerticalScrollBar().setUnitIncrement(TRACKLIST_ROWHEIGHT);

		songPositionBar = new SongPositionBar(sequencer);
		songPositionLabel = new SongPositionLabel(sequencer);
		JPanel songPositionPanel = new JPanel(new BorderLayout(4, 4));
		songPositionPanel.add(songPositionBar, BorderLayout.CENTER);
		songPositionPanel.add(songPositionLabel, BorderLayout.EAST);

		playIcon = new ImageIcon(MaestroMain.class.getResource("view/icons/play.png"));
		pauseIcon = new ImageIcon(MaestroMain.class.getResource("view/icons/pause.png"));
		stopIcon = new ImageIcon(MaestroMain.class.getResource("view/icons/stop.png"));

		playButton = new JButton(playIcon);
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (sequencer.isRunning())
					sequencer.stop(AbcPreviewer.this);
				else
					sequencer.start(AbcPreviewer.this);
			}
		});

		stopButton = new JButton(stopIcon);
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});

		sequencer.addChangeListener(new SequencerListener() {
			public void propertyChanged(SequencerEvent evt) {
				if (evt.getProperty() == SequencerProperty.IS_RUNNING) {
					playButton.setIcon(sequencer.isRunning() ? pauseIcon : playIcon);
				}
			}
		});

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
		buttonPanel.add(playButton);
		buttonPanel.add(stopButton);

		add(trackListScroller, "0, 0, 2, 0");
		add(songPositionPanel, "1, 2");
		add(buttonPanel, "1, 3");
	}

	private void closeMidi() {
		if (sequencer != null)
			sequencer.close();
		if (synth != null)
			synth.close();
		if (transmitter != null)
			transmitter.close();
		if (receiver != null)
			receiver.close();
	}

	private void openSong(File abcFile) {
		stop();

		for (int i = 0; i < 16; i++) {
			sequencer.setTrackMute(i, false, this);
			sequencer.setTrackSolo(i, false, this);
		}
		instrumentOverrideMap.clear();

		FileInputStream in = null;
		try {
			abcLines = AbcToMidi.readLines(new FileInputStream(abcFile));
			sequencer.setSequence(new AbcToMidi().convert(abcLines, useLotroInstruments, instrumentOverrideMap), this);
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Failed to open file", JOptionPane.ERROR_MESSAGE);
		}
		catch (ParseException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error reading ABC file", JOptionPane.ERROR_MESSAGE);
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
		}
		finally {
			try {
				if (in != null)
					in.close();
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Failed to close file", JOptionPane.ERROR_MESSAGE);
			}
		}

		trackListPanel.songChanged();
	}

	private void stop() {
		sequencer.stop(AbcPreviewer.this);
		sequencer.setPosition(0, AbcPreviewer.this);
	}

	//
	// Track list
	//
	private static final int TRACKLIST_ROWHEIGHT = 18;

	private class TrackListPanel extends JPanel implements ActionListener {
		private TableLayout layout;

		private Object trackIndexKey = new Object();

		public TrackListPanel() {
			super(new TableLayout(new double[] {
					0, FILL, PREFERRED, 0
			}, new double[] {
					0, 0
			}));

			layout = (TableLayout) getLayout();
			layout.setVGap(4);
			layout.setHGap(4);

			setBackground(Color.WHITE);
		}

		public void clear() {
			for (Component c : getComponents()) {
				if (c instanceof JCheckBox) {
					((JCheckBox) c).removeActionListener(this);
				}
			}
			removeAll();
			for (int i = layout.getNumRow() - 2; i >= 1; i--) {
				layout.deleteRow(i);
			}
			invalidate();
			repaint();
		}

		public void songChanged() {
			clear();
			if (sequencer.getSequence() == null)
				return;

			Track[] tracks = sequencer.getSequence().getTracks();
			for (int i = 0; i < tracks.length; i++) {
				Track track = tracks[i];

				// Only show tracks with at least one note
				boolean hasNotes = false;
				String title = null;
				LotroInstrument instrument = LotroInstrument.LUTE;
				for (int j = 0; j < track.size(); j++) {
					MidiEvent evt = track.get(j);
					if (evt.getMessage() instanceof ShortMessage) {
						ShortMessage m = (ShortMessage) evt.getMessage();
						if (m.getCommand() == ShortMessage.NOTE_ON) {
							hasNotes = true;
						}
						else if (m.getCommand() == ShortMessage.PROGRAM_CHANGE) {
							for (LotroInstrument inst : LotroInstrument.values()) {
								if (m.getData1() == inst.midiProgramId) {
									instrument = inst;
									break;
								}
							}
						}
					}
					else if (evt.getMessage() instanceof MetaMessage) {
						MetaMessage meta = (MetaMessage) evt.getMessage();
						if (meta.getType() == META_TRACK_NAME) {
							try {
								title = new String(meta.getData(), "US-ASCII");
							}
							catch (UnsupportedEncodingException e) {
								// ASCII is supported...
							}
						}
					}
				}

				if (hasNotes) {
					if (title == null)
						title = "Track " + (i + 1);

					JCheckBox checkBox = new JCheckBox(title);
					checkBox.putClientProperty(trackIndexKey, i);
					checkBox.setBackground(getBackground());
					checkBox.setSelected(!sequencer.getTrackMute(i));
					checkBox.addActionListener(this);

					JComboBox comboBox = new JComboBox(LotroInstrument.values());
					comboBox.putClientProperty(trackIndexKey, i);
					comboBox.setBackground(getBackground());
					comboBox.setSelectedItem(instrument);
					comboBox.addActionListener(this);

					int r = layout.getNumRow() - 1;
					layout.insertRow(r, TRACKLIST_ROWHEIGHT);
					add(checkBox, "1, " + r);
					add(comboBox, "2, " + r);
				}
			}

			validate();
			repaint();
		}

		private void changeInstrument(Track track, int patch) {
			long position = sequencer.getPosition();

//			int channel = -1;
//			for (int j = 0; j < track.size(); j++) {
//				MidiEvent evt = track.get(j);
//				if (evt.getMessage() instanceof ShortMessage) {
//					ShortMessage m = (ShortMessage) evt.getMessage();
//					if (m.getCommand() == ShortMessage.PROGRAM_CHANGE) {
//						channel = m.getChannel();
//						track.remove(evt);
//					}
//					else if (m.getCommand() == ShortMessage.NOTE_ON) {
//						channel = m.getChannel();
//					}
//				}
//			}
//
//			if (channel != -1) {
//				MidiEvent evt = MidiFactory.createProgramChangeEvent(patch, channel, 0);
//				track.add(evt);
//				receiver.send(evt.getMessage(), 0);
//				ShortMessage noteOff = new ShortMessage();
//				try {
//					for (int i = 0; i < 128; i++) {
//						noteOff.setMessage(ShortMessage.NOTE_OFF, channel, i, 127);
//						receiver.send(noteOff, 0);
//					}
//				}
//				catch (InvalidMidiDataException e) {
//					e.printStackTrace();
//				}
//			}
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() instanceof JCheckBox) {
				JCheckBox checkBox = (JCheckBox) evt.getSource();
				int i = (Integer) checkBox.getClientProperty(trackIndexKey);
				sequencer.setTrackMute(i, !checkBox.isSelected(), AbcPreviewer.this);
			}
			else if (evt.getSource() instanceof JComboBox) {
				JComboBox comboBox = (JComboBox) evt.getSource();
				int i = (Integer) comboBox.getClientProperty(trackIndexKey);

				try {
					long positon = sequencer.getPosition();
					instrumentOverrideMap.put(i, (LotroInstrument) comboBox.getSelectedItem());
					Sequence song = new AbcToMidi().convert(abcLines, useLotroInstruments, instrumentOverrideMap);
					sequencer.setSequence(song, AbcPreviewer.this);
					sequencer.setPosition(positon, AbcPreviewer.this);
				}
				catch (InvalidMidiDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}

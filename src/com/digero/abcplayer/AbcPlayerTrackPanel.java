package com.digero.abcplayer;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.Note;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.view.ColorTable;
import com.digero.common.view.NoteGraph;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.IDisposable;

public class AbcPlayerTrackPanel extends JPanel implements IDisposable, TableLayoutConstants {
	// +--------------+-----+-----+-----------------------------------------------+
	// | Track Name               |                                               |
	// +--------------+-----+-----+            Note graph                         +
	// | [Instrument] | [M] | [S] |                                               |
	// +--------------+-----+-----+-----------------------------------------------+

	public static final int PREFERRED_HEIGHT = 48;
	private static final int H_GAP = 4;
	private static final int V_GAP = 4;
	private static final int MUTE_BUTTON_WIDTH = 24;
	private static final int COMBO_BOX_WIDTH = 80;

	private static final double[] LAYOUT_COLS = new double[] {
			0, COMBO_BOX_WIDTH, MUTE_BUTTON_WIDTH, MUTE_BUTTON_WIDTH, FILL
	};
	private static final double[] LAYOUT_ROWS = new double[] {
			0, FILL, PREFERRED, 0
	};

	private SequencerWrapper sequencer;
	private TrackInfo track;
	private LotroInstrument instrument;

	private JToggleButton muteButton;
	private JButton soloButton;
	private JLabel titleLabel;
	private JComboBox comboBox;
	private AbcNoteGraph noteGraph;

	private SequencerListener sequencerListener;

	private boolean isMouseDownSolo = false;

	private List<ActionListener> listeners = new ArrayList<ActionListener>();

	public AbcPlayerTrackPanel(SequencerWrapper sequencer, TrackInfo trackInfo) {
		super(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));
		TableLayout layout = (TableLayout) getLayout();
		layout.setHGap(H_GAP);
		layout.setVGap(V_GAP);
		
		setPreferredSize(new Dimension(200, PREFERRED_HEIGHT));

		this.sequencer = sequencer;
		this.track = trackInfo;

		muteButton = new JToggleButton("M");
		muteButton.setToolTipText("Mute this part");
		muteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbcPlayerTrackPanel.this.sequencer.setTrackMute(track.getTrackNumber(), muteButton.isSelected());
			}
		});
		muteButton.setMargin(new Insets(0, 0, 0, 0));
		muteButton.setOpaque(false);
		muteButton.setFont(muteButton.getFont().deriveFont(10));

		soloButton = new JButton("S");
		soloButton.setToolTipText("Play only this part (solo)");
		soloButton.getModel().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				AbcPlayerTrackPanel.this.sequencer.setTrackSolo(track.getTrackNumber(), isSolo());
			}
		});
		soloButton.setMargin(new Insets(0, 0, 0, 0));
		soloButton.setOpaque(false);
		soloButton.setFont(soloButton.getFont().deriveFont(10));

		titleLabel = new JLabel(trackInfo.getTrackNumber() + ". " + trackInfo.getName());
		titleLabel.setOpaque(false);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setForeground(ColorTable.PANEL_TEXT_DISABLED.get());
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

		instrument = LotroInstrument.LUTE;
		outerLoop: for (int instId : track.getInstruments()) {
			for (LotroInstrument inst : LotroInstrument.values()) {
				if (instId == inst.midiProgramId) {
					instrument = inst;
					break outerLoop;
				}
			}
		}

		comboBox = new JComboBox(LotroInstrument.values());
		comboBox.setMaximumRowCount(12);
		comboBox.setSelectedItem(instrument);
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				instrument = (LotroInstrument) comboBox.getSelectedItem();
				ActionEvent e2 = new ActionEvent(AbcPlayerTrackPanel.this, e.getID(), e.getActionCommand());
				for (ActionListener al : listeners) {
					al.actionPerformed(e2);
				}
			}
		});
		comboBox.setOpaque(false);

		noteGraph = new AbcNoteGraph(sequencer, trackInfo);
		noteGraph.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					isMouseDownSolo = true;
					AbcPlayerTrackPanel.this.sequencer.setTrackSolo(track.getTrackNumber(), isSolo());
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					isMouseDownSolo = false;
					AbcPlayerTrackPanel.this.sequencer.setTrackSolo(track.getTrackNumber(), isSolo());
				}
			}
		});

		sequencer.addChangeListener(sequencerListener = new SequencerListener() {
			public void propertyChanged(SequencerEvent evt) {
				if (evt.getProperty() == SequencerProperty.TRACK_ACTIVE)
					updateColors();
			}
		});

		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorTable.PANEL_BORDER.get()));

		// Add title label first so it's on top of the note graph
		add(titleLabel, "1, 1, 4, 1");
		add(comboBox, "1, 2, L, F");
		add(muteButton, "2, 2");
		add(soloButton, "3, 2");
		add(noteGraph, "4, 0, 4, 3, F, F");

		updateColors();
	}

	@Override
	public void dispose() {
		sequencer.removeChangeListener(sequencerListener);
	}

	public void addInstrumentChangeListener(ActionListener al) {
		listeners.add(al);
	}

	public void removeInstrumentChangeListener(ActionListener al) {
		listeners.remove(al);
	}

	public LotroInstrument getInstrument() {
		return instrument;
	}

	public TrackInfo getTrackInfo() {
		return track;
	}

	public void setTrackInfo(TrackInfo trackInfo) {
		this.track = trackInfo;
		noteGraph.setTrackInfo(trackInfo);
		titleLabel.setText(trackInfo.getTrackNumber() + ". " + trackInfo.getName());
	}

	private void updateColors() {
		if (sequencer.isTrackActive(track.getTrackNumber())) {
			noteGraph.setNoteColor(ColorTable.NOTE_ABC_ENABLED);
			noteGraph.setBadNoteColor(ColorTable.NOTE_BAD_ENABLED);
			setBackground(ColorTable.GRAPH_BACKGROUND_ENABLED.get());
		}
		else {
			noteGraph.setNoteColor(ColorTable.NOTE_ABC_OFF);
			noteGraph.setBadNoteColor(ColorTable.NOTE_BAD_OFF);
			setBackground(ColorTable.GRAPH_BACKGROUND_OFF.get());
		}
	}

	private boolean isSolo() {
		return isMouseDownSolo || soloButton.getModel().isPressed();
	}

	private class AbcNoteGraph extends NoteGraph {
		public AbcNoteGraph(SequencerWrapper sequencer, TrackInfo trackInfo) {
			super(sequencer, trackInfo, Note.MIN_PLAYABLE.id - 2, Note.MAX_PLAYABLE.id + 2);
		}

		@Override
		protected int transposeNote(int noteId) {
			if (instrument == LotroInstrument.COWBELL || instrument == LotroInstrument.MOOR_COWBELL)
				return (Note.MIN_PLAYABLE.id + Note.MAX_PLAYABLE.id) / 2;
			
			return noteId - 12 * instrument.octaveDelta;
		}

		@Override
		protected boolean isNotePlayable(int noteId) {
			if (instrument == LotroInstrument.COWBELL || instrument == LotroInstrument.MOOR_COWBELL)
				return true;

			return noteId >= instrument.lowestPlayable.id && noteId <= instrument.highestPlayable.id;
		}
	}
}

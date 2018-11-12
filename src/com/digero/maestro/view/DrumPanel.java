package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.MidiDrum;
import com.digero.common.midi.Note;
import com.digero.common.midi.NoteFilterSequencerWrapper;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerEvent.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Listener;
import com.digero.common.util.Util;
import com.digero.common.view.ColorTable;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.LotroDrumInfo;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.TrackInfo;

@SuppressWarnings("serial")
public class DrumPanel extends JPanel implements IDiscardable, TableLayoutConstants, ICompileConstants
{
	//     0            1              2               3
	//   +---+--------------------+----------+--------------------+
	//   |   | TRACK NAME         | Drum     |  +--------------+  |
	// 0 |   |                    | +------+ |  | (note graph) |  |
	//   |   | Instrument(s)      | +-----v+ |  +--------------+  |
	//   +---+--------------------+----------+--------------------+
	private static final int GUTTER_WIDTH = TrackPanel.GUTTER_WIDTH;
	private static final int COMBO_WIDTH = 122;
	private static final int TITLE_WIDTH = TrackPanel.TITLE_WIDTH + TrackPanel.CONTROL_WIDTH - COMBO_WIDTH;
	private static final double[] LAYOUT_COLS = new double[] { GUTTER_WIDTH, TITLE_WIDTH, COMBO_WIDTH, FILL };
	private static final double[] LAYOUT_ROWS = new double[] { PREFERRED };

	private TrackInfo trackInfo;
	private NoteFilterSequencerWrapper seq;
	private SequencerWrapper abcSequencer;
	private AbcPart abcPart;
	private int drumId;
	private boolean isAbcPreviewMode;

	private JPanel gutter;
	private JCheckBox checkBox;
	private JComboBox<LotroDrumInfo> drumComboBox;
	private DrumNoteGraph noteGraph;
	private TrackVolumeBar trackVolumeBar;
	private ActionListener trackVolumeBarListener;

	public DrumPanel(TrackInfo info, NoteFilterSequencerWrapper sequencer, AbcPart part, int drumNoteId,
			SequencerWrapper abcSequencer_, TrackVolumeBar trackVolumeBar_)
	{
		super(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));

		this.trackInfo = info;
		this.seq = sequencer;
		this.abcSequencer = abcSequencer_;
		this.abcPart = part;
		this.drumId = drumNoteId;
		this.trackVolumeBar = trackVolumeBar_;

		TableLayout tableLayout = (TableLayout) getLayout();
		tableLayout.setHGap(TrackPanel.HGAP);

		gutter = new JPanel((LayoutManager) null);
		gutter.setOpaque(false);

		checkBox = new JCheckBox();
		checkBox.setSelected(abcPart.isDrumEnabled(trackInfo.getTrackNumber(), drumId));
		checkBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				abcPart.setDrumEnabled(trackInfo.getTrackNumber(), drumId, checkBox.isSelected());
			}
		});

		checkBox.setOpaque(false);

		String title = trackInfo.getTrackNumber() + ". " + trackInfo.getName();
		String instr;
		if (info.isDrumTrack())
			instr = MidiDrum.fromId(drumId).name;
		else
		{
			instr = Note.fromId(drumNoteId).abc;
			checkBox.setFont(checkBox.getFont().deriveFont(Font.BOLD));
		}

		checkBox.setToolTipText("<html><b>" + title + "</b><br>" + instr + "</html>");

		instr = Util.ellipsis(instr, TITLE_WIDTH, checkBox.getFont());
		checkBox.setText(instr);

		drumComboBox = new JComboBox<LotroDrumInfo>(LotroDrumInfo.ALL_DRUMS.toArray(new LotroDrumInfo[0]));
		drumComboBox.setSelectedItem(getSelectedDrum());
		drumComboBox.setMaximumRowCount(20);
		drumComboBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				LotroDrumInfo selected = (LotroDrumInfo) drumComboBox.getSelectedItem();
				abcPart.getDrumMap(trackInfo.getTrackNumber()).set(drumId, selected.note.id);
			}
		});

		seq.addChangeListener(sequencerListener);
		if (abcSequencer != null)
			abcSequencer.addChangeListener(sequencerListener);
		abcPart.addAbcListener(abcPartListener);

		noteGraph = new DrumNoteGraph(seq, trackInfo);
		noteGraph.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ColorTable.OCTAVE_LINE.get()));
		noteGraph.addMouseListener(new MouseAdapter()
		{
			private int soloAbcTrack = -1;
			private int soloAbcDrumId = -1;
			private int soloTrack = -1;
			private int soloDrumId = -1;

			@Override public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON3)
				{
					int trackNumber = trackInfo.getTrackNumber();
					if (isAbcPreviewMode() && abcSequencer instanceof NoteFilterSequencerWrapper)
					{
						if (abcPart.isTrackEnabled(trackNumber))
						{
							soloAbcTrack = abcPart.getPreviewSequenceTrackNumber();
							Note soloDrumNote = abcPart.mapNote(trackNumber, drumId);
							soloAbcDrumId = (soloDrumNote == null) ? -1 : soloDrumNote.id;
						}

						if (soloAbcTrack >= 0 && soloAbcDrumId >= 0)
						{
							((NoteFilterSequencerWrapper) abcSequencer).setNoteSolo(soloAbcTrack, soloAbcDrumId, true);
						}
					}
					else
					{
						soloTrack = trackNumber;
						soloDrumId = drumId;
						seq.setNoteSolo(trackNumber, drumId, true);
					}
				}
			}

			@Override public void mouseReleased(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON3)
				{
					if (soloAbcTrack >= 0 && soloAbcDrumId >= 0 && abcSequencer instanceof NoteFilterSequencerWrapper)
					{
						((NoteFilterSequencerWrapper) abcSequencer).setNoteSolo(soloAbcTrack, soloAbcDrumId, false);
					}
					soloAbcTrack = -1;
					soloAbcDrumId = -1;

					if (soloTrack >= 0 && soloDrumId >= 0)
					{
						seq.setNoteSolo(soloTrack, soloDrumId, false);
					}
					soloTrack = -1;
					soloDrumId = -1;
				}
			}
		});

		if (trackVolumeBar != null)
		{
			trackVolumeBar.addActionListener(trackVolumeBarListener = new ActionListener()
			{
				@Override public void actionPerformed(ActionEvent e)
				{
					updateState();
				}
			});
		}

		addPropertyChangeListener("enabled", new PropertyChangeListener()
		{
			@Override public void propertyChange(PropertyChangeEvent evt)
			{
				updateState();
			}
		});

		add(gutter, "0, 0");
		add(checkBox, "1, 0");
		add(drumComboBox, "2, 0, f, c");
		add(noteGraph, "3, 0");

		updateState();
	}

	@Override public void discard()
	{
		noteGraph.discard();
		abcPart.removeAbcListener(abcPartListener);
		seq.removeChangeListener(sequencerListener);
		if (abcSequencer != null)
			abcSequencer.removeChangeListener(sequencerListener);
		if (trackVolumeBar != null)
			trackVolumeBar.removeActionListener(trackVolumeBarListener);
	}

	private Listener<AbcPartEvent> abcPartListener = new Listener<AbcPartEvent>()
	{
		@Override public void onEvent(AbcPartEvent e)
		{
			if (e.isNoteGraphRelated())
			{
				checkBox.setEnabled(abcPart.isTrackEnabled(trackInfo.getTrackNumber()));
				checkBox.setSelected(abcPart.isDrumEnabled(trackInfo.getTrackNumber(), drumId));
				drumComboBox.setSelectedItem(getSelectedDrum());
				updateState();
			}
		}
	};

	private Listener<SequencerEvent> sequencerListener = new Listener<SequencerEvent>()
	{
		@Override public void onEvent(SequencerEvent evt)
		{
			if (evt.getProperty() == SequencerProperty.TRACK_ACTIVE)
				updateState();
		}
	};

	private void updateState()
	{
		boolean abcPreviewMode = isAbcPreviewMode();
		int trackNumber = trackInfo.getTrackNumber();
		boolean trackEnabled = abcPart.isTrackEnabled(trackNumber);
		boolean noteEnabled = abcPart.isDrumEnabled(trackNumber, drumId);
		boolean noteEnabledOtherPart = false;

		boolean noteActive;
		if (abcPreviewMode)
		{
			noteActive = false;
		}
		else
		{
			noteActive = seq.isTrackActive(trackNumber) && seq.isNoteActive(drumId);
		}

		boolean isDraggingVolumeBar = (trackVolumeBar != null) && trackVolumeBar.isDragging();
		noteGraph.setShowingNoteVelocity(isDraggingVolumeBar);

		if (isDraggingVolumeBar)
			noteGraph.setDeltaVolume(trackVolumeBar.getDeltaVolume());
		else
			noteGraph.setDeltaVolume(abcPart.getTrackVolumeAdjust(trackInfo.getTrackNumber()));

		for (AbcPart part : abcPart.getAbcSong().getParts())
		{
			if (part.isTrackEnabled(trackNumber))
			{
				if (part != this.abcPart && part.isDrumEnabled(trackNumber, drumId))
					noteEnabledOtherPart = true;

				if (abcPreviewMode)
				{
					Note drumNote = part.mapNote(trackNumber, drumId);
					if (drumNote != null && abcSequencer.isTrackActive(part.getPreviewSequenceTrackNumber())
							&& abcSequencer.isNoteActive(drumNote.id))
					{
						noteActive = true;
					}
				}
			}
		}

		gutter.setOpaque(noteEnabled || noteEnabledOtherPart);
		if (noteEnabled)
			gutter.setBackground(ColorTable.PANEL_HIGHLIGHT.get());
		else if (noteEnabledOtherPart)
			gutter.setBackground(ColorTable.PANEL_HIGHLIGHT_OTHER_PART.get());

		noteGraph.setShowingAbcNotesOn(noteActive);
		checkBox.setEnabled(trackEnabled);
		drumComboBox.setEnabled(trackEnabled);
		drumComboBox.setVisible(abcPart.getInstrument() == LotroInstrument.BASIC_DRUM);

		if (!noteActive)
		{
			noteGraph.setNoteColor(ColorTable.NOTE_DRUM_OFF);
			noteGraph.setBadNoteColor(ColorTable.NOTE_DRUM_OFF);

			setBackground(ColorTable.GRAPH_BACKGROUND_OFF.get());
			checkBox.setForeground(ColorTable.PANEL_TEXT_OFF.get());
		}
		else if (trackEnabled && abcPart.isDrumEnabled(trackNumber, drumId))
		{
			noteGraph.setNoteColor(ColorTable.NOTE_DRUM_ENABLED);
			noteGraph.setBadNoteColor(ColorTable.NOTE_BAD_ENABLED);

			setBackground(ColorTable.GRAPH_BACKGROUND_ENABLED.get());
			checkBox.setForeground(ColorTable.PANEL_TEXT_ENABLED.get());
		}
		else
		{
			noteGraph.setNoteColor(ColorTable.NOTE_DRUM_OFF);
			noteGraph.setBadNoteColor(ColorTable.NOTE_BAD_OFF);

			setBackground(ColorTable.GRAPH_BACKGROUND_OFF.get());
			checkBox.setForeground(ColorTable.PANEL_TEXT_OFF.get());
		}
	}

	public void setAbcPreviewMode(boolean isAbcPreviewMode)
	{
		if (this.isAbcPreviewMode != isAbcPreviewMode)
		{
			this.isAbcPreviewMode = isAbcPreviewMode;
			updateState();
		}
	}

	private boolean isAbcPreviewMode()
	{
		return abcSequencer != null && isAbcPreviewMode;
	}

	private LotroDrumInfo getSelectedDrum()
	{
		return LotroDrumInfo.getById(abcPart.getDrumMap(trackInfo.getTrackNumber()).get(drumId));
	}

	private class DrumNoteGraph extends NoteGraph
	{
		private boolean showingAbcNotesOn = true;

		public DrumNoteGraph(SequencerWrapper sequencer, TrackInfo trackInfo)
		{
			super(sequencer, trackInfo, -1, 1, 2, 5);
		}

		public void setShowingAbcNotesOn(boolean showingAbcNotesOn)
		{
			if (this.showingAbcNotesOn != showingAbcNotesOn)
			{
				this.showingAbcNotesOn = showingAbcNotesOn;
				repaint();
			}
		}

		@Override protected int transposeNote(int noteId)
		{
			return 0;
		}

		@Override protected boolean isNotePlayable(int noteId)
		{
			return abcPart.isDrumPlayable(trackInfo.getTrackNumber(), drumId);
		}

		@Override protected boolean isShowingNotesOn()
		{
			if (sequencer.isRunning())
				return sequencer.isTrackActive(trackInfo.getTrackNumber());

			if (abcSequencer != null && abcSequencer.isRunning())
				return showingAbcNotesOn;

			return false;
		}

		@Override protected boolean isNoteVisible(NoteEvent ne)
		{
			return ne.note.id == drumId;
		}
	}
}

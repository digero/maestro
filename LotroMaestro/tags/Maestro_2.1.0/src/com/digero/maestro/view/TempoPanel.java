package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.digero.common.midi.Note;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerEvent.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Listener;
import com.digero.common.view.ColorTable;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequenceDataCache;
import com.digero.maestro.midi.SequenceDataCache.TempoEvent;
import com.digero.maestro.midi.SequenceInfo;
import com.sun.media.sound.MidiUtils;

public class TempoPanel extends JPanel implements IDiscardable, TableLayoutConstants
{
	//     0           1              2               3
	//   +---+-------------------+-----------+---------------------+
	//   |   |                   |           |  +---------------+  |
	// 0 |   | Tempo             |   120 BPM |  | (tempo graph) |  |
	//   |   |                   |           |  +---------------+  |
	//   +---+-------------------+-----------+---------------------+

	static final int GUTTER_COLUMN = 0;
	static final int TITLE_COLUMN = 1;
	static final int TEMPO_COLUMN = 2;
	static final int GRAPH_COLUMN = 3;

	private static final int GUTTER_WIDTH = TrackPanel.GUTTER_WIDTH;
	private static final int TITLE_WIDTH = TrackPanel.TITLE_WIDTH;
	private static final int TEMPO_WIDTH = TrackPanel.CONTROL_WIDTH;

	private static final double[] LAYOUT_COLS = new double[] { GUTTER_WIDTH, TITLE_WIDTH, TEMPO_WIDTH, FILL };
	private static final double[] LAYOUT_ROWS = new double[] { 24 };

	private final SequenceInfo sequenceInfo;
	private final SequencerWrapper sequencer;
	private final SequencerWrapper abcSequencer;
	private boolean abcPreviewMode = false;

	private TempoNoteGraph tempoGraph;
	private JLabel currentTempoLabel;

	public TempoPanel(SequenceInfo sequenceInfo, SequencerWrapper sequencer, SequencerWrapper abcSequencer)
	{
		super(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));

		TableLayout tableLayout = (TableLayout) getLayout();
		tableLayout.setHGap(TrackPanel.HGAP);

		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorTable.PANEL_BORDER.get()));

		this.sequenceInfo = sequenceInfo;
		this.sequencer = sequencer;
		this.abcSequencer = abcSequencer;

		int minBPM = 50;
		int maxBPM = 200;
		for (TempoEvent event : sequenceInfo.getDataCache().getTempoEvents().values())
		{
			int bpm = (int) Math.round(MidiUtils.convertTempo(event.tempoMPQ));
			if (bpm < minBPM)
				minBPM = bpm;
			if (bpm > maxBPM)
				maxBPM = bpm;
		}

		JPanel gutter = new JPanel();
		gutter.setOpaque(true);
		gutter.setBackground(ColorTable.PANEL_HIGHLIGHT_OTHER_PART.get());

		this.tempoGraph = new TempoNoteGraph(sequenceInfo, sequencer, minBPM, maxBPM);
		setBackground(ColorTable.GRAPH_BACKGROUND_DISABLED.get());

		JLabel titleLabel = new JLabel("Tempo");
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setForeground(ColorTable.PANEL_TEXT_DISABLED.get());

		currentTempoLabel = new JLabel();
		currentTempoLabel.setForeground(ColorTable.PANEL_TEXT_DISABLED.get());
		updateTempoLabel();

		add(gutter, GUTTER_COLUMN + ", 0");
		add(titleLabel, TITLE_COLUMN + ", 0");
		add(currentTempoLabel, TEMPO_COLUMN + ", 0, R, C");
		add(tempoGraph, GRAPH_COLUMN + ", 0");

		sequencer.addChangeListener(sequencerListener);
		abcSequencer.addChangeListener(sequencerListener);
	}

	@Override public void discard()
	{
		if (sequencer != null)
			sequencer.removeChangeListener(sequencerListener);
		if (abcSequencer != null)
			abcSequencer.removeChangeListener(sequencerListener);
	}

	public void setAbcPreviewMode(boolean abcPreviewMode)
	{
		if (this.abcPreviewMode != abcPreviewMode)
		{
			this.abcPreviewMode = abcPreviewMode;
			updateTempoLabel();
		}
	}

	public boolean isAbcPreviewMode()
	{
		return abcPreviewMode;
	}

	private float getCurrentTempoFactor()
	{
		return isAbcPreviewMode() ? abcSequencer.getTempoFactor() : sequencer.getTempoFactor();
	}

	private int lastRenderedBPM = -1;

	private void updateTempoLabel()
	{
		int mpq = sequenceInfo.getDataCache().getTempoMPQ(sequencer.getThumbTick());
		int bpm = (int) Math.round(MidiUtils.convertTempo(mpq) * getCurrentTempoFactor());
		if (bpm != lastRenderedBPM)
		{
			currentTempoLabel.setText(bpm + " BPM ");
			lastRenderedBPM = bpm;
		}
	}

	private Listener<SequencerEvent> sequencerListener = new Listener<SequencerEvent>()
	{
		@Override public void onEvent(SequencerEvent e)
		{
			if (e.getProperty().isInMask(SequencerProperty.THUMB_POSITION_MASK | SequencerProperty.TEMPO.mask))
				updateTempoLabel();

			if (e.getProperty() == SequencerProperty.IS_RUNNING)
				tempoGraph.repaint();
		}
	};

	private int tempoToNoteId(int tempoMPQ, int minBPM, int maxBPM)
	{
		int bpm = (int) Math.round(MidiUtils.convertTempo(tempoMPQ));

		float tempoFactor = getCurrentTempoFactor();
		minBPM = Math.round(minBPM * tempoFactor);
		maxBPM = Math.round(maxBPM * tempoFactor);
		bpm = Math.round(bpm * tempoFactor);

		return (bpm - minBPM) * (Note.MAX.id - Note.MIN.id) / (maxBPM - minBPM) + Note.MIN.id;
	}

	private class TempoNoteGraph extends NoteGraph
	{
		private final int minBPM;
		private final int maxBPM;
		private List<NoteEvent> events;

		public TempoNoteGraph(SequenceInfo sequenceInfo, SequencerWrapper sequencer, int minBPM, int maxBPM)
		{
			super(sequencer, sequenceInfo, Note.MIN.id - (Note.MIN.id + Note.MAX.id) / 4, Note.MAX.id
					+ (Note.MIN.id + Note.MAX.id) / 4);

			this.minBPM = minBPM;
			this.maxBPM = maxBPM;

			setOctaveLinesVisible(false);
			setNoteColor(ColorTable.NOTE_TEMPO);
			setNoteOnColor(ColorTable.NOTE_TEMPO_ON);
			setNoteOnExtraHeightPix(0);
			setNoteOnOutlineWidthPix(0);
		}

		private void recalcTempoEvents()
		{
			// Make fake note events for every tempo event
			events = new ArrayList<NoteEvent>();
			TempoEvent prevEvent = null;
			SequenceDataCache dataCache = sequenceInfo.getDataCache();
			for (TempoEvent event : dataCache.getTempoEvents().values())
			{
				if (prevEvent != null)
				{
					int id = tempoToNoteId(prevEvent.tempoMPQ, minBPM, maxBPM);
					events.add(new NoteEvent(Note.fromId(id), 127, prevEvent.tick, event.tick, dataCache));
				}
				prevEvent = event;
			}

			if (prevEvent != null)
			{
				int id = tempoToNoteId(prevEvent.tempoMPQ, minBPM, maxBPM);
				events.add(new NoteEvent(Note.fromId(id), 127, prevEvent.tick, dataCache.getSongLengthTicks(),
						dataCache));
			}
			else
			{
				int id = tempoToNoteId(sequenceInfo.getPrimaryTempoMPQ(), minBPM, maxBPM);
				events.add(new NoteEvent(Note.fromId(id), 127, 0, dataCache.getSongLengthTicks(), dataCache));
			}
		}

		@Override protected boolean isShowingNotesOn()
		{
			return sequencer.isRunning() || abcSequencer.isRunning();
		}

		@Override protected List<NoteEvent> getEvents()
		{
			if (events == null)
				recalcTempoEvents();
			return events;
		}
	}
}

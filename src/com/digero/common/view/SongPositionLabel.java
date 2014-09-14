package com.digero.common.view;

import javax.swing.JLabel;

import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerEvent.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Listener;
import com.digero.common.util.Util;

public class SongPositionLabel extends JLabel implements Listener<SequencerEvent>, IDiscardable
{
	private SequencerWrapper sequencer;
	private boolean adjustForTempo;
	private long initialOffsetTick = 0;

	public SongPositionLabel(SequencerWrapper sequencer)
	{
		this(sequencer, false);
	}

	public SongPositionLabel(SequencerWrapper sequencer, boolean adjustForTempo)
	{
		this.sequencer = sequencer;
		this.adjustForTempo = adjustForTempo;
		sequencer.addChangeListener(this);
		update();
	}

	@Override public void discard()
	{
		if (sequencer != null)
		{
			sequencer.removeChangeListener(this);
		}
	}

	public long getInitialOffsetTick()
	{
		return initialOffsetTick;
	}

	public void setInitialOffsetTick(long initialOffsetTick)
	{
		if (this.initialOffsetTick != initialOffsetTick)
		{
			this.initialOffsetTick = initialOffsetTick;
			update();
		}
	}

	@Override public void onEvent(SequencerEvent evt)
	{
		SequencerProperty p = evt.getProperty();
		if (p.isInMask(SequencerProperty.THUMB_POSITION_MASK | SequencerProperty.LENGTH.mask
				| SequencerProperty.TEMPO.mask))
		{
			update();
		}
	}

	private long lastPrintedMicros = -1;
	private long lastPrintedLength = -1;

	private void update()
	{
		long tickLength = Math.max(0, sequencer.getTickLength() - initialOffsetTick);
		long tick = Math.max(0, Math.min(tickLength, sequencer.getThumbTick() - initialOffsetTick));

		if (adjustForTempo)
		{
			tick = Math.round(tick / sequencer.getTempoFactor());
			tickLength = Math.round(tickLength / sequencer.getTempoFactor());
		}

		long micros = sequencer.tickToMicros(tick);
		long length = sequencer.tickToMicros(tickLength);
		if (micros != lastPrintedMicros || length != lastPrintedLength)
		{
			lastPrintedMicros = micros;
			lastPrintedLength = length;
			setText(Util.formatDuration(micros, length) + "/" + Util.formatDuration(length, length));
		}
	}
}

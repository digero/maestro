package com.digero.common.view;

import javax.swing.JLabel;

import com.digero.common.midi.IBarNumberCache;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerEvent.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Listener;

public class BarNumberLabel extends JLabel implements Listener<SequencerEvent>, IDiscardable
{
	private IBarNumberCache barNumberCache;
	private SequencerWrapper sequencer;
	private long initialOffsetTick = 0;

	public BarNumberLabel(SequencerWrapper sequencer, IBarNumberCache barNumberCache)
	{
		this.sequencer = sequencer;
		this.barNumberCache = barNumberCache;

		sequencer.addChangeListener(this);
	}

	@Override public void discard()
	{
		if (sequencer != null)
			sequencer.removeChangeListener(this);
	}

	public IBarNumberCache getBarNumberCache()
	{
		return barNumberCache;
	}

	public void setBarNumberCache(IBarNumberCache barNumberCache)
	{
		if (this.barNumberCache != barNumberCache)
		{
			this.barNumberCache = barNumberCache;
			update();
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
				| SequencerProperty.TEMPO.mask | SequencerProperty.SEQUENCE.mask))
		{
			update();
		}
	}

	private int lastPrintedBarNumber = -1;
	private int lastPrintedBarCount = -1;

	private void update()
	{
		if (barNumberCache == null)
		{
			if (lastPrintedBarNumber != -1 || lastPrintedBarCount != -1)
			{
				lastPrintedBarNumber = -1;
				lastPrintedBarCount = -1;
				setText("");
			}
			return;
		}

		long tickLength = Math.max(0, sequencer.getTickLength() - initialOffsetTick);
		long tick = Math.min(tickLength, sequencer.getThumbTick() - initialOffsetTick);

		int barNumber = (tick < 0) ? 0 : (barNumberCache.tickToBarNumber(tick) + 1);
		int barCount = barNumberCache.tickToBarNumber(tickLength) + 1;

		if (barNumber != lastPrintedBarNumber || barCount != lastPrintedBarCount)
		{
			lastPrintedBarNumber = barNumber;
			lastPrintedBarCount = barCount;
			setText(barNumber + "/" + barCount);
		}
	}
}

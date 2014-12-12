package com.digero.maestro.abc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.digero.common.midi.IBarNumberCache;
import com.digero.common.midi.ITempoCache;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.Util;
import com.digero.maestro.midi.SequenceDataCache;
import com.digero.maestro.midi.SequenceInfo;
import com.sun.media.sound.MidiUtils;

public class QuantizedTimingInfo implements ITempoCache, IBarNumberCache
{
	// Tick => TimingInfoEvent
	private final NavigableMap<Long, TimingInfoEvent> timingInfoByTick = new TreeMap<Long, TimingInfoEvent>();

	private NavigableSet<Long> barStartTicks = null;
	private Long[] barStartTickByBar = null;
	private final long songLengthTicks;
	private final int tickResolution;

	private final int primaryTempoMPQ;
	private final float exportTempoFactor;
	private final TimeSignature meter;
	private final boolean tripletTiming;

	public QuantizedTimingInfo(SequenceInfo source, float exportTempoFactor, TimeSignature meter,
			boolean useTripletTiming) throws AbcConversionException
	{
		double exportPrimaryTempoMPQ = TimingInfo.roundTempoMPQ(source.getPrimaryTempoMPQ() / exportTempoFactor);
		this.primaryTempoMPQ = (int) Math.round(exportPrimaryTempoMPQ * exportTempoFactor);
		this.exportTempoFactor = exportTempoFactor;
		this.meter = meter;
		this.tripletTiming = useTripletTiming;
		this.tickResolution = source.getDataCache().getTickResolution();
		this.songLengthTicks = source.getDataCache().getSongLengthTicks();
		final int resolution = source.getDataCache().getTickResolution();

		TimingInfo defaultTiming = new TimingInfo(source.getPrimaryTempoMPQ(), resolution, exportTempoFactor, meter,
				useTripletTiming);
		timingInfoByTick.put(0L, new TimingInfoEvent(0, 0, 0, defaultTiming));

		Collection<TimingInfoEvent> reversedEvents = timingInfoByTick.descendingMap().values();

		/* Go through the tempo events from the MIDI file and quantize them so each event starts at
		 * an integral multiple of the previous event's MinNoteLengthTicks. This ensures that we can
		 * split notes at each tempo change without creating a note that is shorter than
		 * MinNoteLengthTicks. */
		for (SequenceDataCache.TempoEvent sourceEvent : source.getDataCache().getTempoEvents().values())
		{
			long tick = 0;
			long micros = 0;
			double barNumber = 0;
			TimingInfo info = new TimingInfo(sourceEvent.tempoMPQ, resolution, exportTempoFactor, meter,
					useTripletTiming);

			// Iterate over the existing events in reverse order
			Iterator<TimingInfoEvent> reverseIterator = reversedEvents.iterator();
			while (reverseIterator.hasNext())
			{
				TimingInfoEvent prev = reverseIterator.next();
				assert prev.tick <= sourceEvent.tick;

				long gridUnitTicks = prev.info.getMinNoteLengthTicks();

				// Quantize the tick length to the floor multiple of gridUnitTicks
				long lengthTicks = Util.floorGrid(sourceEvent.tick - prev.tick, gridUnitTicks);

				/* If the new event has a coarser timing grid than prev, then it's possible that the
				 * bar splits will not align to the grid. To avoid this, adjust the length so that
				 * the new event starts at a time that will allow the bar to land on the
				 * quantization grid. */
				while (lengthTicks > 0)
				{
					double barNumberTmp = prev.barNumber + lengthTicks / ((double) prev.info.getBarLengthTicks());
					double gridUnitsRemaining = ((Math.ceil(barNumberTmp) - barNumberTmp) * info.getBarLengthTicks())
							/ info.getMinNoteLengthTicks();

					final double epsilon = TimingInfo.MIN_TEMPO_BPM / (2.0 * TimingInfo.MAX_TEMPO_BPM);
					if (Math.abs(gridUnitsRemaining - Math.round(gridUnitsRemaining)) <= epsilon)
						break; // Ok, the bar ends on the grid

					lengthTicks -= gridUnitTicks;
				}

				if (lengthTicks <= 0)
				{
					// The prev tempo event was quantized to zero-length; remove it
					reverseIterator.remove();
					continue;
				}

				tick = prev.tick + lengthTicks;
				micros = prev.micros + MidiUtils.ticks2microsec(lengthTicks, prev.info.getTempoMPQ(), resolution);
				barNumber = prev.barNumber + lengthTicks / ((double) prev.info.getBarLengthTicks());
				break;
			}

			TimingInfoEvent event = new TimingInfoEvent(tick, micros, barNumber, info);

			timingInfoByTick.put(tick, event);
		}
	}

	public int getPrimaryTempoMPQ()
	{
		return primaryTempoMPQ;
	}

	public int getPrimaryTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getPrimaryTempoMPQ()));
	}

	public int getPrimaryExportTempoMPQ()
	{
		return (int) Math.round(primaryTempoMPQ / exportTempoFactor);
	}

	public int getPrimaryExportTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo((double) primaryTempoMPQ / exportTempoFactor));
	}

	public float getExportTempoFactor()
	{
		return exportTempoFactor;
	}

	public TimeSignature getMeter()
	{
		return meter;
	}

	public boolean isTripletTiming()
	{
		return tripletTiming;
	}

	public TimingInfo getTimingInfo(long tick)
	{
		return getTimingEventForTick(tick).info;
	}

	public long quantize(long tick)
	{
		TimingInfoEvent e = getTimingEventForTick(tick);
		return e.tick + Util.roundGrid(tick - e.tick, e.info.getMinNoteLengthTicks());
	}

	@Override public long tickToMicros(long tick)
	{
		TimingInfoEvent e = getTimingEventForTick(tick);
		return e.micros + MidiUtils.ticks2microsec(tick - e.tick, e.info.getTempoMPQ(), e.info.getResolutionPPQ());
	}

	@Override public long microsToTick(long micros)
	{
		TimingInfoEvent e = getTimingEventForMicros(micros);
		return e.tick + MidiUtils.microsec2ticks(micros - e.micros, e.info.getTempoMPQ(), e.info.getResolutionPPQ());
	}

	@Override public int tickToBarNumber(long tick)
	{
		TimingInfoEvent e = getTimingEventForTick(tick);
		return (int) Math.floor(e.barNumber + (tick - e.tick) / ((double) e.info.getBarLengthTicks()));
	}

	public long tickToBarStartTick(long tick)
	{
		if (barStartTicks == null)
			calcBarStarts();

		if (tick <= barStartTicks.last())
			return barStartTicks.floor(tick);

		return barNumberToBarStartTick(tickToBarNumber(tick));
	}

	public long tickToBarEndTick(long tick)
	{
		if (barStartTicks == null)
			calcBarStarts();

		Long endTick = barStartTicks.higher(tick);
		if (endTick != null)
			return endTick;

		return barNumberToBarEndTick(tickToBarNumber(tick));
	}

	public long barNumberToBarStartTick(int barNumber)
	{
		if (barStartTickByBar == null)
			calcBarStarts();

		if (barNumber < barStartTickByBar.length)
			return barStartTickByBar[barNumber];

		TimingInfoEvent e = timingInfoByTick.lastEntry().getValue();
		return e.tick + Math.round((barNumber - e.barNumber) * e.info.getBarLengthTicks());
	}

	public long barNumberToBarEndTick(int barNumber)
	{
		return barNumberToBarStartTick(barNumber + 1);
	}

	public long barNumberToMicrosecond(int barNumber)
	{
		return tickToMicros(barNumberToBarStartTick(barNumber));
	}

	public int getMidiResolution()
	{
		return tickResolution;
	}

	private void calcBarStarts()
	{
		barStartTicks = new TreeSet<Long>();
		barStartTicks.add(0L);
		TimingInfoEvent prev = null;
		for (TimingInfoEvent event : timingInfoByTick.values())
		{
			if (prev != null)
			{
				// Calculate the start time for all bars that start between prev and event
				long barStart = prev.tick
						+ Math.round((Math.ceil(prev.barNumber) - prev.barNumber) * prev.info.getBarLengthTicks());
				while (barStart < event.tick)
				{
					barStartTicks.add(barStart);
					barStart += prev.info.getBarLengthTicks();
				}
			}
			prev = event;
		}

		// Calculate bar starts for all bars after the last tempo change
		long barStart = prev.tick
				+ Math.round((Math.ceil(prev.barNumber) - prev.barNumber) * prev.info.getBarLengthTicks());
		while (barStart <= songLengthTicks)
		{
			barStartTicks.add(barStart);
			barStart += prev.info.getBarLengthTicks();
		}
		barStartTicks.add(barStart);

		barStartTickByBar = barStartTicks.toArray(new Long[0]);
	}

	TimingInfoEvent getTimingEventForTick(long tick)
	{
		return timingInfoByTick.floorEntry(tick).getValue();
	}

	TimingInfoEvent getTimingEventForMicros(long micros)
	{
		TimingInfoEvent retVal = timingInfoByTick.firstEntry().getValue();
		for (TimingInfoEvent event : timingInfoByTick.values())
		{
			if (event.micros > micros)
				break;

			retVal = event;
		}
		return retVal;
	}

	TimingInfoEvent getNextTimingEvent(long tick)
	{
		Map.Entry<Long, TimingInfoEvent> entry = timingInfoByTick.higherEntry(tick);
		return (entry == null) ? null : entry.getValue();
	}

	NavigableMap<Long, TimingInfoEvent> getTimingInfoByTick()
	{
		return timingInfoByTick;
	}

	class TimingInfoEvent
	{
		public final long tick;
		public final long micros;
		public final double barNumber; // May start in the middle of a bar

		public final TimingInfo info;

		public TimingInfoEvent(long tick, long micros, double barNumber, TimingInfo info)
		{
			this.tick = tick;
			this.micros = micros;
			this.barNumber = barNumber;
			this.info = info;
		}
	}
}

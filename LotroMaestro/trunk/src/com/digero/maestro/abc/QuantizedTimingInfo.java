package com.digero.maestro.abc;

import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.digero.common.midi.TimeSignature;
import com.digero.maestro.midi.ITempoCache;
import com.digero.maestro.midi.SequenceDataCache;
import com.sun.media.sound.MidiUtils;

public class QuantizedTimingInfo implements ITempoCache
{
	// Tick => TimingInfoEvent
	private final NavigableMap<Long, TimingInfoEvent> timingInfoByTick = new TreeMap<Long, TimingInfoEvent>();

//	// Bar number => TimingInfoEvent
//	private final NavigableMap<Double, TimingInfoEvent> timingInfoByBar = new TreeMap<Double, TimingInfoEvent>();

	private NavigableSet<Long> barStartTicks = null;
	private Long[] barStartTickByBar = null;
	private final long songLengthTicks;
	private final int tickResolution;

	private final int primaryTempoBPM;
	private final float exportTempoFactor;
	private final TimeSignature meter;
	private final boolean tripletTiming;

	public QuantizedTimingInfo(SequenceDataCache source, float exportTempoFactor, TimeSignature meter,
			boolean useTripletTiming) throws AbcConversionException
	{
		this.primaryTempoBPM = source.getPrimaryTempoBPM();
		this.exportTempoFactor = exportTempoFactor;
		this.meter = meter;
		this.tripletTiming = useTripletTiming;
		this.tickResolution = source.getTickResolution();
		this.songLengthTicks = source.getSongLengthTicks();
		final int resolution = source.getTickResolution();

		TimingInfo defaultTiming = new TimingInfo(source.getPrimaryTempoMPQ(), resolution, exportTempoFactor,
				meter, useTripletTiming);
		timingInfoByTick.put(0L, new TimingInfoEvent(0, 0, 0, defaultTiming));

		/*
		 * Go through the tempo events from the MIDI file and quantize them so
		 * each event starts at an integral multiple of the previous event's
		 * MinNoteLengthTicks. This ensures that we can split notes at each
		 * tempo change without creating a note that is shorter than
		 * MinNoteLengthTicks.
		 */
		for (SequenceDataCache.TempoEvent sourceEvent : source.getTempoEvents().values())
		{
			long tick = 0;
			long micros = 0;
			double barNumber = 0;
			TimingInfo info = new TimingInfo(sourceEvent.tempoMPQ, resolution, exportTempoFactor, meter,
					useTripletTiming);

			Map.Entry<Long, TimingInfoEvent> previousEntry = timingInfoByTick.lowerEntry(sourceEvent.tick);
			if (previousEntry != null)
			{
				TimingInfoEvent prev = previousEntry.getValue();
				long minTickLength = prev.info.getMinNoteLengthTicks();

				// Quantize the tick length to the nearest multiple of minTickLength
				long tickLength = ((sourceEvent.tick - prev.tick + minTickLength / 2) / minTickLength) * minTickLength;

				tick = prev.tick + tickLength;
				micros = prev.micros + MidiUtils.ticks2microsec(tickLength, prev.info.getTempoMPQ(), resolution);
				barNumber = prev.barNumber + tickLength / ((double) prev.info.getBarLengthTicks());
			}

			TimingInfoEvent event = new TimingInfoEvent(tick, micros, barNumber, info);

			timingInfoByTick.put(tick, event);
//			timingInfoByBar.put(barNumber, event);
		}
	}

	public int getPrimaryTempoBPM()
	{
		return primaryTempoBPM;
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
		long grid = getTimingInfo(tick).getMinNoteLengthTicks();
		return ((tick + grid / 2) / grid) * grid;
	}

	@Override public long tickToMicros(long tick)
	{
		TimingInfoEvent e = getTimingEventForTick(tick);
		return e.micros + MidiUtils.ticks2microsec(tick - e.tick, e.info.getTempoMPQ(), e.info.getResolutionPPQ());
	}

	@Override public long microsToTick(long micros)
	{
		TimingInfoEvent e = getTimingEventForMicros(micros);
		return e.tick + MidiUtils.ticks2microsec(micros - e.micros, e.info.getTempoMPQ(), e.info.getResolutionPPQ());
	}

	public int tickToBarNumber(long tick)
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
//		TimingInfoEvent e = timingInfoByBar.floorEntry((double) barNumber).getValue();
//		return e.tick + Math.round((barNumber - e.barNumber) * e.info.getBarLengthTicks());

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

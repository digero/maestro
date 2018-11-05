package com.digero.maestro.midi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.digero.common.midi.IBarNumberCache;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.ITempoCache;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.Util;
import com.digero.maestro.abc.TimingInfo;
import com.sun.media.sound.MidiUtils;

public class SequenceDataCache implements MidiConstants, ITempoCache, IBarNumberCache
{
	private final int tickResolution;
	private final float divisionType;
	private final int primaryTempoMPQ;
	private final int minTempoMPQ;
	private final int maxTempoMPQ;
	private final TimeSignature timeSignature;
	private NavigableMap<Long, TempoEvent> tempo = new TreeMap<Long, TempoEvent>();

	private final long songLengthTicks;

	private MapByChannel instruments = new MapByChannel(DEFAULT_INSTRUMENT);
	private MapByChannel volume = new MapByChannel(DEFAULT_CHANNEL_VOLUME);
	private MapByChannel pitchBendCoarse = new MapByChannel(DEFAULT_PITCH_BEND_RANGE_SEMITONES);
	private MapByChannel pitchBendFine = new MapByChannel(DEFAULT_PITCH_BEND_RANGE_CENTS);

	public SequenceDataCache(Sequence song)
	{
		Map<Integer, Long> tempoLengths = new HashMap<Integer, Long>();

		tempo.put(0L, TempoEvent.DEFAULT_TEMPO);
		int minTempoMPQ = Integer.MAX_VALUE;
		int maxTempoMPQ = Integer.MIN_VALUE;
		TimeSignature timeSignature = null;

		divisionType = song.getDivisionType();
		tickResolution = song.getResolution();

		// Keep track of the active registered paramater number for pitch bend range
		int[] rpn = new int[CHANNEL_COUNT];
		Arrays.fill(rpn, REGISTERED_PARAM_NONE);

		Track[] tracks = song.getTracks();
		long lastTick = 0;
		for (int iTrack = 0; iTrack < tracks.length; iTrack++)
		{
			Track track = tracks[iTrack];

			for (int j = 0, sz = track.size(); j < sz; j++)
			{
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();
				long tick = evt.getTick();
				if (tick > lastTick)
					lastTick = tick;

				if (msg instanceof ShortMessage)
				{
					ShortMessage m = (ShortMessage) msg;
					int cmd = m.getCommand();
					int ch = m.getChannel();

					if (cmd == ShortMessage.PROGRAM_CHANGE)
					{
						if (ch != DRUM_CHANNEL)
						{
							instruments.put(ch, tick, m.getData1());
						}
					}
					else if (cmd == ShortMessage.CONTROL_CHANGE)
					{
						switch (m.getData1())
						{
						case CHANNEL_VOLUME_CONTROLLER_COARSE:
							volume.put(ch, tick, m.getData2());
							break;
						case REGISTERED_PARAMETER_NUMBER_MSB:
							rpn[ch] = (rpn[ch] & 0x7F) | ((m.getData2() & 0x7F) << 7);
							break;
						case REGISTERED_PARAMETER_NUMBER_LSB:
							rpn[ch] = (rpn[ch] & (0x7F << 7)) | (m.getData2() & 0x7F);
							break;
						case DATA_ENTRY_COARSE:
							if (rpn[ch] == REGISTERED_PARAM_PITCH_BEND_RANGE)
								pitchBendCoarse.put(ch, tick, m.getData2());
							break;
						case DATA_ENTRY_FINE:
							if (rpn[ch] == REGISTERED_PARAM_PITCH_BEND_RANGE)
								pitchBendFine.put(ch, tick, m.getData2());
							break;
						}
					}
				}
				else if (iTrack == 0 && (divisionType == Sequence.PPQ) && MidiUtils.isMetaTempo(msg))
				{
					TempoEvent te = getTempoEventForTick(tick);
					long elapsedMicros = MidiUtils.ticks2microsec(tick - te.tick, te.tempoMPQ, tickResolution);
					tempoLengths.put(te.tempoMPQ, elapsedMicros + Util.valueOf(tempoLengths.get(te.tempoMPQ), 0));
					tempo.put(tick, new TempoEvent(MidiUtils.getTempoMPQ(msg), tick, te.micros + elapsedMicros));

					if (te.tempoMPQ < minTempoMPQ)
						minTempoMPQ = te.tempoMPQ;
					if (te.tempoMPQ > maxTempoMPQ)
						maxTempoMPQ = te.tempoMPQ;
				}
				else if (msg instanceof MetaMessage)
				{
					MetaMessage m = (MetaMessage) msg;
					if (m.getType() == META_TIME_SIGNATURE && timeSignature == null)
					{
						timeSignature = new TimeSignature(m);
					}
				}
			}
		}

		// Account for the duration of the final tempo
		TempoEvent te = getTempoEventForTick(lastTick);
		long elapsedMicros = MidiUtils.ticks2microsec(lastTick - te.tick, te.tempoMPQ, tickResolution);
		tempoLengths.put(te.tempoMPQ, elapsedMicros + Util.valueOf(tempoLengths.get(te.tempoMPQ), 0));

		Entry<Integer, Long> max = null;
		for (Entry<Integer, Long> entry : tempoLengths.entrySet())
		{
			if (max == null || entry.getValue() > max.getValue())
				max = entry;
		}
		primaryTempoMPQ = (max == null) ? DEFAULT_TEMPO_MPQ : max.getKey();

		this.minTempoMPQ = (minTempoMPQ == Integer.MAX_VALUE) ? DEFAULT_TEMPO_MPQ : minTempoMPQ;
		this.maxTempoMPQ = (maxTempoMPQ == Integer.MIN_VALUE) ? DEFAULT_TEMPO_MPQ : maxTempoMPQ;
		this.timeSignature = (timeSignature == null) ? TimeSignature.FOUR_FOUR : timeSignature;

		songLengthTicks = lastTick;
	}

	public int getInstrument(int channel, long tick)
	{
		return instruments.get(channel, tick);
	}

	public int getVolume(int channel, long tick)
	{
		return volume.get(channel, tick);
	}

	public double getPitchBendRange(int channel, long tick)
	{
		return pitchBendCoarse.get(channel, tick) + (pitchBendFine.get(channel, tick) / 100.0);
	}

	public long getSongLengthTicks()
	{
		return songLengthTicks;
	}

	@Override public long tickToMicros(long tick)
	{
		if (divisionType != Sequence.PPQ)
			return (long) (TimingInfo.ONE_SECOND_MICROS * ((double) tick / (double) (divisionType * tickResolution)));

		TempoEvent te = getTempoEventForTick(tick);
		return te.micros + MidiUtils.ticks2microsec(tick - te.tick, te.tempoMPQ, tickResolution);
	}

	@Override public long microsToTick(long micros)
	{
		if (divisionType != Sequence.PPQ)
			return (long) (divisionType * tickResolution * micros / (double) TimingInfo.ONE_SECOND_MICROS);

		TempoEvent te = getTempoEventForMicros(micros);
		return te.tick + MidiUtils.microsec2ticks(micros - te.micros, te.tempoMPQ, tickResolution);
	}

	public int getTempoMPQ(long tick)
	{
		return getTempoEventForTick(tick).tempoMPQ;
	}

	public int getTempoBPM(long tick)
	{
		return (int) Math.round(MidiUtils.convertTempo(getTempoMPQ(tick)));
	}

	public int getPrimaryTempoMPQ()
	{
		return primaryTempoMPQ;
	}

	public int getPrimaryTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getPrimaryTempoMPQ()));
	}

	public int getMinTempoMPQ()
	{
		return minTempoMPQ;
	}

	public int getMinTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getMinTempoMPQ()));
	}

	public int getMaxTempoMPQ()
	{
		return maxTempoMPQ;
	}

	public int getMaxTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getMaxTempoMPQ()));
	}

	public int getTickResolution()
	{
		return tickResolution;
	}

	public TimeSignature getTimeSignature()
	{
		return timeSignature;
	}

	public long getBarLengthTicks()
	{
		// tickResolution is in ticks per quarter note
		return 4L * tickResolution * timeSignature.numerator / timeSignature.denominator;
	}

	@Override public int tickToBarNumber(long tick)
	{
		return (int) (tick / getBarLengthTicks());
	}

	public NavigableMap<Long, TempoEvent> getTempoEvents()
	{
		return tempo;
	}

	/**
	 * Tempo Handling
	 */
	public static class TempoEvent
	{
		private TempoEvent(int tempoMPQ, long startTick, long startMicros)
		{
			this.tempoMPQ = tempoMPQ;
			this.tick = startTick;
			this.micros = startMicros;
		}

		public static final TempoEvent DEFAULT_TEMPO = new TempoEvent(DEFAULT_TEMPO_MPQ, 0, 0);

		public final int tempoMPQ;
		public final long tick;
		public final long micros;
	}

	public TempoEvent getTempoEventForTick(long tick)
	{
		Entry<Long, TempoEvent> entry = tempo.floorEntry(tick);
		if (entry != null)
			return entry.getValue();

		return TempoEvent.DEFAULT_TEMPO;
	}

	public TempoEvent getTempoEventForMicros(long micros)
	{
		TempoEvent prev = TempoEvent.DEFAULT_TEMPO;
		for (TempoEvent event : tempo.values())
		{
			if (event.micros > micros)
				break;

			prev = event;
		}
		return prev;
	}

	/**
	 * Map by channel
	 */
	private static class MapByChannel
	{
		private NavigableMap<Long, Integer>[] map;
		private int defaultValue;

		@SuppressWarnings("unchecked")//
		public MapByChannel(int defaultValue)
		{
			map = new NavigableMap[CHANNEL_COUNT];
			this.defaultValue = defaultValue;
		}

		public void put(int channel, long tick, Integer value)
		{
			if (map[channel] == null)
				map[channel] = new TreeMap<Long, Integer>();

			map[channel].put(tick, value);
		}

		public int get(int channel, long tick)
		{
			if (map[channel] == null)
				return defaultValue;

			Entry<Long, Integer> entry = map[channel].floorEntry(tick);
			if (entry == null) // No changes before this tick
				return defaultValue;

			return entry.getValue();
		}
	}
}

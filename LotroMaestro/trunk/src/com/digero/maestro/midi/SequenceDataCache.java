package com.digero.maestro.midi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.digero.common.midi.IMidiConstants;
import com.sun.media.sound.MidiUtils;

public class SequenceDataCache implements IMidiConstants {
	private final int primaryTempoMPQ;
	private NavigableMap<Long, Integer> tempo = new TreeMap<Long, Integer>();

	private MapByChannel instruments = new MapByChannel();
	private MapByChannel volume = new MapByChannel();
	private MapByChannel pitchBendCoarse = new MapByChannel();
	private MapByChannel pitchBendFine = new MapByChannel();

	public SequenceDataCache(Sequence song) {
		Map<Integer, Long> tempoLengths = new HashMap<Integer, Long>();
		boolean isPPQ = song.getDivisionType() == Sequence.PPQ;

		int[] rpnCoarse = new int[CHANNEL_COUNT];
		int[] rpnFine = new int[CHANNEL_COUNT];
		Arrays.fill(rpnCoarse, REGISTERED_PARAM_NONE);
		Arrays.fill(rpnFine, REGISTERED_PARAM_NONE);

		Track[] tracks = song.getTracks();
		for (int i = 0; i < tracks.length; i++) {
			Track track = tracks[i];

			for (int j = 0, sz = track.size(); j < sz; j++) {
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();
				long tick = evt.getTick();

				if (msg instanceof ShortMessage) {
					ShortMessage m = (ShortMessage) msg;
					int cmd = m.getCommand();
					int ch = m.getChannel();

					if (cmd == ShortMessage.PROGRAM_CHANGE) {
						if (ch != DRUM_CHANNEL) {
							instruments.put(ch, tick, m.getData1());
						}
					}
					else if (cmd == ShortMessage.CONTROL_CHANGE) {
						switch (m.getData1()) {
						case CHANNEL_VOLUME_CONTROLLER_COARSE:
							volume.put(ch, tick, m.getData2());
							break;
						case REGISTERED_PARAMETER_NUMBER_COARSE:
							rpnCoarse[ch] = m.getData2();
							break;
						case REGISTERED_PARAMETER_NUMBER_FINE:
							rpnFine[ch] = m.getData2();
							break;
						case DATA_ENTRY_COARSE:
							if (rpnCoarse[ch] == REGISTERED_PARAM_PITCH_BEND_RANGE)
								pitchBendCoarse.put(ch, tick, m.getData2());
							break;
						case DATA_ENTRY_FINE:
							if (rpnFine[ch] == REGISTERED_PARAM_PITCH_BEND_RANGE)
								pitchBendFine.put(ch, tick, m.getData2());
							break;
						}
					}
				}
				else if (i == 0 && isPPQ && MidiUtils.isMetaTempo(msg)) {
					Entry<Long, Integer> prevTempo = tempo.floorEntry(tick);
					long prevTempoTick;
					int prevTempoMPQ;
					if (prevTempo == null) {
						prevTempoTick = 0;
						prevTempoMPQ = DEFAULT_TEMPO_MPQ;
					}
					else {
						prevTempoTick = prevTempo.getKey();
						prevTempoMPQ = prevTempo.getValue();
					}

					if (tick > prevTempoTick) {
						Long uSecObj = tempoLengths.get(prevTempoMPQ);
						long uSec = (uSecObj != null) ? uSecObj : 0;
						uSec += MidiUtils.ticks2microsec(tick, prevTempoMPQ, song.getResolution());
						tempoLengths.put(prevTempoMPQ, uSec);
					}

					tempo.put(tick, MidiUtils.getTempoMPQ(msg));
				}
			}
		}

		Entry<Integer, Long> max = null;
		for (Entry<Integer, Long> entry : tempoLengths.entrySet()) {
			if (max == null || entry.getValue() > max.getValue())
				max = entry;
		}
		primaryTempoMPQ = (max == null) ? DEFAULT_TEMPO_MPQ : max.getKey();
	}

	public int getInstrument(int channel, long tick) {
		return instruments.get(channel, tick, DEFAULT_INSTRUMENT);
	}

	public int getVolume(int channel, long tick) {
		return volume.get(channel, tick, DEFAULT_CHANNEL_VOLUME);
	}

	public int getPitchBend(MidiEvent evt) {
		if (!(evt.getMessage() instanceof ShortMessage))
			return 0;
		ShortMessage m = (ShortMessage) evt.getMessage();
		if (m.getCommand() != ShortMessage.PITCH_BEND)
			return 0;

		double range = pitchBendCoarse.get(m.getChannel(), evt.getTick(), DEFAULT_PITCH_BEND_RANGE_SEMITONES) + 
				pitchBendFine.get(m.getChannel(), evt.getTick(), DEFAULT_PITCH_BEND_RANGE_CENTS) / 100.0;
		
		double pct = 2 * (((m.getData1() | (m.getData2() << 7)) / (double) (1 << 14)) - 0.5);

		return (int) Math.round(pct * range);
	}

	public int getTempoMPQ(long tick) {
		Entry<Long, Integer> entry = tempo.floorEntry(tick);
		if (entry == null) // No changes before this tick
			return DEFAULT_TEMPO_MPQ;

		return entry.getValue();
	}

	public int getTempoBPM(long tick) {
		return (int) Math.round(MidiUtils.convertTempo(getTempoMPQ(tick)));
	}

	public int getPrimaryTempoMPQ() {
		return primaryTempoMPQ;
	}

	public int getPrimaryTempoBPM() {
		return (int) Math.round(MidiUtils.convertTempo(getPrimaryTempoMPQ()));
	}

	private static class MapByChannel {
		private NavigableMap<Long, Integer>[] map;

		@SuppressWarnings("unchecked")
		public MapByChannel() {
			map = new NavigableMap[CHANNEL_COUNT];
		}

		public void put(int channel, long tick, Integer value) {
			if (map[channel] == null)
				map[channel] = new TreeMap<Long, Integer>();

			map[channel].put(tick, value);
		}

		public int get(int channel, long tick, Integer defaultValue) {
			if (map[channel] == null)
				return defaultValue;

			Entry<Long, Integer> entry = map[channel].floorEntry(tick);
			if (entry == null) // No changes before this tick
				return defaultValue;

			return entry.getValue();
		}
	}
}

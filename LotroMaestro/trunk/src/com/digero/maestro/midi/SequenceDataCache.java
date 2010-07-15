package com.digero.maestro.midi;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

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

	public SequenceDataCache(Sequence song) {
		Map<Integer, Long> tempoLengths = new HashMap<Integer, Long>();
		boolean isPPQ = song.getDivisionType() == Sequence.PPQ;

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
						if (m.getData1() == COARSE_CHANNEL_VOLUME_CONTROLLER) {
							volume.put(ch, tick, m.getData2());
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

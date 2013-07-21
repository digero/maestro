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

	private MapByChannel instruments = new MapByChannel(DEFAULT_INSTRUMENT);
	private MapByChannel volume = new MapByChannel(DEFAULT_CHANNEL_VOLUME);
	private MapByChannel pitchBendCoarse = new MapByChannel(DEFAULT_PITCH_BEND_RANGE_SEMITONES);
	private MapByChannel pitchBendFine = new MapByChannel(DEFAULT_PITCH_BEND_RANGE_CENTS);

	public SequenceDataCache(Sequence song) {
		Map<Integer, Long> tempoLengths = new HashMap<Integer, Long>();
		boolean isPPQ = song.getDivisionType() == Sequence.PPQ;

		// Keep track of the active registered paramater number for pitch bend range
		int[] rpn = new int[CHANNEL_COUNT];
		Arrays.fill(rpn, REGISTERED_PARAM_NONE);

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
		return instruments.get(channel, tick);
	}

	public int getVolume(int channel, long tick) {
		return volume.get(channel, tick);
	}

	public double getPitchBendRange(int channel, long tick) {
		return pitchBendCoarse.get(channel, tick) + (pitchBendFine.get(channel, tick) / 100.0);
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

	public NavigableMap<Long, Integer> getTickToTempoMPQMap() {
		return tempo;
	}

	private static class MapByChannel {
		private NavigableMap<Long, Integer>[] map;
		private int defaultValue;

		@SuppressWarnings("unchecked")
		public MapByChannel(int defaultValue) {
			map = new NavigableMap[CHANNEL_COUNT];
			this.defaultValue = defaultValue;
		}

		public void put(int channel, long tick, Integer value) {
			if (map[channel] == null)
				map[channel] = new TreeMap<Long, Integer>();

			map[channel].put(tick, value);
		}

		public int get(int channel, long tick) {
			if (map[channel] == null)
				return defaultValue;

			Entry<Long, Integer> entry = map[channel].floorEntry(tick);
			if (entry == null) // No changes before this tick
				return defaultValue;

			return entry.getValue();
		}
	}
}

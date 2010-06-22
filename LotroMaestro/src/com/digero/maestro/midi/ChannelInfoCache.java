package com.digero.maestro.midi;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.digero.common.midi.IMidiConstants;

public class ChannelInfoCache implements IMidiConstants {
	private static final int DEFAULT_INSTRUMENT = 0;
	private static final int DEFAULT_VOLUME = MAX_VOLUME;

	private MapByChannel instruments = new MapByChannel();
	private MapByChannel volume = new MapByChannel();

	public ChannelInfoCache(Sequence song) {
		Track[] tracks = song.getTracks();
		for (int i = 0; i < tracks.length; i++) {
			Track track = tracks[i];

			for (int j = 0, sz = track.size(); j < sz; j++) {
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();

				if (msg instanceof ShortMessage) {
					ShortMessage m = (ShortMessage) msg;
					int cmd = m.getCommand();
					int ch = m.getChannel();

					if (cmd == ShortMessage.PROGRAM_CHANGE) {
						if (ch != DRUM_CHANNEL) {
							instruments.put(ch, evt.getTick(), m.getData1());
						}
					}
					else if (cmd == ShortMessage.CONTROL_CHANGE) {
						if (m.getData1() == COARSE_CHANNEL_VOLUME_CONTROLLER) {
							volume.put(ch, evt.getTick(), m.getData2());
						}
					}
				}
			}
		}
	}

	public int getInstrument(int channel, long tick) {
		return instruments.get(channel, tick, DEFAULT_INSTRUMENT);
	}

	public int getVolume(int channel, long tick) {
		return volume.get(channel, tick, DEFAULT_VOLUME);
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

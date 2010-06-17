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

public class InstrumentChangeCache implements IMidiConstants {
	private NavigableMap<Long, Integer>[] mapByChannel;

	@SuppressWarnings("unchecked")
	public InstrumentChangeCache(Sequence song) {
		Track[] tracks = song.getTracks();
		mapByChannel = new NavigableMap[16];

		for (int i = 0; i < tracks.length; i++) {
			Track track = tracks[i];

			for (int j = 0, sz = track.size(); j < sz; j++) {
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();

				if (msg instanceof ShortMessage) {
					ShortMessage m = (ShortMessage) msg;
					int c = m.getChannel();

					if (m.getCommand() == ShortMessage.PROGRAM_CHANGE && c != DRUM_CHANNEL) {
						if (mapByChannel[c] == null)
							mapByChannel[c] = new TreeMap<Long, Integer>();

						mapByChannel[c].put(evt.getTick(), m.getData1());
					}
				}
			}
		}
	}

	public int getInstrument(long tick, int channel) {
		if (mapByChannel[channel] == null)
			return 0;

		Entry<Long, Integer> entry = mapByChannel[channel].floorEntry(tick);
		if (entry == null)
			return 0; // No instrument changes <= this tick. Use default (Piano)

		return entry.getValue();
	}
}

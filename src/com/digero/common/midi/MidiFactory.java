package com.digero.common.midi;

import java.io.UnsupportedEncodingException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

/**
 * Provides static methods to create MidiEvents.
 */
public class MidiFactory implements IMidiConstants {
	/**
	 * @param mpqn Microseconds per quarter note
	 */
	public static MidiEvent createTempoEvent(int mpqn, long ticks) {
		try {
			byte[] data = new byte[3];
			data[0] = (byte) ((mpqn >>> 16) & 0xFF);
			data[1] = (byte) ((mpqn >>> 8) & 0xFF);
			data[2] = (byte) (mpqn & 0xFF);

			MetaMessage msg = new MetaMessage();
			msg.setMessage(META_TEMPO, data, data.length);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e) {
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createTrackNameEvent(String name) {
		try {
			byte[] data = name.getBytes("US-ASCII");
			MetaMessage msg = new MetaMessage();
			msg.setMessage(META_TRACK_NAME, data, data.length);
			return new MidiEvent(msg, 0);
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		catch (InvalidMidiDataException e) {
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createProgramChangeEvent(int patch, int channel, long ticks) {
		try {
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.PROGRAM_CHANGE, channel, patch, 0);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e) {
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createNoteOnEvent(int id, int channel, long ticks) {
		return createNoteOnEventEx(id, channel, 112, ticks);
	}

	public static MidiEvent createNoteOnEventEx(int id, int channel, int velocity, long ticks) {
		try {
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.NOTE_ON, channel, id, velocity);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e) {
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createNoteOffEvent(int id, int channel, long ticks) {
		return createNoteOffEventEx(id, channel, 112, ticks);
	}

	public static MidiEvent createNoteOffEventEx(int id, int channel, int velocity, long ticks) {
		try {
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.NOTE_OFF, channel, id, velocity);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e) {
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createPanEvent(int value, int channel) {
		try {
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, PAN_CONTROL, value);
			return new MidiEvent(msg, 0);
		}
		catch (InvalidMidiDataException e) {
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createChannelVolumeEvent(int volume, int channel, long ticks) {
		try {
			if (volume < 0 || volume > Byte.MAX_VALUE)
				throw new IllegalArgumentException();

			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, CHANNEL_VOLUME_CONTROLLER_COARSE, volume);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e) {
			throw new RuntimeException(e);
		}
	}
}

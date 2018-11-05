package com.digero.common.midi;

import java.io.UnsupportedEncodingException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

/**
 * Provides static methods to create MidiEvents.
 */
public class MidiFactory implements MidiConstants
{
	/**
	 * @param mpqn Microseconds per quarter note
	 */
	public static MidiEvent createTempoEvent(int mpqn, long ticks)
	{
		try
		{
			byte[] data = new byte[3];
			data[0] = (byte) ((mpqn >>> 16) & 0xFF);
			data[1] = (byte) ((mpqn >>> 8) & 0xFF);
			data[2] = (byte) (mpqn & 0xFF);

			MetaMessage msg = new MetaMessage();
			msg.setMessage(META_TEMPO, data, data.length);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createTrackNameEvent(String name)
	{
		try
		{
			byte[] data = name.getBytes("US-ASCII");
			MetaMessage msg = new MetaMessage();
			msg.setMessage(META_TRACK_NAME, data, data.length);
			return new MidiEvent(msg, 0);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
		catch (InvalidMidiDataException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createProgramChangeEvent(int patch, int channel, long ticks)
	{
		try
		{
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.PROGRAM_CHANGE, channel, patch, 0);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static void modifyProgramChangeMessage(ShortMessage msg, int patch)
	{
		try
		{
			msg.setMessage(ShortMessage.PROGRAM_CHANGE, msg.getChannel(), patch, 0);
		}
		catch (InvalidMidiDataException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createNoteOnEvent(int id, int channel, long ticks)
	{
		return createNoteOnEventEx(id, channel, 112, ticks);
	}

	public static MidiEvent createNoteOnEventEx(int id, int channel, int velocity, long ticks)
	{
		try
		{
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.NOTE_ON, channel, id, velocity);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createNoteOffEvent(int id, int channel, long ticks)
	{
		return createNoteOffEventEx(id, channel, 112, ticks);
	}

	public static MidiEvent createNoteOffEventEx(int id, int channel, int velocity, long ticks)
	{
		try
		{
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.NOTE_OFF, channel, id, velocity);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createPanEvent(int value, int channel)
	{
		return createPanEvent(value, channel, 0);
	}

	public static MidiEvent createPanEvent(int value, int channel, long ticks)
	{
		return createControllerEvent(PAN_CONTROL, value, channel, ticks);
	}

	public static MidiEvent createReverbControlEvent(int value, int channel, long ticks)
	{
		return createControllerEvent(REVERB_CONTROL, value, channel, ticks);
	}

	public static MidiEvent createChorusControlEvent(int value, int channel, long ticks)
	{
		return createControllerEvent(CHORUS_CONTROL, value, channel, ticks);
	}

	public static MidiEvent createChannelVolumeEvent(int volume, int channel, long ticks)
	{
		if (volume < 0 || volume > Byte.MAX_VALUE)
			throw new IllegalArgumentException();

		return createControllerEvent(CHANNEL_VOLUME_CONTROLLER_COARSE, volume, channel, ticks);
	}

	public static MidiEvent createControllerEvent(byte controller, int value, int channel, long ticks)
	{
		try
		{
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value);
			return new MidiEvent(msg, ticks);
		}
		catch (InvalidMidiDataException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static MidiEvent createTimeSignatureEvent(TimeSignature meter, long ticks)
	{
		return new MidiEvent(meter.toMidiMessage(), ticks);
	}

	public static boolean isSupportedMidiKeyMode(KeyMode mode)
	{
		return mode == KeyMode.MAJOR || mode == KeyMode.MINOR;
	}

	public static MidiEvent createKeySignatureEvent(KeySignature key, long ticks)
	{
		return new MidiEvent(key.toMidiMessage(), ticks);
	}
}

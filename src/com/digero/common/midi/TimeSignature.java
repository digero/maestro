package com.digero.common.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;

/**
 * Representation of a MIDI time signature.
 */
public class TimeSignature implements MidiConstants
{
	public static final int MAX_DENOMINATOR = 8;
	public static final TimeSignature FOUR_FOUR = new TimeSignature(4, 4);

	public final int numerator;
	public final int denominator;
	private final byte metronome;
	private final byte thirtySecondNotes;

	/**
	 * Constructs a TimeSignature from a numerator and denominator.
	 * 
	 * @param numerator The numerator, must be less than 256.
	 * @param denominator The denominator, must be a power of 2.
	 * @throws IllegalArgumentException If the numerator is not less than 256, or the denominator is
	 *             not a power of 2.
	 */
	public TimeSignature(int numerator, int denominator)
	{
		verifyData(numerator, denominator);

		this.numerator = numerator;
		this.denominator = denominator;
		this.metronome = 24;
		this.thirtySecondNotes = 8;
	}

	public TimeSignature(MetaMessage midiMessage)
	{
		byte[] data = midiMessage.getData();
		if (midiMessage.getType() != META_TIME_SIGNATURE || data.length < 4)
		{
			throw new IllegalArgumentException("Midi message is not a time signature event");
		}

		if ((1 << data[1]) > MAX_DENOMINATOR)
		{
			this.numerator = 4;
			this.denominator = 4;
			this.metronome = 24;
			this.thirtySecondNotes = 8;
		}
		else
		{
			this.numerator = data[0];
			this.denominator = 1 << data[1];
			this.metronome = data[2];
			this.thirtySecondNotes = data[3];
		}
	}

	public TimeSignature(String str)
	{
		str = str.trim();
		if (str.equals("C"))
		{
			this.numerator = 4;
			this.denominator = 4;
		}
		else if (str.equals("C|"))
		{
			this.numerator = 2;
			this.denominator = 2;
		}
		else
		{
			String[] parts = str.split("[/:| ]");
			if (parts.length != 2)
			{
				throw new IllegalArgumentException("The string: \"" + str
						+ "\" is not a valid time signature (expected format: 4/4)");
			}
			this.numerator = Integer.parseInt(parts[0]);
			this.denominator = Integer.parseInt(parts[1]);
		}
		verifyData(this.numerator, this.denominator);
		this.metronome = 24;
		this.thirtySecondNotes = 8;
	}

	/**
	 * A best-guess as to whether this time signature represents compound meter.
	 */
	public boolean isCompound()
	{
		return (numerator % 3) == 0;
	}

	private static void verifyData(int numerator, int denominator)
	{
		if (denominator == 0 || denominator != (1 << floorLog2(denominator)))
		{
			throw new IllegalArgumentException("The denominator of the time signature must be a power of 2");
		}
		if (denominator > MAX_DENOMINATOR)
		{
			throw new IllegalArgumentException("The denominator must be less than or equal to " + MAX_DENOMINATOR);
		}
		if (numerator > 255)
		{
			throw new IllegalArgumentException("The numerator of the time signature must be less than 256");
		}
	}

	public MetaMessage toMidiMessage()
	{
		MetaMessage midiMessage = new MetaMessage();
		byte[] data = new byte[4];
		data[0] = (byte) numerator;
		data[1] = floorLog2(denominator);
		data[2] = metronome;
		data[3] = thirtySecondNotes;

		try
		{
			midiMessage.setMessage(META_TIME_SIGNATURE, data, data.length);
		}
		catch (InvalidMidiDataException e)
		{
			throw new RuntimeException(e);
		}
		return midiMessage;
	}

	@Override public String toString()
	{
		return numerator + "/" + denominator;
	}

	@Override public int hashCode()
	{
		return (denominator << 24) ^ (numerator << 16) ^ (((int) metronome) << 8) ^ thirtySecondNotes;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj instanceof TimeSignature)
		{
			TimeSignature that = (TimeSignature) obj;
			return this.numerator == that.numerator && this.denominator == that.denominator
					&& this.metronome == that.metronome && this.thirtySecondNotes == that.thirtySecondNotes;
		}
		return false;
	}

	/**
	 * @return The floor of the binary logarithm for a 32 bit integer. -1 is returned if n is 0.
	 */
	private static byte floorLog2(int n)
	{
		byte pos = 0; // Position of the most significant bit
		if (n >= (1 << 16))
		{
			n >>>= 16;
			pos += 16;
		}
		if (n >= (1 << 8))
		{
			n >>>= 8;
			pos += 8;
		}
		if (n >= (1 << 4))
		{
			n >>>= 4;
			pos += 4;
		}
		if (n >= (1 << 2))
		{
			n >>>= 2;
			pos += 2;
		}
		if (n >= (1 << 1))
		{
			pos += 1;
		}
		return ((n == 0) ? (-1) : pos);
	}

}

package com.digero.common.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;

import com.digero.common.abc.Accidental;

/**
 * Representation of a MIDI key signature.
 */
public class KeySignature implements MidiConstants
{
	public static final KeySignature C_MAJOR = new KeySignature(0, true);

	public final byte sharpsFlats;
	public final KeyMode mode;

	public KeySignature(int sharpsFlats, boolean major)
	{
		if (sharpsFlats < -7 || sharpsFlats > 7)
			throw new IllegalArgumentException("Key signatures can't have more than 7 sharps or flats");

		this.sharpsFlats = (byte) sharpsFlats;
		this.mode = major ? KeyMode.MAJOR : KeyMode.MINOR;
	}

	public KeySignature(int sharpsFlats, KeyMode mode)
	{
		if (sharpsFlats < -7 || sharpsFlats > 7)
			throw new IllegalArgumentException("Key signatures can't have more than 7 sharps or flats");

		this.sharpsFlats = (byte) sharpsFlats;
		this.mode = mode;
	}

	public KeySignature(MetaMessage midiMessage)
	{
		byte[] data = midiMessage.getData();
		if (midiMessage.getType() != META_KEY_SIGNATURE || data.length < 2)
		{
			throw new IllegalArgumentException("Midi message is not a key signature");
		}

		this.sharpsFlats = data[0];
		this.mode = (data[1] == 1) ? KeyMode.MINOR : KeyMode.MAJOR;
	}

	public MetaMessage toMidiMessage()
	{
		try
		{
			MetaMessage midiMessage = new MetaMessage();
			byte[] data = new byte[2];
			data[0] = sharpsFlats;
			data[1] = (byte) (mode == KeyMode.MINOR ? 1 : 0);
			midiMessage.setMessage(META_KEY_SIGNATURE, data, data.length);
			return midiMessage;
		}
		catch (InvalidMidiDataException e)
		{
			throw new RuntimeException(e);
		}
	}

	public KeySignature(String str)
	{
		if (str.length() == 0)
			throw new IllegalArgumentException("Invalid key signature: " + str);

		String keyPart;
		if (str.length() == 1)
		{
			keyPart = str;
		}
		else if (str.charAt(1) == 'b' || str.charAt(1) == '#' || str.charAt(1) == 's')
		{
			keyPart = str.substring(0, 2).replace('s', '#');
		}
		else
		{
			keyPart = str.substring(0, 1);
		}

		String suffix = str.substring(keyPart.length()).trim();
		if (suffix.length() > 3)
			suffix = suffix.substring(0, 3);

		this.mode = KeyMode.parseMode(suffix);
		if (this.mode == null)
			throw new IllegalArgumentException("Invalid key signature: " + str);

		String[] keys = modeToKeys(this.mode);
		for (int i = 0; i < keys.length; i++)
		{
			if (keys[i].equalsIgnoreCase(keyPart))
			{
				this.sharpsFlats = (byte) (i - 7);
				return;
			}
		}
		throw new IllegalArgumentException("Invalid key signature: " + str);
	}

	public KeySignature transpose(int semitones)
	{
		if (semitones % 12 == 0)
			return this;

		int x = (semitones * -5) % 12;

		if (x > 6)
		{
			x -= 12;
		}
		else if (x < -6)
		{
			x += 12;
		}

		return new KeySignature(x, this.mode);
	}

	/**
	 * Computes the default accidental in this key for the given "white-key" note (aka natural note
	 * ID in the key of C).
	 */
	public Accidental getDefaultAccidental(int naturalId)
	{
		int id = (naturalId - Note.CX.id) % 12;

		for (int sharp = 0; sharp < sharpsFlats; sharp++)
		{
			if (SHARPS[sharp] == id)
				return Accidental.SHARP;
		}

		for (int flat = 0; flat < -sharpsFlats; flat++)
		{
			if (FLATS[flat] == id)
				return Accidental.FLAT;
		}

		return Accidental.NONE;
	}

	/**
	 * What accidental should be written for the given note in this key.
	 */
	public Accidental getOutputAccidental(Note note)
	{
		note = note.getEnharmonicNote(sharpsFlats >= 0);
		Accidental acc = Accidental.fromDeltaId(note.id - note.naturalId);
		if (acc.deltaNoteId == getDefaultAccidental(note.naturalId).deltaNoteId)
			return Accidental.NONE;
		return acc;
	}

	public Accidental getOutputAccidental(int noteId)
	{
		return getOutputAccidental(Note.fromId(noteId));
	}

	@Override public String toString()
	{
		return modeToKeys(mode)[sharpsFlats + 7] + " " + mode.toShortString().toLowerCase();
	}

	@Override public boolean equals(Object obj)
	{
		if (obj instanceof KeySignature)
		{
			KeySignature that = (KeySignature) obj;
			return this.mode == that.mode && this.sharpsFlats == that.sharpsFlats;
		}
		return false;
	}

	@Override public int hashCode()
	{
		return ((int) sharpsFlats) ^ (mode.ordinal() << 4);
	}

	private static final String[] MAJOR_KEYS = new String[] { "Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", //
		"C", //
		"G", "D", "A", "E", "B", "F#", "C#" };

	private static final String[] MINOR_KEYS = new String[] { "Ab", "Eb", "Bb", "F", "C", "G", "D", //
		"A", //
		"E", "B", "F#", "C#", "G#", "D#", "A#" };

	private static final String[] DORIAN_KEYS = new String[] { "Db", "Ab", "Eb", "Bb", "F", "C", "G", //
		"D", //
		"A", "E", "B", "F#", "C#", "G#", "D#" };

	private static final String[] PHRYGIAN_KEYS = new String[] { "Eb", "Bb", "F", "C", "G", "D", "A", //
		"E", //
		"B", "F#", "C#", "G#", "D#", "A#", "E#" };

	private static final String[] LYDIAN_KEYS = new String[] { "Fb", "Cb", "Gb", "Db", "Ab", "Eb", "Bb", //
		"F", //
		"C", "G", "D", "A", "E", "B", "F#" };

	private static final String[] MIXOLYDIAN_KEYS = new String[] { "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", //
		"G", //
		"D", "A", "E", "B", "F#", "C#", "G#" };

	private static final String[] LOCRIAN_KEYS = new String[] { "Bb", "F", "C", "G", "D", "A", "E", //
		"B", //
		"F#", "C#", "G#", "D#", "A#", "E#", "B#" };

	private static final int[] SHARPS = new int[] { Note.FX.id, Note.CX.id, Note.GX.id, Note.DX.id, Note.AX.id,
		Note.EX.id, Note.BX.id };

	private static final int[] FLATS = new int[] { Note.BX.id, Note.EX.id, Note.AX.id, Note.DX.id, Note.GX.id,
		Note.CX.id, Note.FX.id };

	private static final String[] modeToKeys(KeyMode mode)
	{
		switch (mode)
		{
		case MAJOR:
		case IONIAN:
			return MAJOR_KEYS;
		case MINOR:
		case AEOLIAN:
			return MINOR_KEYS;
		case DORIAN:
			return DORIAN_KEYS;
		case PHRYGIAN:
			return PHRYGIAN_KEYS;
		case LYDIAN:
			return LYDIAN_KEYS;
		case MIXOLYDIAN:
			return MIXOLYDIAN_KEYS;
		case LOCRIAN:
			return LOCRIAN_KEYS;
		default:
			return null;
		}
	}

	public static void main(String[] args)
	{
		int[] whiteKeys = { Note.C3.id, Note.D3.id, Note.E3.id, Note.F3.id, Note.G3.id, Note.A3.id, Note.B3.id };

		Note root = Note.GbX;

		for (int i = -6; i <= 6; i++)
		{
			KeySignature key = new KeySignature(i, true);
			System.out.print(key + ":\t");
			for (int id : whiteKeys)
			{
				id += root.id - Note.CX.id;
				Note note = Note.fromId(id).getEnharmonicNote(!root.isFlat());
				//Note natural = Note.fromId(note.naturalId);
				Accidental acc = key.getOutputAccidental(note);
				System.out.print(acc.toString() + note.toString() + "\t");
			}
			System.out.println();
		}
	}

	public static void mainOld(String[] args)
	{
		System.out.println("Sharps");
		for (int id : SHARPS)
		{
			System.out.print(id + ", ");
		}
		System.out.println();
		for (int i = 0; i <= 7; i++)
		{
			int id = (i * 7 /*- 1*/) % 12;
			if (id < 0)
				id += 12;
			Note note = Note.fromId(id);
			String n = note.toString().replace("X", "").replace('s', '#');
			System.out.print(n + ", ");
		}

		System.out.println();
		System.out.println();
		System.out.println("Flats");
		for (int id : FLATS)
		{
			System.out.print(id + ", ");
		}
		System.out.println();
		for (int i = -1; i >= -7; i--)
		{
			int id = (i * 7 /* + 5 */) % 12;
			if (id < 0)
				id += 12;
			Note note = Note.fromId(id);
			String n = note.toString().replace("X", "").replace('s', '#');
			System.out.print(n + ", ");
		}
	}
}

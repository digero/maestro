package com.digero.maestro.midi;

import javax.sound.midi.MetaMessage;

/**
 * Representation of a MIDI key signature.
 */
public class KeySignature implements IMidiConstants {
	public static final KeySignature C_MAJOR = new KeySignature(0, true);

	public enum Accidental {
		NONE, FLAT, NATURAL, SHARP
	};

	public final byte sharpsFlats;
	public final boolean major;

	public KeySignature(int sharpsFlats, boolean major) {
		if (sharpsFlats < -7 || sharpsFlats > 7)
			throw new IllegalArgumentException("Key signatures can't have more than 7 sharps or flats");

		this.sharpsFlats = (byte) sharpsFlats;
		this.major = major;
	}

	public KeySignature(MetaMessage midiMessage) {
		byte[] data = midiMessage.getData();
		if (midiMessage.getType() != META_KEY_SIGNATURE || data.length < 2) {
			throw new IllegalArgumentException("Midi message is not a key signature");
		}

		this.sharpsFlats = data[0];
		this.major = (data[1] == 0);
	}

	public KeySignature(String str) {
		if (str.length() == 0)
			throw new IllegalArgumentException("Invalid key signature: " + str);

		String keyPart;
		if (str.length() == 1) {
			keyPart = str;
		}
		else if (str.charAt(1) == 'b' || str.charAt(1) == '#' || str.charAt(1) == 's') {
			keyPart = str.substring(0, 2).replace('s', '#');
		}
		else {
			keyPart = str.substring(0, 1);
		}

		String suffix = str.substring(keyPart.length()).trim();
		if (suffix.length() > 3)
			suffix = suffix.substring(0, 3);

		final int MAJOR = 0x01;
		final int MINOR = 0x02;
		final int BOTH = MAJOR | MINOR;
		int keys = 0;

		if (suffix.length() == 0) {
			keys = BOTH; // Try to find a major key match, then a minor key
		}
		else if (suffix.equals("M") || suffix.equalsIgnoreCase("maj")) {
			keys = MAJOR;
		}
		else if (suffix.equals("m") || suffix.equalsIgnoreCase("min")) {
			keys = MINOR;
		}

		if ((keys & MAJOR) != 0) {
			for (int i = 0; i < MAJOR_KEYS.length; i++) {
				if (MAJOR_KEYS[i].equalsIgnoreCase(keyPart)) {
					this.sharpsFlats = (byte) (i - 7);
					this.major = true;
					return;
				}
			}
		}

		if ((keys & MINOR) != 0) {
			for (int i = 0; i < MINOR_KEYS.length; i++) {
				if (MINOR_KEYS[i].equalsIgnoreCase(keyPart)) {
					this.sharpsFlats = (byte) (i - 7);
					this.major = false;
					return;
				}
			}
		}

		throw new IllegalArgumentException("Invalid key signature: " + str);
	}

	public KeySignature transpose(int semitones) {
		if (semitones % 12 == 0)
			return this;

		int x = (semitones * -5) % 12;

		if (x > 6) {
			x -= 12;
		}
		else if (x < -6) {
			x += 12;
		}

		return new KeySignature(x, this.major);
	}

	public Accidental getAccidental(Note note) {
		int id = (note.id - Note.CX.id) % 12;

		for (int sharp = 0; sharp < sharpsFlats; sharp++) {
			if (SHARPS[sharp] == note.naturalId)
				return note.isAccented ? null : Accidental.SHARP;
		}

		for (int flat = 0; flat < -sharpsFlats; flat++) {
			if (FLATS[flat] == note.naturalId)
				return note.isAccented ? null : Accidental.FLAT;
		}

		return note.isAccented ? null : Accidental.NATURAL;
	}

	@Override
	public String toString() {
		if (major)
			return MAJOR_KEYS[sharpsFlats + 7];
		else
			return MINOR_KEYS[sharpsFlats + 7] + " min";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof KeySignature) {
			KeySignature that = (KeySignature) obj;
			return this.major == that.major && this.sharpsFlats == that.sharpsFlats;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ((int) sharpsFlats) ^ (major ? 0x100 : 0x200);
	}

	private static final String[] MAJOR_KEYS = new String[] {
			"Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", //
			"C", //
			"G", "D", "A", "E", "B", "F#", "C#"
	};

	private static final String[] MINOR_KEYS = new String[] {
			"Ab", "Eb", "Bb", "F", "C", "G", "D", //
			"A", //
			"E", "B", "F#", "C#", "G#", "D#", "A#"
	};

	private static final int[] SHARPS = new int[] {
			Note.FX.id, Note.CX.id, Note.GX.id, Note.DX.id, Note.AX.id, Note.EX.id, Note.BX.id
	};

	private static final int[] FLATS = new int[] {
			Note.BX.id, Note.EX.id, Note.AX.id, Note.DX.id, Note.GX.id, Note.CX.id, Note.FX.id
	};
}

package com.digero.maestro.midi;

import javax.sound.midi.MetaMessage;

/**
 * Representation of a MIDI key signature.
 */
public class KeySignature implements MidiConstants {
	public static final KeySignature C_MAJOR = new KeySignature(0, true);

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

		String[] keys;
		String suffix = str.substring(keyPart.length()).trim();
		if (suffix.length() > 3)
			suffix = suffix.substring(0, 3);

		if (suffix.equals("M") || suffix.equalsIgnoreCase("MAJ") || suffix.length() == 0) {
			keys = MAJOR_KEYS;
		}
		else if (suffix.equals("m") || suffix.equalsIgnoreCase("MIN")) {
			keys = MINOR_KEYS;
		}
		else {
			throw new IllegalArgumentException("Invalid key signature: " + str);
		}

		for (int i = 0; i < keys.length; i++) {
			if (keys[i].equalsIgnoreCase(keyPart)) {
				this.sharpsFlats = (byte) (i - 7);
				this.major = (keys == MAJOR_KEYS);
				return;
			}
		}

		throw new IllegalArgumentException("Invalid key signature: " + str);
	}

	@Override
	public String toString() {
		if (major)
			return MAJOR_KEYS[sharpsFlats + 7] + " maj";
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
}

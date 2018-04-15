package com.digero.common.abc;

import com.digero.common.midi.Note;

public interface AbcConstants
{
	// Chord
	public static final int MAX_CHORD_NOTES = 6;

	// TimingInfo
	public static final int ONE_SECOND_MICROS = 1000000;
	public static final int ONE_MINUTE_MICROS = 60 * ONE_SECOND_MICROS;
	public static final int SHORTEST_NOTE_MICROS = ONE_MINUTE_MICROS / 1000;
	public static final int LONGEST_NOTE_MICROS = 8 * ONE_SECOND_MICROS;
	public static final int LONGEST_NOTE_MICROS_WORST_CASE = (2 * SHORTEST_NOTE_MICROS - 1)
			* (LONGEST_NOTE_MICROS / (2 * SHORTEST_NOTE_MICROS - 1));
	public static final int MAX_TEMPO = ONE_MINUTE_MICROS / SHORTEST_NOTE_MICROS;
	public static final int MIN_TEMPO = (ONE_MINUTE_MICROS + LONGEST_NOTE_MICROS / 2) / LONGEST_NOTE_MICROS; // Round up

	/** Note ID used in ABC files for Cowbells. Somewhat arbitrary */
	public static final int COWBELL_NOTE_ID = 71;

	/** The highest Note ID for bagpipe drones */
	public static final int BAGPIPE_LAST_DRONE_NOTE_ID = Note.B2.id;

	/** The highest Note ID for the student fiddle "flub" notes */
	public static final int STUDENT_FIDDLE_LAST_FLUB_NOTE_ID = Note.Fs2.id;
}

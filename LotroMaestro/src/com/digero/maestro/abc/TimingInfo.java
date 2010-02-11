package com.digero.maestro.abc;

import com.digero.maestro.midi.TimeSignature;

public class TimingInfo {
	public final int tempo;
	public final TimeSignature meter;

	public final int defaultNoteLength;
	public final int defaultDivisor;
	public final int minNoteLength;
	public final int shortestDivisor;
	public final int maxNoteLength;
	public final int barLength;

	public TimingInfo(int tempo, TimeSignature meter) throws AbcConversionException {
		if (tempo > AbcPart.MAX_TEMPO || tempo < AbcPart.MIN_TEMPO) {
			throw new AbcConversionException("Tempo " + tempo + " is out of range. Must be between "
					+ AbcPart.MIN_TEMPO + " and " + AbcPart.MAX_TEMPO + ".");
		}

		this.tempo = tempo;
		this.meter = meter;

		// From http://abcnotation.com/abc2mtex/abc.txt:
		// The default note length can be calculated by computing the meter as
		// a decimal; if it is less than 0.75 the default is a sixteenth note,
		// otherwise it is an eighth note. For example, 2/4 = 0.5, so the
		// default note length is a sixteenth note, while 4/4 = 1.0 or
		// 6/8 = 0.75, so the default is an eighth note.
		defaultNoteLength = AbcPart.ONE_MINUTE_MICROS / tempo;
		defaultDivisor = ((double) meter.numerator / meter.denominator < 0.75) ? 16 : 8;
		int minNoteLength = defaultNoteLength;
		int shortestDivisor = 1;
		while (minNoteLength >= AbcPart.SHORTEST_NOTE_MICROS * 2) {
			minNoteLength /= 2;
			shortestDivisor *= 2;
		}
		this.minNoteLength = minNoteLength;
		this.shortestDivisor = shortestDivisor;

		maxNoteLength = defaultNoteLength * (AbcPart.LONGEST_NOTE_MICROS / defaultNoteLength);
//		barLength = defaultDivisor * defaultNoteLength * meter.numerator / meter.denominator;
		barLength = Integer.MAX_VALUE;
	}

	public int getBPM() {
		return tempo * 4 / defaultDivisor;
	}

	public int getMPQN() {
		return 60000000 / getBPM();
	}

	public int getMidiResolution() {
		return shortestDivisor * defaultDivisor / 4;
	}

	public long getMidiTicks(long micros) {
		return (long) ((double) micros * getMidiResolution() / getMPQN());
	}
}
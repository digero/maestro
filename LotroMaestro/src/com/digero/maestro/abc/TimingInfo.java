package com.digero.maestro.abc;

import com.digero.maestro.midi.TimeSignature;

public class TimingInfo {
	public static final int ONE_SECOND_MICROS = 1000000;
	public static final int ONE_MINUTE_MICROS = 60 * ONE_SECOND_MICROS;
	public static final int SHORTEST_NOTE_MICROS = ONE_SECOND_MICROS / 16;
	public static final int LONGEST_NOTE_MICROS = 8 * ONE_SECOND_MICROS;
	public static final int LONGEST_NOTE_MICROS_WORST_CASE = (2 * SHORTEST_NOTE_MICROS - 1)
			* (LONGEST_NOTE_MICROS / (2 * SHORTEST_NOTE_MICROS - 1));
	public static final int MAX_TEMPO = ONE_MINUTE_MICROS / SHORTEST_NOTE_MICROS;
	public static final int MIN_TEMPO = (ONE_MINUTE_MICROS + LONGEST_NOTE_MICROS / 2) / LONGEST_NOTE_MICROS; // Round up

	public final int tempo;
	public final TimeSignature meter;

	public final int defaultDivisor;
	public final int minNoteLength;
	public final int minNoteDivisor;
	public final int maxNoteLength;
	public final int barLength;

	public TimingInfo(int tempo, TimeSignature meter) throws AbcConversionException {
		if (tempo > MAX_TEMPO || tempo < MIN_TEMPO) {
			throw new AbcConversionException("Tempo " + tempo + " is out of range. Must be between " + MIN_TEMPO
					+ " and " + MAX_TEMPO + ".");
		}

		this.tempo = tempo;
		this.meter = meter;

		// From http://abcnotation.com/abc2mtex/abc.txt:
		// The default note length can be calculated by computing the meter as
		// a decimal; if it is less than 0.75 the default is a sixteenth note,
		// otherwise it is an eighth note. For example, 2/4 = 0.5, so the
		// default note length is a sixteenth note, while 4/4 = 1.0 or
		// 6/8 = 0.75, so the default is an eighth note.
		this.defaultDivisor = (((double) meter.numerator / meter.denominator < 0.75) ? 16 : 8) * 4 / meter.denominator;

		{
			int minNoteLength = (ONE_MINUTE_MICROS / tempo) / (defaultDivisor / 4);
			int minNoteDivisor = defaultDivisor;
			while (minNoteLength < SHORTEST_NOTE_MICROS) {
				minNoteLength *= 2;
				minNoteDivisor /= 2;
			}

			assert minNoteDivisor > 0;

			while (minNoteLength >= SHORTEST_NOTE_MICROS * 2) {
				minNoteLength /= 2;
				minNoteDivisor *= 2;
			}

			if (meter.denominator > minNoteDivisor) {
				throw new AbcConversionException("The denominator of the meter must be no greater than "
						+ minNoteDivisor);
			}

			this.minNoteLength = minNoteLength;
			this.minNoteDivisor = minNoteDivisor;
		}

		this.maxNoteLength = this.minNoteLength * (LONGEST_NOTE_MICROS / this.minNoteLength);
		this.barLength = this.minNoteDivisor * this.minNoteLength * meter.numerator / meter.denominator;

		assert barLength % this.minNoteLength == 0 : barLength + " % " + this.minNoteLength + " != 0";
//		barLength = Integer.MAX_VALUE;
	}

	public int getBPM() {
		return tempo / (defaultDivisor / 4);
	}

	public int getMPQN() {
		return 60000000 / getBPM();
	}

	public int getMidiResolution() {
		return minNoteDivisor * 4;
	}

	public long getMidiTicks(long micros) {
		return (long) ((double) micros * getMidiResolution() / getMPQN());
	}

	public long getMicros(long midiTicks) {
		return (long) ((double) midiTicks * getMPQN() / getMidiResolution());
	}

	public long getBarStart(long micros) {
		return (micros / barLength) * barLength;
	}

	public long getBarEnd(long micros) {
		return (micros / barLength + 1) * barLength;
	}
}
package com.digero.maestro.abc;

import com.digero.common.midi.TimeSignature;

public class TimingInfo
{
	public static final int ONE_SECOND_MICROS = 1000000;
	public static final int ONE_MINUTE_MICROS = 60 * ONE_SECOND_MICROS;
	public static final int SHORTEST_NOTE_MICROS = ONE_MINUTE_MICROS / 1000;
	public static final int LONGEST_NOTE_MICROS = 6 * ONE_SECOND_MICROS;
	public static final int LONGEST_NOTE_MICROS_WORST_CASE = (2 * SHORTEST_NOTE_MICROS - 1)
			* (LONGEST_NOTE_MICROS / (2 * SHORTEST_NOTE_MICROS - 1));
	public static final int MAX_TEMPO = ONE_MINUTE_MICROS / SHORTEST_NOTE_MICROS;
	public static final int MIN_TEMPO = (ONE_MINUTE_MICROS + LONGEST_NOTE_MICROS / 2) / LONGEST_NOTE_MICROS; // Round up

	public final int tempo;
	public final int exportTempo;
	public final TimeSignature meter;

	public final int defaultDivisor;
	public final int minNoteLength;
	public final int minNoteDivisor;
	public final int maxNoteLength;
	public final int barLength;

	public TimingInfo(int sourceTempo, int exportTempo, TimeSignature meter, boolean useTripletTiming)
			throws AbcConversionException
	{
		this.tempo = sourceTempo;
		this.exportTempo = exportTempo;
		this.meter = meter;

		if (tempo > MAX_TEMPO || tempo < MIN_TEMPO)
		{
			throw new AbcConversionException("Tempo " + tempo + " is out of range. Must be between " + MIN_TEMPO
					+ " and " + MAX_TEMPO + ".");
		}

		// From http://abcnotation.com/abc2mtex/abc.txt:
		// The default note length can be calculated by computing the meter as
		// a decimal; if it is less than 0.75 the default is a sixteenth note,
		// otherwise it is an eighth note. For example, 2/4 = 0.5, so the
		// default note length is a sixteenth note, while 4/4 = 1.0 or
		// 6/8 = 0.75, so the default is an eighth note.
		this.defaultDivisor = (((double) meter.numerator / meter.denominator < 0.75) ? 16 : 8) * 4 / meter.denominator;

		// Calculate min note length
		{
			int minNoteDivisor = defaultDivisor;
			if (useTripletTiming)
				minNoteDivisor *= 3;
			int minNoteLength = (ONE_MINUTE_MICROS / tempo) / (minNoteDivisor / 4);
			int minNoteLengthAtExportTempo = (ONE_MINUTE_MICROS / exportTempo) / (minNoteDivisor / 4);
			while (minNoteLengthAtExportTempo < SHORTEST_NOTE_MICROS)
			{
				minNoteLengthAtExportTempo *= 2;
				minNoteLength *= 2;
				minNoteDivisor /= 2;
			}

			assert minNoteDivisor > 0;

			while (minNoteLengthAtExportTempo >= SHORTEST_NOTE_MICROS * 2)
			{
				minNoteLengthAtExportTempo /= 2;
				minNoteLength /= 2;
				minNoteDivisor *= 2;
			}

			if (meter.denominator > minNoteDivisor)
			{
				throw new AbcConversionException("The denominator of the meter must be no greater than "
						+ minNoteDivisor);
			}

			this.minNoteLength = minNoteLength;
			this.minNoteDivisor = minNoteDivisor;
			this.maxNoteLength = this.minNoteLength * (LONGEST_NOTE_MICROS / minNoteLengthAtExportTempo);
		}

		this.barLength = this.minNoteDivisor * this.minNoteLength * meter.numerator / meter.denominator;

		assert barLength % this.minNoteLength == 0 : barLength + " % " + this.minNoteLength + " != 0";
//		barLength = Integer.MAX_VALUE;
	}

	public int getBPM()
	{
		return tempo / (defaultDivisor / 4);
	}

	public int getMPQN()
	{
		return ONE_MINUTE_MICROS / getBPM();
	}

	public int getMidiResolution()
	{
		return minNoteDivisor * 12;
	}

	public long getMidiTicks(long micros)
	{
		return (long) ((double) micros * getMidiResolution() / getMPQN());
	}

	public long getMicros(long midiTicks)
	{
		return (long) ((double) midiTicks * getMPQN() / getMidiResolution());
	}

	public long getBarStart(long micros)
	{
		return (micros / barLength) * barLength;
	}

	public long getBarEnd(long micros)
	{
		return (micros / barLength + 1) * barLength;
	}
}
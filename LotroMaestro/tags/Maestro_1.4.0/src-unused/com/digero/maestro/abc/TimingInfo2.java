package com.digero.maestro.abc;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.TimeSignature;
import com.sun.media.sound.MidiUtils;

public class TimingInfo2 {
	public static final int ONE_SECOND_MICROS = 1000000;
	public static final int ONE_MINUTE_MICROS = 60 * ONE_SECOND_MICROS;
	public static final int SHORTEST_NOTE_MICROS = ONE_MINUTE_MICROS / 1000;
	public static final int LONGEST_NOTE_MICROS = 8 * ONE_SECOND_MICROS;
	public static final int LONGEST_NOTE_MICROS_WORST_CASE = (2 * SHORTEST_NOTE_MICROS - 1)
			* (LONGEST_NOTE_MICROS / (2 * SHORTEST_NOTE_MICROS - 1));
	public static final int MAX_TEMPO = ONE_MINUTE_MICROS / SHORTEST_NOTE_MICROS;
	public static final int MIN_TEMPO = (ONE_MINUTE_MICROS + LONGEST_NOTE_MICROS / 2) / LONGEST_NOTE_MICROS; // Round up

	private final NavigableMap<Long, Integer> microsToTempoMap;
	private final int mainTempo;
	private final TimeSignature meter;
	private boolean tripletTiming = false;
	private NoteLengthCache noteLengthCache = new NoteLengthCache();

	public TimingInfo2(int mainTempo, TimeSignature meter, NavigableMap<Long, Integer> microsToTempoMap) {
		this.microsToTempoMap = microsToTempoMap;
		this.mainTempo = mainTempo;
		this.meter = meter;
	}

	public void setTripletTiming(boolean useTripletTiming) {
		this.tripletTiming = useTripletTiming;
	}

	public boolean isTripletTiming() {
		return tripletTiming;
	}

	public NavigableMap<Long, Integer> getMicrosToTempoMap() {
		return microsToTempoMap;
	}

	public int getMainTempo() {
		return mainTempo;
	}

	public int getTempoAt(long micros) {
		Entry<Long, Integer> entry = microsToTempoMap.floorEntry(micros);
		if (entry != null)
			return entry.getValue();

		return MidiConstants.DEFAULT_TEMPO_BPM;
	}

	public int getMidiMPQN() {
		return (int) MidiUtils.convertTempo(getMainTempo() / (getDefaultDivisor() / 4));
	}

	public int getMidiResolution() {
		return noteLengthCache.getMinNoteDivisorAtTempo(getMainTempo()) * 12;
	}

	public TimeSignature getMeter() {
		return meter;
	}

	public int getDefaultDivisor() {
		// From http://abcnotation.com/abc2mtex/abc.txt:
		// The default note length can be calculated by computing the meter as
		// a decimal; if it is less than 0.75 the default is a sixteenth note,
		// otherwise it is an eighth note. For example, 2/4 = 0.5, so the
		// default note length is a sixteenth note, while 4/4 = 1.0 or
		// 6/8 = 0.75, so the default is an eighth note.
		return (((double) meter.numerator / meter.denominator < 0.75) ? 16 : 8) * 4 / meter.denominator;
	}

	public int getMinNoteDivisorAt(long micros) {
		return noteLengthCache.getMinNoteDivisorAtTempo(getTempoAt(micros));
	}

	public int getMinNoteLengthAt(long micros) {
		return noteLengthCache.getMinNoteLengthAtTempo(getTempoAt(micros));
	}

	public int getMaxNoteLengthAt(long micros) {
		int minNoteLength = getMinNoteLengthAt(micros);
		return minNoteLength * (LONGEST_NOTE_MICROS / minNoteLength);
	}

	public int getBarLengthAt(long micros) {

		NavigableSet<Integer> barStartMicros = new TreeSet<Integer>();
		barStartMicros.add(0);
		int curTempo = getTempoAt(0);
		int nextBarStart = noteLengthCache.getMinNoteDivisorAtTempo(curTempo)
				* noteLengthCache.getMinNoteLengthAtTempo(curTempo) * meter.numerator / meter.denominator;
		for (Entry<Long, Integer> microsToTempo : microsToTempoMap.entrySet()) {
			// Fill in the bars up to this tempo change
			while (nextBarStart < microsToTempo.getKey()) {
				barStartMicros.add(nextBarStart);
				nextBarStart += noteLengthCache.getMinNoteDivisorAtTempo(curTempo)
						* noteLengthCache.getMinNoteLengthAtTempo(curTempo) * meter.numerator / meter.denominator;
			}
// TODO
			curTempo = microsToTempo.getValue();
		}

		return getMinNoteDivisorAt(micros) * getMinNoteLengthAt(micros) * getMeter().numerator / getMeter().denominator;
	}

	private class NoteLengthCache {
		private int cacheTempo = -1;
		private boolean cacheTripletTiming;
		private int minNoteLengthCached = -1;
		private int minNoteDivisorCached = -1;

		public int getMinNoteLengthAtTempo(int tempo) {
			refreshCache(tempo);
			return minNoteLengthCached;
		}

		public int getMinNoteDivisorAtTempo(int tempo) {
			refreshCache(tempo);
			return minNoteDivisorCached;
		}

		private void refreshCache(int tempo) {
			if (tempo != cacheTempo || tripletTiming != cacheTripletTiming) {
				int minNoteDivisor = getDefaultDivisor();
				if (tripletTiming)
					minNoteDivisor *= 3;
				int minNoteLength = (ONE_MINUTE_MICROS / tempo) / (minNoteDivisor / 4);
				while (minNoteLength < SHORTEST_NOTE_MICROS) {
					minNoteLength *= 2;
					minNoteDivisor /= 2;
				}

				assert minNoteDivisor > 0;

				while (minNoteLength >= SHORTEST_NOTE_MICROS * 2) {
					minNoteLength /= 2;
					minNoteDivisor *= 2;
				}

				this.minNoteLengthCached = minNoteLength;
				this.minNoteDivisorCached = minNoteDivisor;

				cacheTempo = tempo;
				cacheTripletTiming = tripletTiming;
			}
		}
	}
}

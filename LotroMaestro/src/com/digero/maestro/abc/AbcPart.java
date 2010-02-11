package com.digero.maestro.abc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.maestro.midi.Chord;
import com.digero.maestro.midi.KeySignature;
import com.digero.maestro.midi.MidiConstants;
import com.digero.maestro.midi.MidiFactory;
import com.digero.maestro.midi.Note;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequenceInfo;

public class AbcPart {
	public static final int ONE_SECOND_MICROS = 1000000;
	public static final int ONE_MINUTE_MICROS = 60 * ONE_SECOND_MICROS;
	public static final int SHORTEST_NOTE_MICROS = ONE_SECOND_MICROS / 16;
	public static final int LONGEST_NOTE_MICROS = 8 * ONE_SECOND_MICROS;
	public static final int MAX_TEMPO = ONE_MINUTE_MICROS / SHORTEST_NOTE_MICROS;
	public static final int MIN_TEMPO = (ONE_MINUTE_MICROS + LONGEST_NOTE_MICROS / 2) / LONGEST_NOTE_MICROS; // Round up

	private SequenceInfo sequenceInfo;
	private boolean enabled;
	private int partNumber;
	private String title;
	private LotroInstrument instrument;
	private int baseTranspose;
	private int[] trackTranspose;
	private boolean[] trackDisabled;
	private final List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

	public AbcPart(SequenceInfo sequenceInfo, int baseTranspose, int partNumber) {
		this(sequenceInfo, baseTranspose, partNumber,
				sequenceInfo.getTitle() + " - " + LotroInstrument.LUTE.toString(), LotroInstrument.LUTE, true);
	}

	public AbcPart(SequenceInfo sequenceInfo, int baseTranspose, int partNumber, String title,
			LotroInstrument instrument, boolean enabled) {
		this.sequenceInfo = sequenceInfo;
		this.baseTranspose = baseTranspose;
		this.partNumber = partNumber;
		this.title = title;
		this.instrument = instrument;
		this.enabled = enabled;

		this.trackTranspose = new int[getTrackCount()];
		this.trackDisabled = new boolean[getTrackCount()];
	}

	public void exportToMidi(Sequence out, TimingInfo tm) throws AbcConversionException {
		exportToMidi(out, tm, 0, Integer.MAX_VALUE);
	}

	public void exportToMidi(Sequence out, TimingInfo tm, long songStartMicros, long songEndMicros)
			throws AbcConversionException {
		if (out.getDivisionType() != Sequence.PPQ || out.getResolution() != tm.getMidiResolution()) {
			throw new AbcConversionException("Sequence has incorrect timing data");
		}

		List<Chord> chords = combineAndQuantize(tm, songStartMicros, songEndMicros);
		int channel = out.getTracks().length;
		if (channel >= MidiConstants.DRUM_CHANNEL)
			channel++;
		Track track = out.createTrack();

		track.add(MidiFactory.createTrackNameEvent(title));
		track.add(MidiFactory.createProgramChangeEvent(instrument.midiProgramId, channel, 0));
		List<NoteEvent> notesOn = new ArrayList<NoteEvent>();

		for (Chord chord : chords) {
			for (int j = 0; j < chord.size(); j++) {
				NoteEvent ne = chord.get(j);
				if (ne.note == Note.REST)
					continue;

				Iterator<NoteEvent> onIter = notesOn.iterator();
				while (onIter.hasNext()) {
					NoteEvent on = onIter.next();
					if (on.endMicros < ne.startMicros) {
						// This note has already been turned off
						onIter.remove();
						track.add(MidiFactory.createNoteOffEvent(on.note.id, channel, tm.getMidiTicks(on.endMicros)));
					}
					else if (on.note.id == ne.note.id) {
						// Shorten the note that's
						// currently on to end at the same time that the next one starts
						on.endMicros = ne.startMicros;
						onIter.remove();
					}
				}

				if (!instrument.isSustainable(ne.note.id))
					ne.endMicros = ne.startMicros + ONE_SECOND_MICROS;

				track.add(MidiFactory.createNoteOnEvent(ne.note.id, channel, tm.getMidiTicks(ne.startMicros)));
				notesOn.add(ne);
			}
		}
		
		for (NoteEvent on : notesOn) {
			track.add(MidiFactory.createNoteOffEvent(on.note.id, channel, tm.getMidiTicks(on.endMicros)));
		}
	}

	public String exportToAbc(TimingInfo tm, KeySignature key) throws AbcConversionException {
		return exportToAbc(tm, key, 0, Integer.MAX_VALUE);
	}

	public String exportToAbc(TimingInfo tm, KeySignature key, long songStartMicros, long songEndMicros)
			throws AbcConversionException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		exportToAbc(tm, key, songStartMicros, songEndMicros, os);
		return os.toString();
	}

	public void exportToAbc(TimingInfo tm, KeySignature key, OutputStream os) throws AbcConversionException {
		exportToAbc(tm, key, 0, Integer.MAX_VALUE, os);
	}

	public void exportToAbc(TimingInfo tm, KeySignature key, long songStartMicros, long songEndMicros, OutputStream os)
			throws AbcConversionException {
		List<Chord> chords = combineAndQuantize(tm, songStartMicros, songEndMicros);

		if (key.sharpsFlats != 0)
			throw new AbcConversionException("Only C major and A minor are currently supported");

		PrintStream out = new PrintStream(os);
		out.println("X: " + partNumber);
		out.println("T: " + title);
		out.println("M: " + tm.meter);
		out.println("Q: " + tm.tempo);
		out.println("K: " + key);
		out.println();

		// Keep track of which notes have been sharped or flatted so 
		// we can naturalize them the next time they show up.
		boolean[] accented = new boolean[Note.C5.id + 1];

		// Write out ABC notation
		final int LINE_LENGTH = 80;
		int lineLength = 0;
		long barNumber = 0;
		StringBuilder bar = new StringBuilder();
		for (Chord c : chords) {
			if (c.size() == 0) {
				System.err.println("Chord has no notes!");
				continue;
			}

			if (barNumber != (c.getStartMicros() / tm.barLength)) {
				if (lineLength > 0 && (lineLength + bar.length()) > LINE_LENGTH) {
					out.println();              
					lineLength = 0;
				}
				out.print(bar);
				out.print("|");
				lineLength += bar.length();
				bar.setLength(0);
				barNumber = c.getStartMicros() / tm.barLength;
			}

			if (c.size() > 1) {
				bar.append('[');
			}

			int notesWritten = 0;
			for (int j = 0; j < c.size(); j++) {
				NoteEvent evt = c.get(j);
				if (evt.getLength() == 0) {
					System.err.println("Zero-length note");
					continue;
				}

				if (evt.note != Note.REST) {
					if (evt.note.isAccented()) {
						accented[evt.note.naturalId] = true;
					}
					else if (accented[evt.note.id]) {
						accented[evt.note.id] = false;
						bar.append('=');
					}
				}

				bar .append(evt.note.abc);
				int numerator = (int) (evt.getLength() / tm.minNoteLength);
				int denominator = tm.shortestDivisor;
				// Reduce the fraction
				int gcd = gcd(numerator, denominator);
				numerator /= gcd;
				denominator /= gcd;
				if (numerator != 1) {
					bar .append(numerator);
				}
				if (denominator != 1) {
					bar.append('/');
					if (numerator != 1 || denominator != 2)
						bar .append( denominator);
				}

				notesWritten++;
			}

			if (c.size() > 1) {
				if (notesWritten == 0) {
					// Remove the [
					bar.delete(bar.length() - 1, bar.length());
				}
				else {
					bar.append(']');
				}
			}

			bar.append(' ');
		}

		// Insert line breaks
		for (int i = LINE_LENGTH; i < bar.length(); i += LINE_LENGTH) {
			for (int j = 0; j < LINE_LENGTH - 1; j++, i--) {
				if (bar.charAt(i) == ' ') {
					bar.replace(i, i + 1, "\r\n");
					i++;
					break;
				}
			}
		}
		
		if (lineLength > 0 && (lineLength + bar.length()) > LINE_LENGTH)
			out.println();
		out.print(bar);
		out.print("|]");
	}

	/**
	 * Combine the tracks into one, quantize the note lengths, separate into
	 * chords.
	 */
	private List<Chord> combineAndQuantize(TimingInfo tm, long songStartMicros, long songEndMicros)
			throws AbcConversionException {

		// Combine the events from the enabled tracks
		List<NoteEvent> events = new ArrayList<NoteEvent>();
		for (int t = 0; t < getTrackCount(); t++) {
			if (!trackDisabled[t]) {
				for (NoteEvent ne : getTrackEvents(t)) {
					// Skip notes that are outside of the play range.
					if (ne.endMicros <= songStartMicros || ne.startMicros >= songEndMicros)
						continue;

					Note mappedNote = mapNote(ne.note.id, t);
					assert mappedNote.id >= instrument.lowestPlayable.id : mappedNote;
					assert mappedNote.id <= instrument.highestPlayable.id : mappedNote;
					if (mappedNote != null) {
						long start = Math.max(ne.startMicros, songStartMicros) - songStartMicros;
						long end = Math.min(ne.endMicros, songEndMicros) - songStartMicros;
						events.add(new NoteEvent(mappedNote, start, end));
					}
				}
			}
		}

		if (events.size() == 0)
			return Collections.emptyList();

		Collections.sort(events);

		// Quantize the events
		for (NoteEvent ne : events) {
			ne.startMicros = ((ne.startMicros + tm.minNoteLength / 2) / tm.minNoteLength) * tm.minNoteLength;
			ne.endMicros = ((ne.endMicros + tm.minNoteLength / 2) / tm.minNoteLength) * tm.minNoteLength;

			// Make sure the note didn't get quantized to zero length
			if (ne.getLength() == 0)
				ne.setLength(tm.minNoteLength);
		}

		// Add initial rest if necessary
		if (events.get(0).startMicros > 0) {
			events.add(0, new NoteEvent(Note.REST, 0, events.get(0).startMicros));
		}

		// Remove duplicate notes
		List<NoteEvent> notesOn = new ArrayList<NoteEvent>();
		Iterator<NoteEvent> neIter = events.iterator();
		dupLoop: while (neIter.hasNext()) {
			NoteEvent ne = neIter.next();
			Iterator<NoteEvent> onIter = notesOn.iterator();
			while (onIter.hasNext()) {
				NoteEvent on = onIter.next();
				if (on.endMicros < ne.startMicros) {
					// This note has already been turned off
					onIter.remove();
				}
				else if (on.note.id == ne.note.id) {
					if (on.startMicros == ne.startMicros) {
						// If they start at the same time, remove the second event.
						// Lengthen the first one if it's shorter than the second one.
						if (ne.endMicros > on.endMicros) {
							on.endMicros = ne.endMicros;
						}
						// Remove the duplicate note
						neIter.remove();
						continue dupLoop;
					}
					else {
						// Otherwise, if they don't start at the same time, shorten the note that's
						// currently on to end at the same time that the next one starts
						on.endMicros = ne.startMicros;
						onIter.remove();
					}
				}
			}
			notesOn.add(ne);
		}

		breakAndTieNotes(events, tm.maxNoteLength, tm.barLength);

		List<Chord> chords = new ArrayList<Chord>(events.size() / 2);
		List<NoteEvent> tmpEvents = new ArrayList<NoteEvent>();

		// Combine notes that play at the same time into chords
		Chord currentChord = new Chord(events.get(0));
		chords.add(currentChord);
		for (int i = 1; i < events.size(); i++) {
			NoteEvent ne = events.get(i);
			if (currentChord.getStartMicros() == ne.startMicros) {
				// This note starts at the same time as the rest of the notes in the chord
				currentChord.add(ne);
			}
			else {
				// Create a new chord
				Chord nextChord = new Chord(ne);

				// The next chord starts playing immediately after the *shortest* note (or rest) in
				// the current chord is finished, so we may need to add a rest inside the chord to
				// shorten it, or a rest after the chord to add a pause.

				if (currentChord.getEndMicros() > nextChord.getStartMicros()) {
					// Make sure there's room to add the rest
					while (currentChord.size() >= Chord.MAX_CHORD_NOTES) {
						currentChord.remove(currentChord.size() - 1);
					}
				}

				// Check the chord length again, since removing a note might have changed its length
				if (currentChord.getEndMicros() > nextChord.getStartMicros()) {
					// If the chord is too long, add a short rest in the chord to shorten it
					currentChord
							.add(new NoteEvent(Note.REST, currentChord.getStartMicros(), nextChord.getStartMicros()));
				}
				else if (currentChord.getEndMicros() < nextChord.getStartMicros()) {
					// If the chord is too short, insert a rest to fill the gap
					tmpEvents.clear();
					tmpEvents.add(new NoteEvent(Note.REST, currentChord.getEndMicros(), nextChord.getStartMicros()));
					breakAndTieNotes(tmpEvents, tm.maxNoteLength, tm.barLength);

					for (NoteEvent restEvent : tmpEvents) {
						restEvent.tiesTo = null;
						chords.add(new Chord(restEvent));
					}
				}

				chords.add(nextChord);
				currentChord = nextChord;
			}
		}

		return chords;
	}

	private void breakAndTieNotes(List<NoteEvent> events, int maxNoteLength, int barLength) {
		// Break long notes
		for (int i = 0; i < events.size(); i++) {
			NoteEvent ne = events.get(i);
			long endMicros = ne.endMicros;
			while (ne.getLength() > maxNoteLength) {
				ne.setLength(maxNoteLength);

				if (!instrument.isSustainable(ne.note.id))
					break;

				ne = new NoteEvent(ne.note, ne.endMicros, endMicros);
				int ins = Collections.binarySearch(events, ne);
				if (ins < 0)
					ins = -ins - 1;
				assert (ins > i);
				events.add(ins, ne);
			}
		}

		// Break and tie notes that cross bar boundaries
		for (int i = 0; i < events.size(); i++) {
			NoteEvent ne = events.get(i);
			long endMicros = ne.endMicros;
			long barEnd;
			while ((barEnd = (ne.startMicros / barLength + 1) * barLength) < endMicros) {
				ne.endMicros = barEnd;

				ne.tiesTo = new NoteEvent(ne.note, ne.endMicros, endMicros);
				ne = ne.tiesTo;

				int ins = Collections.binarySearch(events, ne);
				if (ins < 0)
					ins = -ins - 1;
				assert (ins > i);
				events.add((ins < 0) ? (-ins - 1) : ins, ne);
			}
		}
	}

	private static int gcd(int a, int b) {
		while (b != 0) {
			int t = b;
			b = a % b;
			a = t;
		}
		return a;
	}

	protected List<NoteEvent> getTrackEvents(int track) {
		return sequenceInfo.getTrackInfo(track).getNoteEvents();
	}

	/**
	 * Maps from a MIDI note to an ABC note. If no mapping is available, returns
	 * <code>null</code>.
	 */
	protected Note mapNote(int noteId, int track) {
		noteId += getBaseTranspose() + getTrackTranspose(track);
		while (noteId < instrument.lowestPlayable.id)
			noteId += 12;
		while (noteId > instrument.highestPlayable.id)
			noteId -= 12;
		return Note.fromId(noteId);
	}

	public long firstNoteStart() {
		long start = Long.MAX_VALUE;

		for (int t = 0; t < getTrackCount(); t++) {
			if (!trackDisabled[t]) {
				for (NoteEvent ne : getTrackEvents(t)) {
					if (mapNote(ne.note.id, t) != null) {
						if (ne.startMicros < start)
							start = ne.startMicros;
						break;
					}
				}
			}
		}

		return start;
	}

	public SequenceInfo getSequenceInfo() {
		return sequenceInfo;
	}

	public int getTrackCount() {
		return sequenceInfo.getTrackCount();
	}

	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		return getPartNumber() + ". " + getTitle();
	}

	public void setTitle(String name) {
		if (name == null)
			throw new NullPointerException();

		if (!this.title.equals(name)) {
			this.title = name;
			fireChangeEvent();
		}
	}

	public LotroInstrument getInstrument() {
		return instrument;
	}

	public void setInstrument(LotroInstrument instrument) {
		if (instrument == null)
			throw new NullPointerException();

		if (this.instrument != instrument) {
			this.instrument = instrument;
			fireChangeEvent();
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean partEnabled) {
		if (this.enabled != partEnabled) {
			this.enabled = partEnabled;
			fireChangeEvent();
		}
	}

	public int getBaseTranspose() {
		return baseTranspose;
	}

	public void setBaseTranspose(int baseTranspose) {
		if (this.baseTranspose != baseTranspose) {
			this.baseTranspose = baseTranspose;
			fireChangeEvent();
		}
	}

	public int getTrackTranspose(int track) {
		return trackTranspose[track];
	}

	public void setTrackTranspose(int track, int transpose) {
		if (trackTranspose[track] != transpose) {
			trackTranspose[track] = transpose;
			fireChangeEvent();
		}
	}

	public boolean isTrackEnabled(int track) {
		return !trackDisabled[track];
	}

	public void setTrackEnabled(int track, boolean enabled) {
		if (trackDisabled[track] != !enabled) {
			trackDisabled[track] = !enabled;
			fireChangeEvent();
		}
	}

	public int getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(int partNumber) {
		if (this.partNumber != partNumber) {
			this.partNumber = partNumber;
			fireChangeEvent();
		}
	}

	public void addChangeListener(ChangeListener l) {
		changeListeners.add(l);
	}

	public void removeChangeListener(ChangeListener l) {
		changeListeners.remove(l);
	}

	protected void fireChangeEvent() {
		if (changeListeners.size() > 0) {
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener l : changeListeners) {
				l.stateChanged(e);
			}
		}
	}
}

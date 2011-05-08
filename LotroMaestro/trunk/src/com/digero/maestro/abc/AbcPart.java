package com.digero.maestro.abc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import com.digero.common.abc.Dynamics;
import com.digero.common.abc.LotroInstrument;
import com.digero.common.abc.TimingInfo;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.Note;
import com.digero.common.midi.PanGenerator;
import com.digero.maestro.midi.Chord;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequenceInfo;

public class AbcPart {
	private SequenceInfo sequenceInfo;
	private boolean enabled;
	private int partNumber;
	private boolean dynamicVolume;
	private String title;
	private LotroInstrument instrument;
	private AbcMetadataSource metadata;
	private int baseTranspose;
	private int[] trackTranspose;
	private boolean[] trackEnabled;
	private final List<AbcPartListener> changeListeners = new ArrayList<AbcPartListener>();

	@SuppressWarnings("unchecked")
	public AbcPart(SequenceInfo sequenceInfo, int baseTranspose, AbcMetadataSource metadata) {
		this.sequenceInfo = sequenceInfo;
		this.baseTranspose = baseTranspose;
		this.metadata = metadata;
		this.instrument = LotroInstrument.LUTE;
		this.partNumber = 1;
		this.title = this.instrument.toString();
		this.enabled = true;
		this.dynamicVolume = true;

		int t = getTrackCount();
		this.trackTranspose = new int[t];
		this.trackEnabled = new boolean[t];
		this.drumsDisabled = new HashSet[t];
		this.drumNoteMap = new HashMap[t];
		for (int i = 0; i < t; i++) {
			this.drumNoteMap[i] = new HashMap<Integer, Integer>();
			this.drumsDisabled[i] = new HashSet<Integer>();
		}
	}

	public void exportToMidi(Sequence out, TimingInfo tm) throws AbcConversionException {
		exportToMidi(out, tm, 0, Integer.MAX_VALUE, 0, PanGenerator.CENTER);
	}

	public void exportToMidi(Sequence out, TimingInfo tm, long songStartMicros, long songEndMicros, int deltaVelocity,
			int pan) throws AbcConversionException {
		if (out.getDivisionType() != Sequence.PPQ || out.getResolution() != tm.getMidiResolution()) {
			throw new AbcConversionException("Sequence has incorrect timing data");
		}

		List<Chord> chords = combineAndQuantize(tm, false, songStartMicros, songEndMicros, deltaVelocity);
		int channel = out.getTracks().length;
		if (channel >= MidiConstants.DRUM_CHANNEL)
			channel++;
		Track track = out.createTrack();

		track.add(MidiFactory.createTrackNameEvent(title));
		track.add(MidiFactory.createProgramChangeEvent(instrument.midiProgramId, channel, 0));
		track.add(MidiFactory.createPanEvent(pan, channel));

		List<NoteEvent> notesOn = new ArrayList<NoteEvent>();

		for (Chord chord : chords) {
			Dynamics dynamics = chord.calcDynamics();
			if (dynamics == null)
				dynamics = Dynamics.DEFAULT;
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
						// Shorten the note that's currently on, to end at the same time that the next one starts
						on.endMicros = ne.startMicros;
						onIter.remove();
					}
				}

				if (!instrument.isSustainable(ne.note.id))
					ne.endMicros = ne.startMicros + TimingInfo.ONE_SECOND_MICROS;

				track.add(MidiFactory.createNoteOnEventEx(ne.note.id, channel, dynamics.abcVol, tm
						.getMidiTicks(ne.startMicros)));
				notesOn.add(ne);
			}
		}

		for (NoteEvent on : notesOn) {
			track.add(MidiFactory.createNoteOffEvent(on.note.id, channel, tm.getMidiTicks(on.getTieEnd().endMicros)));
		}
	}

	public String exportToAbc(TimingInfo tm, KeySignature key, long songStartMicros, long songEndMicros,
			int deltaVelocity) throws AbcConversionException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		exportToAbc(tm, key, songStartMicros, songEndMicros, deltaVelocity, os);
		return os.toString();
	}

	public void exportToAbc(TimingInfo tm, KeySignature key, long songStartMicros, long songEndMicros,
			int deltaVelocity, OutputStream os) throws AbcConversionException {
		List<Chord> chords = combineAndQuantize(tm, true, songStartMicros, songEndMicros, deltaVelocity);

		if (key.sharpsFlats != 0)
			throw new AbcConversionException("Only C major and A minor are currently supported");

		PrintStream out = new PrintStream(os);
		out.println("X: " + partNumber);
		if (metadata != null) {
			if (metadata.getSongTitle().length() > 0)
				out.println("T: " + (metadata.getSongTitle() + " - " + title + " " + metadata.getTitleTag()).trim());
			else
				out.println("T: " + (title + " " + metadata.getTitleTag()).trim());

			if (metadata.getComposer().length() > 0)
				out.println("C: " + metadata.getComposer());

			if (metadata.getTranscriber().length() > 0)
				out.println("Z: " + metadata.getTranscriber());
		}
		else {
			out.println("T: " + title.trim());
		}
		out.println("M: " + tm.meter);
		out.println("Q: " + tm.tempo);
		out.println("K: " + key);

		// Keep track of which notes have been sharped or flatted so 
		// we can naturalize them the next time they show up.
		boolean[] sharps = new boolean[Note.C5.id + 1];
		boolean[] flats = new boolean[Note.C5.id + 1];

		// Write out ABC notation
		final int LINE_LENGTH = 80;
		final int BAR_LENGTH = 160;
		int lineLength = 0;
		long barNumber = 0;
		StringBuilder sb = new StringBuilder();
		Dynamics curDyn = null;
		Dynamics initDyn = null;

		for (Chord c : chords) {
			initDyn = c.calcDynamics();
			if (initDyn != null)
				break;
		}

		for (Chord c : chords) {
			if (c.size() == 0) {
				System.err.println("Chord has no notes!");
				continue;
			}

			if (barNumber != (c.getStartMicros() / tm.barLength)) {
				barNumber = c.getStartMicros() / tm.barLength;

				if (barNumber % 10 == 1) {
					out.println();
					out.println("% Bar " + barNumber);
					lineLength = 0;
				}
				else if (lineLength > 0 && (lineLength + sb.length()) > LINE_LENGTH) {
					out.println();
					lineLength = 0;
				}

				// Trim end
				int length = sb.length();
				while (Character.isWhitespace(sb.charAt(length - 1)))
					length--;
				sb.setLength(length);

				// Insert line breaks inside very long bars
				for (int i = BAR_LENGTH; i < sb.length(); i += BAR_LENGTH) {
					for (int j = 0; j < BAR_LENGTH - 1; j++, i--) {
						if (sb.charAt(i) == ' ') {
							sb.replace(i, i + 1, "\r\n\t");
							i += "\r\n\t".length() - 1;
							break;
						}
					}
				}

				out.print(sb);
				out.print("  |  ");
				lineLength += sb.length() + 2;
				sb.setLength(0);

				Arrays.fill(sharps, false);
				Arrays.fill(flats, false);
			}

			Dynamics newDyn = (initDyn != null) ? initDyn : c.calcDynamics();
			initDyn = null;
			if (newDyn != null && newDyn != curDyn) {
				sb.append('+').append(newDyn).append("+ ");
				curDyn = newDyn;
			}

			if (c.size() > 1) {
				sb.append('[');
			}

			int notesWritten = 0;
			for (int j = 0; j < c.size(); j++) {
				NoteEvent evt = c.get(j);
				if (evt.getLength() == 0) {
					System.err.println("Zero-length note");
					continue;
				}

				String noteAbc = evt.note.abc;
				if (evt.note != Note.REST) {
					if (evt.note.isSharp()) {
						if (sharps[evt.note.naturalId])
							noteAbc = Note.fromId(evt.note.naturalId).abc;
						else
							sharps[evt.note.naturalId] = true;
					}
					else if (evt.note.isFlat()) {
						if (flats[evt.note.naturalId])
							noteAbc = Note.fromId(evt.note.naturalId).abc;
						else
							flats[evt.note.naturalId] = true;
					}
					else if (sharps[evt.note.id] || flats[evt.note.id]) {
						sharps[evt.note.id] = false;
						flats[evt.note.id] = false;
						sb.append('=');
					}
				}

				sb.append(noteAbc);
				int numerator = (int) (evt.getLength() / tm.minNoteLength) * tm.defaultDivisor;
				int denominator = tm.minNoteDivisor;
				// Reduce the fraction
				int gcd = gcd(numerator, denominator);
				numerator /= gcd;
				denominator /= gcd;
				if (numerator != 1) {
					sb.append(numerator);
				}
				if (denominator != 1) {
					sb.append('/');
					if (numerator != 1 || denominator != 2)
						sb.append(denominator);
				}

				if (evt.tiesTo != null)
					sb.append('-');

				notesWritten++;
			}

			if (c.size() > 1) {
				if (notesWritten == 0) {
					// Remove the [
					sb.delete(sb.length() - 1, sb.length());
				}
				else {
					sb.append(']');
				}
			}

			sb.append(' ');
		}

		// Insert line breaks
		for (int i = BAR_LENGTH; i < sb.length(); i += BAR_LENGTH) {
			for (int j = 0; j < BAR_LENGTH - 1; j++, i--) {
				if (sb.charAt(i) == ' ') {
					sb.replace(i, i + 1, "\r\n");
					i++;
					break;
				}
			}
		}

		if (lineLength > 0 && (lineLength + sb.length()) > LINE_LENGTH)
			out.println();
		out.print(sb);
		out.print(" |]");
		out.println();
		out.println();
	}

	/**
	 * Combine the tracks into one, quantize the note lengths, separate into
	 * chords.
	 */
	private List<Chord> combineAndQuantize(TimingInfo tm, boolean addTies, final long songStartMicros,
			final long songEndMicros, int deltaVelocity) throws AbcConversionException {

		// Combine the events from the enabled tracks
		List<NoteEvent> events = new ArrayList<NoteEvent>();
		for (int t = 0; t < getTrackCount(); t++) {
			if (isTrackEnabled(t)) {
				for (NoteEvent ne : getTrackEvents(t)) {
					// Skip notes that are outside of the play range.
					if (ne.endMicros <= songStartMicros || ne.startMicros >= songEndMicros)
						continue;

					Note mappedNote = mapNote(t, ne.note.id);
					if (mappedNote != null) {
						assert mappedNote.id >= instrument.lowestPlayable.id : mappedNote;
						assert mappedNote.id <= instrument.highestPlayable.id : mappedNote;
						long start = Math.max(ne.startMicros, songStartMicros) - songStartMicros;
						long end = Math.min(ne.endMicros, songEndMicros) - songStartMicros;
						int velocity = ne.velocity;
						if (dynamicVolume)
							velocity = Math.min(Math.max(velocity + deltaVelocity, 0), Dynamics.fff.midiVol);
						events.add(new NoteEvent(mappedNote, velocity, start, end));
					}
				}
			}
		}

		if (events.size() == 0)
			return Collections.emptyList();

		Collections.sort(events);

		// Add initial rest if necessary
		if (events.get(0).startMicros > 0) {
			events.add(0, new NoteEvent(Note.REST, Dynamics.DEFAULT.midiVol, 0, events.get(0).startMicros));
		}

		// Add a rest at the end if necessary
		if (songEndMicros < Long.MAX_VALUE) {
			NoteEvent lastEvent = events.get(events.size() - 1);
			long songLengthMicros = songEndMicros - songStartMicros;
			if (lastEvent.endMicros < songLengthMicros) {
				if (lastEvent.note == Note.REST) {
					lastEvent.endMicros = songLengthMicros;
				}
				else {
					events
							.add(new NoteEvent(Note.REST, Dynamics.DEFAULT.midiVol, lastEvent.endMicros,
									songLengthMicros));
				}
			}
		}

		// Quantize the events
		for (NoteEvent ne : events) {
			ne.startMicros = ((ne.startMicros + tm.minNoteLength / 2) / tm.minNoteLength) * tm.minNoteLength;
			ne.endMicros = ((ne.endMicros + tm.minNoteLength / 2) / tm.minNoteLength) * tm.minNoteLength;

			// Make sure the note didn't get quantized to zero length
			if (ne.getLength() == 0)
				ne.setLength(tm.minNoteLength);
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
						if (on.endMicros < ne.endMicros)
							on.endMicros = ne.endMicros;

						// Remove the duplicate note
						neIter.remove();
						continue dupLoop;
					}
					else {
						// Otherwise, if they don't start at the same time:
						// 1. Lengthen the second note if necessary, so it doesn't end before 
						//    the first note would have ended.
						if (ne.endMicros < on.endMicros)
							ne.endMicros = on.endMicros;

						// 2. Shorten the note that's currently on to end at the same time that 
						//    the next one starts.
						on.endMicros = ne.startMicros;
						onIter.remove();
					}
				}
			}
			notesOn.add(ne);
		}

		breakLongNotes(events, tm, addTies);

		List<Chord> chords = new ArrayList<Chord>(events.size() / 2);
		List<NoteEvent> tmpEvents = new ArrayList<NoteEvent>();

		// Combine notes that play at the same time into chords
		Chord curChord = new Chord(events.get(0));
		chords.add(curChord);
		for (int i = 1; i < events.size(); i++) {
			NoteEvent ne = events.get(i);
			if (curChord.getStartMicros() == ne.startMicros) {
				// This note starts at the same time as the rest of the notes in the chord
				if (!curChord.add(ne)) {
					// Couldn't add the note (too many notes in the chord)
					removeNote(events, i);
				}
			}
			else {
				// Create a new chord
				Chord nextChord = new Chord(ne);

				// The next chord starts playing immediately after the *shortest* note (or rest) in
				// the current chord is finished, so we may need to add a rest inside the chord to
				// shorten it, or a rest after the chord to add a pause.

				if (curChord.getEndMicros() > nextChord.getStartMicros()) {
					// Make sure there's room to add the rest
					while (curChord.size() >= Chord.MAX_CHORD_NOTES) {
						removeNote(events, curChord.remove(curChord.size() - 1));
					}
				}

				// Check the chord length again, since removing a note might have changed its length
				if (curChord.getEndMicros() > nextChord.getStartMicros()) {
					// If the chord is too long, add a short rest in the chord to shorten it
					curChord.add(new NoteEvent(Note.REST, Dynamics.DEFAULT.midiVol, curChord.getStartMicros(),
							nextChord.getStartMicros()));
				}
				else if (curChord.getEndMicros() < nextChord.getStartMicros()) {
					// If the chord is too short, insert a rest to fill the gap
					tmpEvents.clear();
					tmpEvents.add(new NoteEvent(Note.REST, Dynamics.DEFAULT.midiVol, curChord.getEndMicros(), nextChord
							.getStartMicros()));
					breakLongNotes(tmpEvents, tm, addTies);

					for (NoteEvent restEvent : tmpEvents)
						chords.add(new Chord(restEvent));
				}

				chords.add(nextChord);
				curChord = nextChord;
			}
		}

		return chords;
	}

	private void breakLongNotes(List<NoteEvent> events, TimingInfo tm, boolean addTies) {
		for (int i = 0; i < events.size(); i++) {
			NoteEvent ne = events.get(i);

			// Make a hard break for notes that are longer than LotRO can play
			long maxNoteEnd = ne.startMicros + tm.maxNoteLength;
			if (ne.endMicros > maxNoteEnd
			//      Bagpipe notes up to B, can sustain indefinitey; don't break them
					&& !(getInstrument() == LotroInstrument.BAGPIPE && ne.note.id <= Note.B2.id)) {

				// Align with a bar boundary if it extends across 1 or more full bars.
				if (tm.getBarEnd(ne.startMicros) < tm.getBarStart(maxNoteEnd)) {
					maxNoteEnd = tm.getBarStart(maxNoteEnd);
					assert ne.endMicros > maxNoteEnd;
				}

				// If the note is a rest or sustainable, add another one after 
				// this ends to keep it going...
				if (ne.note == Note.REST || instrument.isSustainable(ne.note.id)) {
					NoteEvent next = new NoteEvent(ne.note, ne.velocity, maxNoteEnd, ne.endMicros);
					int ins = Collections.binarySearch(events, next);
					if (ins < 0)
						ins = -ins - 1;
					assert (ins > i);
					events.add(ins, next);
				}

				ne.endMicros = maxNoteEnd;
			}

			if (addTies) {
				// Make a soft break (tie) for notes that cross bar boundaries
				long barEnd = tm.getBarEnd(ne.startMicros);
				if (ne.endMicros > barEnd) {
					NoteEvent next = new NoteEvent(ne.note, ne.velocity, barEnd, ne.endMicros);
					int ins = Collections.binarySearch(events, next);
					if (ins < 0)
						ins = -ins - 1;
					assert (ins > i);
					events.add((ins < 0) ? (-ins - 1) : ins, next);

					// Rests don't need to be tied
					if (ne.note != Note.REST) {
						next.tiesFrom = ne;
						ne.tiesTo = next;
					}

					ne.endMicros = barEnd;
				}
			}
		}
	}

	/** Removes a note and breaks any ties the note has. */
	private void removeNote(List<NoteEvent> events, int i) {
		NoteEvent ne = events.remove(i);

		// If the note is tied from another (previous) note, break the incoming tie
		if (ne.tiesFrom != null) {
			ne.tiesFrom.tiesTo = null;
			ne.tiesFrom = null;
		}

		// Remove the remainder of the notes that this is tied to (if any)
		for (NoteEvent neTie = ne.tiesTo; neTie != null; neTie = neTie.tiesTo) {
			events.remove(neTie);
		}
	}

	/** Removes a note and breaks any ties the note has. */
	private void removeNote(List<NoteEvent> events, NoteEvent ne) {
		removeNote(events, events.indexOf(ne));
	}

	private static int gcd(int a, int b) {
		while (b != 0) {
			int t = b;
			b = a % b;
			a = t;
		}
		return a;
	}

	public void dispose() {
		changeListeners.clear();
		sequenceInfo = null;
	}

	protected List<NoteEvent> getTrackEvents(int track) {
		return sequenceInfo.getTrackInfo(track).getEvents();
	}

	/**
	 * Maps from a MIDI note to an ABC note. If no mapping is available, returns
	 * <code>null</code>.
	 */
	protected Note mapNote(int track, int noteId) {
		if (isDrumPart()) {
			if (!isTrackEnabled(track) || !isDrumEnabled(track, noteId))
				return null;

			int dstNote = getDrumMapping(track, noteId);
			return (dstNote == DISABLED_DRUM_ID) ? null : Note.fromId(dstNote);
		}
		else {
			noteId += getTranspose(track);
			while (noteId < instrument.lowestPlayable.id)
				noteId += 12;
			while (noteId > instrument.highestPlayable.id)
				noteId -= 12;
			return Note.fromId(noteId);
		}
	}

	public long firstNoteStart() {
		long start = Long.MAX_VALUE;

		for (int t = 0; t < getTrackCount(); t++) {
			if (isTrackEnabled(t)) {
				for (NoteEvent ne : getTrackEvents(t)) {
					if (mapNote(t, ne.note.id) != null) {
						if (ne.startMicros < start)
							start = ne.startMicros;
						break;
					}
				}
			}
		}

		return start;
	}

	public long lastNoteEnd() {
		long end = Long.MIN_VALUE;

		for (int t = 0; t < getTrackCount(); t++) {
			if (isTrackEnabled(t)) {
				List<NoteEvent> evts = getTrackEvents(t);
				ListIterator<NoteEvent> iter = evts.listIterator(evts.size());
				while (iter.hasPrevious()) {
					NoteEvent ne = iter.previous();
					if (mapNote(t, ne.note.id) != null) {
						if (ne.endMicros > end)
							end = ne.endMicros;
						break;
					}
				}
			}
		}

		return end;
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
			fireChangeEvent(false);
		}
	}

	public LotroInstrument[] getSupportedInstruments() {
		//return LotroInstrument.getNonDrumInstruments();
		return LotroInstrument.values();
	}

	public LotroInstrument getInstrument() {
		return instrument;
	}

	public void setInstrument(LotroInstrument instrument) {
		if (instrument == null)
			throw new NullPointerException();

		if (this.instrument != instrument) {
			this.instrument = instrument;
			fireChangeEvent(true);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean partEnabled) {
		if (this.enabled != partEnabled) {
			this.enabled = partEnabled;
			fireChangeEvent(true);
		}
	}

	public int getBaseTranspose() {
		return isDrumPart() ? 0 : baseTranspose;
	}

	public void setBaseTranspose(int baseTranspose) {
		if (this.baseTranspose != baseTranspose) {
			this.baseTranspose = baseTranspose;
			fireChangeEvent(true);
		}
	}

	public int getTrackTranspose(int track) {
		return isDrumPart() ? 0 : trackTranspose[track];
	}

	public void setTrackTranspose(int track, int transpose) {
		if (trackTranspose[track] != transpose) {
			trackTranspose[track] = transpose;
			fireChangeEvent(true);
		}
	}

	public int getTranspose(int track) {
		return getBaseTranspose() + getTrackTranspose(track) - getInstrument().octaveDelta * 12;
	}

	public boolean isTrackEnabled(int track) {
		return trackEnabled[track];
	}

	public void setTrackEnabled(int track, boolean enabled) {
		if (trackEnabled[track] != enabled) {
			trackEnabled[track] = enabled;
			fireChangeEvent(true);
		}
	}

	public int getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(int partNumber) {
		if (this.partNumber != partNumber) {
			this.partNumber = partNumber;
			firePartNumberChanged();
		}
	}

	public boolean isDynamicVolume() {
		return dynamicVolume;
	}

	public void setDynamicVolume(boolean dynamicVolume) {
		if (this.dynamicVolume != dynamicVolume) {
			this.dynamicVolume = dynamicVolume;
			fireChangeEvent(true);
		}
	}

	public void addAbcListener(AbcPartListener l) {
		changeListeners.add(l);
	}

	public void removeAbcListener(AbcPartListener l) {
		changeListeners.remove(l);
	}

	protected void firePartNumberChanged() {
		fireChangeEventEx(false, true);
	}

	protected void fireChangeEvent(boolean previewRelated) {
		fireChangeEventEx(previewRelated, false);
	}

	protected void fireChangeEventEx(boolean previewRelated, boolean isPartNumber) {
		if (changeListeners.size() > 0) {
			AbcPartEvent e = new AbcPartEvent(this, previewRelated, isPartNumber);
			// Listener list might be modified in the callback
			List<AbcPartListener> listenerListCopy = new ArrayList<AbcPartListener>(changeListeners);
			for (AbcPartListener l : listenerListCopy) {
				l.abcPartChanged(e);
			}
		}
	}

	//
	// DRUMS
	//

	public boolean isDrumPart() {
		return instrument.isPercussion;
	}

	public boolean isDrumTrack(int track) {
		return sequenceInfo.getTrackInfo(track).isDrumTrack();
	}

	public static final int DISABLED_DRUM_ID = LotroDrumInfo.DISABLED.note.id;
	private Map<Integer, Integer>[] drumNoteMap;
	private Set<Integer>[] drumsDisabled;
	private Preferences drumPrefs = Preferences.userNodeForPackage(AbcPart.class).node("drums");

	public void setDrumMapping(int track, int srcNote, int dstNote) {
		if (getDrumMapping(track, srcNote) != dstNote) {
			drumNoteMap[track].put(srcNote, dstNote);
			if (isDrumTrack(track))
				drumPrefs.putInt(Integer.toString(srcNote), dstNote);
			fireChangeEvent(true);
		}
	}

	public int getDrumMapping(int track, int srcNote) {
		Integer dstNote = drumNoteMap[track].get(srcNote);
		if (dstNote == null) {
			if (isDrumTrack(track))
				dstNote = drumPrefs.getInt(Integer.toString(srcNote), DISABLED_DRUM_ID);
			else
				dstNote = Note.isPlayable(srcNote) ? srcNote : DISABLED_DRUM_ID;
			drumNoteMap[track].put(srcNote, dstNote);
		}
		return dstNote;
	}

	public boolean isDrumEnabled(int track, int drumId) {
		return !drumsDisabled[track].contains(drumId);
	}

	public void setDrumEnabled(int track, int noteId, boolean enabled) {
		if (isDrumEnabled(track, noteId) != enabled) {
			if (enabled)
				drumsDisabled[track].remove(noteId);
			else
				drumsDisabled[track].add(noteId);
			fireChangeEvent(true);
		}
	}
}

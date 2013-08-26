package com.digero.maestro.abc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.prefs.Preferences;

import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.common.abc.AbcField;
import com.digero.common.abc.Dynamics;
import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.Note;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Util;
import com.digero.maestro.midi.Chord;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.TrackInfo;

public class AbcPart implements AbcPartMetadataSource, NumberedAbcPart, IDiscardable {
	private SequenceInfo sequenceInfo;
	private boolean enabled;
	private int partNumber;
	private boolean dynamicVolume;
	private String title;
	private LotroInstrument instrument;
	private AbcProject ownerProject;
	private AbcMetadataSource metadata;
	private int baseTranspose;
	private int[] trackTranspose;
	private boolean[] trackEnabled;
	private int enabledTrackCount;
	private int previewSequenceTrackNumber = -1;
	private final List<AbcPartListener> changeListeners = new ArrayList<AbcPartListener>();

	public AbcPart(SequenceInfo sequenceInfo, int baseTranspose, AbcProject ownerProject, AbcMetadataSource metadata) {
		this.sequenceInfo = sequenceInfo;
		this.baseTranspose = baseTranspose;
		this.ownerProject = ownerProject;
		this.metadata = metadata;
		this.instrument = LotroInstrument.LUTE;
		this.partNumber = 1;
		this.title = this.instrument.toString();
		this.enabled = true;
		this.dynamicVolume = true;

		int t = getTrackCount();
		this.trackTranspose = new int[t];
		this.trackEnabled = new boolean[t];
		enabledTrackCount = 0;
		this.drumNoteMap = new DrumNoteMap[t];
	}

	public void exportToMidi(Sequence out, TimingInfo tm, long songStartMicros, long songEndMicros, int deltaVelocity,
			int pan, boolean useLotroInstruments) throws AbcConversionException {
		if (out.getDivisionType() != Sequence.PPQ || out.getResolution() != tm.getMidiResolution()) {
			throw new AbcConversionException("Sequence has incorrect timing data");
		}

		List<Chord> chords = combineAndQuantize(tm, false, songStartMicros, songEndMicros, deltaVelocity);
		exportToMidi(out, tm, chords, pan, useLotroInstruments);
	}

	public int exportToMidi(Sequence out, TimingInfo tm, List<Chord> chords, int pan, boolean useLotroInstruments) {
		int trackNumber = previewSequenceTrackNumber = out.getTracks().length;
		int channel = trackNumber;
		if (channel >= MidiConstants.DRUM_CHANNEL)
			channel++;

		Track track = out.createTrack();

		track.add(MidiFactory.createTrackNameEvent(title));
		track.add(MidiFactory.createProgramChangeEvent(instrument.midiProgramId, channel, 0));
		if (useLotroInstruments)
			track.add(MidiFactory.createChannelVolumeEvent(MidiConstants.MAX_VOLUME, channel, 1));
		track.add(MidiFactory.createPanEvent(pan, channel));

		List<NoteEvent> notesOn = new ArrayList<NoteEvent>();

		int noteDelta = 0;
		if (!useLotroInstruments)
			noteDelta = instrument.octaveDelta * 12;

		for (Chord chord : chords) {
			Dynamics dynamics = chord.calcDynamics();
			if (dynamics == null)
				dynamics = Dynamics.DEFAULT;
			for (int j = 0; j < chord.size(); j++) {
				NoteEvent ne = chord.get(j);
				// Skip rests and notes that are the continuation of a tied note
				if (ne.note == Note.REST || ne.tiesFrom != null)
					continue;

				// Add note off events for any notes that have been turned off by this point
				Iterator<NoteEvent> onIter = notesOn.iterator();
				while (onIter.hasNext()) {
					NoteEvent on = onIter.next();

					// Shorten the note to end at the same time that the next one starts
					long endMicros = on.endMicros;
					if (on.note.id == ne.note.id && on.endMicros > ne.startMicros)
						endMicros = ne.startMicros;

					if (endMicros <= ne.startMicros) {
						// This note has been turned off
						onIter.remove();
						track.add(MidiFactory.createNoteOffEvent(on.note.id + noteDelta, channel,
								tm.getMidiTicks(endMicros)));
					}
				}

				long endMicros = ne.getTieEnd().endMicros;

				// Lengthen Lute, Harp, Drums, etc. to play the entire sound sample
				if (useLotroInstruments && !instrument.isSustainable(ne.note.id))
					endMicros = ne.startMicros + TimingInfo.ONE_SECOND_MICROS;

				if (endMicros != ne.endMicros)
					ne = new NoteEvent(ne.note, ne.velocity, ne.startMicros, endMicros);

				int velocity = useLotroInstruments ? dynamics.abcVol : dynamics.midiVol;
				track.add(MidiFactory.createNoteOnEventEx(ne.note.id + noteDelta, channel, velocity,
						tm.getMidiTicks(ne.startMicros)));
				notesOn.add(ne);
			}
		}

		for (NoteEvent on : notesOn) {
			track.add(MidiFactory.createNoteOffEvent(on.note.id + noteDelta, channel, tm.getMidiTicks(on.endMicros)));
		}

		return trackNumber;
	}

	public TrackInfo exportToPreview(SequenceInfo sequenceInfo, TimingInfo tm, KeySignature key, int deltaVelocity,
			long songStartMicros, long songEndMicros, int pan, boolean useLotroInstruments)
			throws AbcConversionException {

		List<Chord> chords = combineAndQuantize(tm, false, songStartMicros, songEndMicros, deltaVelocity);

		int trackNumber = exportToMidi(sequenceInfo.getSequence(), tm, chords, pan, useLotroInstruments);

		List<NoteEvent> noteEvents = new ArrayList<NoteEvent>(chords.size());
		for (Chord chord : chords) {
			for (int i = 0; i < chord.size(); i++) {
				NoteEvent ne = chord.get(i);
				// Skip rests and notes that are the continuation of a tied note
				if (ne.note == Note.REST || ne.tiesFrom != null)
					continue;

				// Convert tied notes into a single note event
				if (ne.tiesTo != null) {
					ne.endMicros = ne.getTieEnd().endMicros;
					ne.tiesTo = null;
					// Not fixing up the ne.tiesTo.tiesFrom pointer since we that for the    
					// (ne.tiesFrom != null) check above, and we otherwise don't care about ne.tiesTo.
				}

				noteEvents.add(ne);
			}
		}

		return new TrackInfo(sequenceInfo, trackNumber, getTitle(), getInstrument(), tm.meter, key, noteEvents);
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
		out.println();
		out.println("X: " + partNumber);
		if (metadata != null)
			out.println("T: " + metadata.getPartName(this));
		else
			out.println("T: " + title.trim());

		out.println(AbcField.PART_NAME + title.trim());

		if (metadata != null) {
			if (metadata.getComposer().length() > 0)
				out.println("C: " + metadata.getComposer());

			if (metadata.getTranscriber().length() > 0)
				out.println("Z: " + metadata.getTranscriber());
		}

		out.println("M: " + tm.meter);
		out.println("Q: " + tm.exportTempo);
		out.println("K: " + key);
		out.println();

		// Keep track of which notes have been sharped or flatted so 
		// we can naturalize them the next time they show up.
		boolean[] sharps = new boolean[Note.C5.id + 1];
		boolean[] flats = new boolean[Note.C5.id + 1];

		// Write out ABC notation
		final int LINE_LENGTH = 1; // Setting this to 1 is a hack to force each bar on its own line
		final int BAR_LENGTH = 160;
		int lineLength = 0;
		long barNumber = 0;
		StringBuilder bar = new StringBuilder();
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

			c.sort();

			// Is this the start of a new bar?
			if (barNumber != (c.getStartMicros() / tm.barLength)) {
				barNumber = c.getStartMicros() / tm.barLength;

				if (barNumber % 10 == 0) {
					out.println();
					out.print("% Bar " + barNumber);
					if (barNumber > 1)
						out.print(" (" + Util.formatDuration((barNumber - 1) * tm.barLength) + ")");
					out.println();
					lineLength = 0;
				}
				else if (lineLength > 0 && (lineLength + bar.length()) > LINE_LENGTH) {
					out.println();
					lineLength = 0;
				}

				// Trim end
				int length = bar.length();
				while (Character.isWhitespace(bar.charAt(length - 1)))
					length--;
				bar.setLength(length);

				// Insert line breaks inside very long bars
				for (int i = BAR_LENGTH; i < bar.length(); i += BAR_LENGTH) {
					for (int j = 0; j < BAR_LENGTH - 1; j++, i--) {
						if (bar.charAt(i) == ' ') {
							bar.replace(i, i + 1, "\r\n\t");
							i += "\r\n\t".length() - 1;
							break;
						}
					}
				}

				out.print(bar);
				out.print(" |");
				lineLength += bar.length() + 2;
				bar.setLength(0);

				Arrays.fill(sharps, false);
				Arrays.fill(flats, false);
			}

			Dynamics newDyn = (initDyn != null) ? initDyn : c.calcDynamics();
			initDyn = null;
			if (newDyn != null && newDyn != curDyn) {
				bar.append('+').append(newDyn).append("+ ");
				curDyn = newDyn;
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
						bar.append('=');
					}
				}

				bar.append(noteAbc);
				int numerator = (int) (evt.getLength() / tm.minNoteLength) * tm.defaultDivisor;
				int denominator = tm.minNoteDivisor;
				// Reduce the fraction
				int gcd = Util.gcd(numerator, denominator);
				numerator /= gcd;
				denominator /= gcd;
				if (numerator != 1) {
					bar.append(numerator);
				}
				if (denominator != 1) {
					bar.append('/');
					if (numerator != 1 || denominator != 2)
						bar.append(denominator);
				}

				if (evt.tiesTo != null)
					bar.append('-');

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
		for (int i = BAR_LENGTH; i < bar.length(); i += BAR_LENGTH) {
			for (int j = 0; j < BAR_LENGTH - 1; j++, i--) {
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
		out.println("|]");
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
							velocity = Math.min(Math.max(velocity + deltaVelocity, 0), Dynamics.MAXIMUM.midiVol);
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
					events.add(new NoteEvent(Note.REST, Dynamics.DEFAULT.midiVol, lastEvent.endMicros, songLengthMicros));
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
					i--;
				}
			}
			else {
				// Create a new chord
				Chord nextChord = new Chord(ne);

				if (addTies) {
					// The curChord has all the notes it will get. But before continuing, 
					// normalize the chord so that all notes end at the same time and end 
					// before the next chord starts.
					boolean reprocessCurrentNote = false;
					long targetEndMicros = Math.min(nextChord.getStartMicros(), curChord.getEndMicros());

					for (int j = 0; j < curChord.size(); j++) {
						NoteEvent jne = curChord.get(j);
						if (jne.endMicros > targetEndMicros) {
							// This note extends past the end of the chord; break it into two tied notes
							NoteEvent next = jne.splitWithTie(targetEndMicros);

							int ins = Collections.binarySearch(events, next);
							if (ins < 0)
								ins = -ins - 1;
							assert (ins >= i);
							// If we're inserting before the current note, back up and process the added note
							if (ins == i)
								reprocessCurrentNote = true;

							events.add(ins, next);
						}
					}

					// The shorter notes will have changed the chord's duration
					if (targetEndMicros < curChord.getEndMicros())
						curChord.recalcEndMicros();

					if (reprocessCurrentNote) {
						i--;
						continue;
					}
				}
				else {
					// If we're note allowed to add ties, use the old method of shortening the 
					// chord by inserting a short rest. 

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
				}

				// Insert a rest between the chords if needed
				if (curChord.getEndMicros() < nextChord.getStartMicros()) {
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

					/*
					 * If the final note is less than a full bar length, just
					 * tie it to the original note rather than creating a hard
					 * break. We don't want the last piece of a long sustained
					 * note to be a short blast. LOTRO won't complain about a
					 * note being too long if it's part of a tie.
					 */
					if (next.getLength() < tm.barLength && ne.note != Note.REST) {
						next.tiesFrom = ne;
						ne.tiesTo = next;
					}
				}

				ne.endMicros = maxNoteEnd;
			}

			if (addTies) {
				// Make a soft break (tie) for notes that cross bar boundaries
				long barEnd = tm.getBarEnd(ne.startMicros);
				if (ne.endMicros > barEnd) {
					NoteEvent next = ne.splitWithTie(barEnd);
					int ins = Collections.binarySearch(events, next);
					if (ins < 0)
						ins = -ins - 1;
					assert (ins > i);
					events.add(ins, next);
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

	@Override
	public void discard() {
		changeListeners.clear();
		sequenceInfo = null;
		for (int i = 0; i < drumNoteMap.length; i++) {
			if (drumNoteMap[i] != null) {
				drumNoteMap[i].removeChangeListener(drumMapChangeListener);
				drumNoteMap[i] = null;
			}
		}
	}

	protected List<NoteEvent> getTrackEvents(int track) {
		return sequenceInfo.getTrackInfo(track).getEvents();
	}

	/**
	 * Maps from a MIDI note to an ABC note. If no mapping is available, returns
	 * <code>null</code>.
	 */
	public Note mapNote(int track, int noteId) {
		if (isDrumPart()) {
			if (!isTrackEnabled(track) || !isDrumEnabled(track, noteId))
				return null;

			int dstNote;
			if (instrument == LotroInstrument.COWBELL)
				dstNote = Note.G2.id; // "Tom High 1"
			else if (instrument == LotroInstrument.MOOR_COWBELL)
				dstNote = Note.A2.id; // "Tom High 2"
			else
				dstNote = getDrumMap(track).get(noteId);

			return (dstNote == LotroDrumInfo.DISABLED.note.id) ? null : Note.fromId(dstNote);
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

	public long lastNoteEnd(boolean accountForSustain) {
		long end = Long.MIN_VALUE;

		// The last note to start playing isn't necessarily the last note to end.
		// Check the last several notes to find the one that ends last.
		int notesToCheck = 1000;

		for (int t = 0; t < getTrackCount(); t++) {
			if (isTrackEnabled(t)) {
				List<NoteEvent> evts = getTrackEvents(t);
				ListIterator<NoteEvent> iter = evts.listIterator(evts.size());
				while (iter.hasPrevious()) {
					NoteEvent ne = iter.previous();
					if (mapNote(t, ne.note.id) != null) {
						long noteEnd;
						if (!accountForSustain || instrument.isSustainable(ne.note.id))
							noteEnd = ne.endMicros;
						else
							noteEnd = ne.startMicros + TimingInfo.ONE_SECOND_MICROS;

						if (noteEnd > end)
							end = noteEnd;

						if (--notesToCheck <= 0)
							break;
					}
				}
			}
		}

		return end;
	}

	public AbcProject getOwnerProject() {
		return ownerProject;
	}

	public SequenceInfo getSequenceInfo() {
		return sequenceInfo;
	}

	public int getTrackCount() {
		return sequenceInfo.getTrackCount();
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		String val = getPartNumber() + ". " + getTitle();
		if (getEnabledTrackCount() == 0)
			val += "*";
		return val;
	}

	public void setTitle(String name) {
		if (name == null)
			throw new NullPointerException();

		if (!this.title.equals(name)) {
			this.title = name;
			fireChangeEvent(AbcPartProperty.TITLE);
		}
	}

	public LotroInstrument[] getSupportedInstruments() {
		//return LotroInstrument.getNonDrumInstruments();
		return LotroInstrument.values();
	}

	@Override
	public LotroInstrument getInstrument() {
		return instrument;
	}

	@Override
	public void setInstrument(LotroInstrument instrument) {
		if (instrument == null)
			throw new NullPointerException();

		if (this.instrument != instrument) {
			this.instrument = instrument;
			boolean affectsPreview = false;
			for (boolean enabled : trackEnabled) {
				if (enabled) {
					affectsPreview = true;
					break;
				}
			}
			fireChangeEvent(AbcPartProperty.INSTRUMENT, affectsPreview);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean partEnabled) {
		if (this.enabled != partEnabled) {
			this.enabled = partEnabled;
			fireChangeEvent(AbcPartProperty.ENABLED);
		}
	}

	public int getBaseTranspose() {
		return isDrumPart() ? 0 : baseTranspose;
	}

	public void setBaseTranspose(int baseTranspose) {
		if (this.baseTranspose != baseTranspose) {
			this.baseTranspose = baseTranspose;
			fireChangeEvent(AbcPartProperty.BASE_TRANSPOSE, !isDrumPart() /* affectsAbcPreview */);
		}
	}

	public int getTrackTranspose(int track) {
		return isDrumPart() ? 0 : trackTranspose[track];
	}

	public void setTrackTranspose(int track, int transpose) {
		if (trackTranspose[track] != transpose) {
			trackTranspose[track] = transpose;
			fireChangeEvent(AbcPartProperty.TRACK_TRANSPOSE, isTrackEnabled(track) /* previewRelated */);
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
			enabledTrackCount += enabled ? 1 : -1;
			fireChangeEvent(AbcPartProperty.TRACK_ENABLED);
		}
	}

	public int getEnabledTrackCount() {
		return enabledTrackCount;
	}

	public int getPreviewSequenceTrackNumber() {
		return previewSequenceTrackNumber;
	}

	@Override
	public int getPartNumber() {
		return partNumber;
	}

	@Override
	public void setPartNumber(int partNumber) {
		if (this.partNumber != partNumber) {
			this.partNumber = partNumber;
			fireChangeEvent(AbcPartProperty.PART_NUMBER);
		}
	}

	public boolean isDynamicVolume() {
		return dynamicVolume;
	}

	public void setDynamicVolume(boolean dynamicVolume) {
		if (this.dynamicVolume != dynamicVolume) {
			this.dynamicVolume = dynamicVolume;
			fireChangeEvent(AbcPartProperty.DYNAMIC_VOLUME);
		}
	}

	public void addAbcListener(AbcPartListener l) {
		changeListeners.add(l);
	}

	public void removeAbcListener(AbcPartListener l) {
		changeListeners.remove(l);
	}

	protected void fireChangeEvent(AbcPartProperty property) {
		fireChangeEvent(property, property.isAbcPreviewRelated());
	}

	protected void fireChangeEvent(AbcPartProperty property, boolean abcPreviewRelated) {
		if (changeListeners.size() > 0) {
			AbcPartEvent e = new AbcPartEvent(this, property, abcPreviewRelated);
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

	public boolean isCowbellPart() {
		return instrument == LotroInstrument.COWBELL || instrument == LotroInstrument.MOOR_COWBELL;
	}

	public boolean isDrumTrack(int track) {
		return sequenceInfo.getTrackInfo(track).isDrumTrack();
	}

	private DrumNoteMap[] drumNoteMap;
	private BitSet[] drumsEnabled;
	private BitSet[] cowbellsEnabled;
	private Preferences drumPrefs = Preferences.userNodeForPackage(AbcPart.class).node("drums");

	public DrumNoteMap getDrumMap(int track) {
		if (drumNoteMap[track] == null) {
			// For non-drum tracks, just use a straight pass-through
			if (!sequenceInfo.getTrackInfo(track).isDrumTrack()) {
				drumNoteMap[track] = new PassThroughDrumNoteMap();
			}
			else {
				drumNoteMap[track] = new DrumNoteMap();
				drumNoteMap[track].load(drumPrefs);
			}
			drumNoteMap[track].addChangeListener(drumMapChangeListener);
		}
		return drumNoteMap[track];
	}

	private final ChangeListener drumMapChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() instanceof DrumNoteMap) {
				DrumNoteMap map = (DrumNoteMap) e.getSource();

				// Don't write pass-through drum maps to the prefs node 
				// these are used for non-drum tracks and their mapping 
				// isn't desirable to save.
				if (!(map instanceof PassThroughDrumNoteMap))
					map.save(drumPrefs);

				fireChangeEvent(AbcPartProperty.DRUM_MAPPING);
			}
		}
	};

	public boolean isDrumPlayable(int track, int drumId) {
		if (isCowbellPart())
			return true;

		return getDrumMap(track).get(drumId) != LotroDrumInfo.DISABLED.note.id;
	}

	public boolean isDrumEnabled(int track, int drumId) {
		BitSet[] enabledSet = isCowbellPart() ? cowbellsEnabled : drumsEnabled;

		if (enabledSet == null || enabledSet[track] == null) {
			return !isCowbellPart() || (drumId == MidiConstants.COWBELL_DRUM_ID)
					|| !sequenceInfo.getTrackInfo(track).isDrumTrack();
		}

		return enabledSet[track].get(drumId);
	}

	public void setDrumEnabled(int track, int drumId, boolean enabled) {
		if (isDrumEnabled(track, drumId) != enabled) {
			if (drumsEnabled == null) {
				drumsEnabled = new BitSet[getTrackCount()];
			}
			if (cowbellsEnabled == null) {
				cowbellsEnabled = new BitSet[getTrackCount()];
			}

			BitSet[] enabledSet = isCowbellPart() ? cowbellsEnabled : drumsEnabled;
			if (enabledSet[track] == null) {
				enabledSet[track] = new BitSet(MidiConstants.NOTE_COUNT);
				if (isCowbellPart())
					enabledSet[track].set(MidiConstants.COWBELL_DRUM_ID, true);
				else
					enabledSet[track].set(0, MidiConstants.NOTE_COUNT, true);
			}
			enabledSet[track].set(drumId, enabled);
			fireChangeEvent(AbcPartProperty.DRUM_ENABLED);
		}
	}
}

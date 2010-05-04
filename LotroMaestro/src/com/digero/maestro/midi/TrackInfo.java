package com.digero.maestro.midi;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.sun.media.sound.MidiUtils;

public class TrackInfo implements IMidiConstants {
	private SequenceInfo sequenceInfo;

	private Track track;
	private int trackNumber;
	private String name;
	private TimeSignature timeSignature = null;
	private KeySignature keySignature = null;
	private Set<Integer> instruments = new HashSet<Integer>();
	private List<NoteEvent> noteEvents;
	private List<NoteEvent> drumEvents;
	private SortedSet<Integer> drumsInUse;

	TrackInfo(SequenceInfo parent, Track track, int trackNumber, MidiUtils.TempoCache tempoCache,
			InstrumentChangeCache instrumentCache) throws InvalidMidiDataException {
		this.sequenceInfo = parent;
		this.track = track;
		this.trackNumber = trackNumber;

		Sequence song = sequenceInfo.getSequence();

		noteEvents = new ArrayList<NoteEvent>();
		drumEvents = new ArrayList<NoteEvent>();
		drumsInUse = new TreeSet<Integer>();
		List<NoteEvent> notesOn = new ArrayList<NoteEvent>();
		List<NoteEvent> drumsOn = new ArrayList<NoteEvent>();
		int notesNotTurnedOff = 0;

		for (int j = 0, sz = track.size(); j < sz; j++) {
			int pitchBend = 8192;
			MidiEvent evt = track.get(j);
			MidiMessage msg = evt.getMessage();

			if (msg instanceof ShortMessage) {
				ShortMessage m = (ShortMessage) msg;
				int cmd = m.getCommand();
				boolean drums = (m.getChannel() == DRUM_CHANNEL);

				if (cmd == ShortMessage.NOTE_ON || cmd == ShortMessage.NOTE_OFF) {
					int noteId = m.getData1();
					int velocity = m.getData2();
					long micros = MidiUtils.tick2microsecond(song, evt.getTick(), tempoCache);

					if (cmd == ShortMessage.NOTE_ON && velocity > 0) {
						Note note = Note.fromId(noteId);
						if (note == null)
							throw new InvalidMidiDataException("Encountered unrecognized note ID: " + noteId);

						NoteEvent ne = new NoteEvent(note, micros);

						Iterator<NoteEvent> onIter = (drums ? drumsOn : notesOn).iterator();
						while (onIter.hasNext()) {
							NoteEvent on = onIter.next();
							if (on.note.id == ne.note.id) {
								onIter.remove();
								(drums ? drumEvents : noteEvents).remove(on);
								notesNotTurnedOff++;
								break;
							}
						}

						if (drums) {
							if (noteEvents.isEmpty())
								instruments.clear();
							drumEvents.add(ne);
							drumsInUse.add(ne.note.id);
							drumsOn.add(ne);
						}
						else {
							noteEvents.add(ne);
							notesOn.add(ne);
							instruments.add(instrumentCache.getInstrument(evt.getTick(), m.getChannel()));
						}
					}
					else {
						Iterator<NoteEvent> iter = drums ? drumsOn.iterator() : notesOn.iterator();
						while (iter.hasNext()) {
							NoteEvent ne = iter.next();
							if (ne.note.id == m.getData1()) {
								iter.remove();
								ne.endMicros = micros;
								break;
							}
						}
					}
				}
				else if (cmd == ShortMessage.PROGRAM_CHANGE && !drums) {
					int instrument = m.getData1();

					if (noteEvents.isEmpty())
						instruments.clear();

					if (!instruments.contains(instrument))
						instruments.add(instrument);
				}
			}
			else if (msg instanceof MetaMessage) {
				MetaMessage m = (MetaMessage) msg;
				int type = m.getType();

				if (type == META_TRACK_NAME && name == null) {
					try {
						byte[] data = m.getData();
						String tmp = new String(data, 0, data.length, "US-ASCII").trim();
						if (tmp.length() > 0)
							name = tmp;
					}
					catch (UnsupportedEncodingException ex) {
						// Ignore.  This should never happen...
					}
				}
				else if (type == META_KEY_SIGNATURE && keySignature == null) {
					keySignature = new KeySignature(m);
				}
				else if (type == META_TIME_SIGNATURE && timeSignature == null) {
					timeSignature = new TimeSignature(m);
				}
			}
		}

		// Turn off notes that are on at the end of the song.  This shouldn't happen...
		if (notesOn.size() > 0 || drumsOn.size() > 0) {
			System.err.println((notesOn.size() + drumsOn.size() + notesNotTurnedOff)
					+ " note(s) not turned off at the end of the track.");

			noteEvents.removeAll(notesOn);
			drumEvents.removeAll(drumsOn);
//			for (NoteEvent ne : notesOn)
//				ne.endMicros = song.getMicrosecondLength();
//
//			for (NoteEvent ne : drumsOn)
//				ne.endMicros = song.getMicrosecondLength();
		}

		noteEvents = Collections.unmodifiableList(noteEvents);
		drumEvents = Collections.unmodifiableList(drumEvents);
		drumsInUse = Collections.unmodifiableSortedSet(drumsInUse);
	}

	public SequenceInfo getSequenceInfo() {
		return sequenceInfo;
	}

	public Track getTrack() {
		return track;
	}

	public int getTrackNumber() {
		return trackNumber;
	}

	public boolean hasName() {
		return name != null;
	}

	public String getName() {
		if (name == null)
			return "Track " + trackNumber;
		return name;
	}

	public KeySignature getKeySignature() {
		return keySignature;
	}

	public TimeSignature getTimeSignature() {
		return timeSignature;
	}

	@Override
	public String toString() {
		return getName();
	}

	public boolean hasDrums() {
		return drumEvents.size() > 0;
	}

	public boolean hasNotes() {
		return noteEvents.size() > 0;
	}

	/** Gets an unmodifiable list of the note events in this track. */
	public List<NoteEvent> getNoteEvents() {
		return noteEvents;
	}

	public List<NoteEvent> getDrumEvents() {
		return drumEvents;
	}

	public SortedSet<Integer> getDrumsInUse() {
		return drumsInUse;
	}

	public int getNoteCount() {
		return noteEvents.size();
	}

	public int getDrumCount() {
		return drumEvents.size();
	}

	public String getNoteCountString() {
		if (getNoteCount() == 1) {
			return "1 note";
		}
		return getNoteCount() + " notes";
	}

	public String getInstrumentNames() {
		if (instruments.size() == 0) {
			if (hasDrums() && hasNotes())
				return "Drums, " + MidiConstants.getInstrumentName(0);
			else if (hasDrums())
				return "Drums";
			else if (hasNotes())
				return MidiConstants.getInstrumentName(0);
			else
				return "<None>";
		}

		String names = "";
		boolean first = true;
		for (int i : instruments) {
			if (!first)
				names += ", ";
			else
				first = false;

			names += MidiConstants.getInstrumentName(i);
		}

		if (hasDrums())
			names = "Drums, " + names;

		return names;
	}

	public int getInstrumentCount() {
		return instruments.size();
	}
}
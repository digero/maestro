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

import com.digero.common.abc.TimingInfo;
import com.digero.common.midi.IMidiConstants;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.TimeSignature;
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
	private int[] noteVelocities = new int[128];

	@SuppressWarnings("unchecked")
	TrackInfo(SequenceInfo parent, Track track, int trackNumber, MidiUtils.TempoCache tempoCache,
			InstrumentChangeCache instrumentCache) throws InvalidMidiDataException {
		this.sequenceInfo = parent;
		this.track = track;
		this.trackNumber = trackNumber;

		Sequence song = sequenceInfo.getSequence();

		noteEvents = new ArrayList<NoteEvent>();
		drumEvents = new ArrayList<NoteEvent>();
		drumsInUse = new TreeSet<Integer>();
		List<NoteEvent>[] notesOn = new List[16];
		int notesNotTurnedOff = 0;

		int[] pitchBend = new int[16];
		for (int j = 0, sz = track.size(); j < sz; j++) {
			MidiEvent evt = track.get(j);
			MidiMessage msg = evt.getMessage();

			if (msg instanceof ShortMessage) {
				ShortMessage m = (ShortMessage) msg;
				int cmd = m.getCommand();
				int c = m.getChannel();
				boolean drums = (c == DRUM_CHANNEL);

				if (notesOn[c] == null)
					notesOn[c] = new ArrayList<NoteEvent>();

				if (cmd == ShortMessage.NOTE_ON || cmd == ShortMessage.NOTE_OFF) {
					int noteId = m.getData1() + (drums ? 0 : pitchBend[c]);
					int velocity = m.getData2();
					long micros = MidiUtils.tick2microsecond(song, evt.getTick(), tempoCache);

					if (cmd == ShortMessage.NOTE_ON && velocity > 0) {
						Note note = Note.fromId(noteId);
						if (note == null)
							throw new InvalidMidiDataException("Encountered unrecognized note ID: " + noteId);

						NoteEvent ne = new NoteEvent(note, velocity, micros, micros);

						Iterator<NoteEvent> onIter = notesOn[c].iterator();
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
						}
						else {
							noteEvents.add(ne);
							instruments.add(instrumentCache.getInstrument(evt.getTick(), c));
						}
						notesOn[c].add(ne);
						noteVelocities[velocity]++;
					}
					else {
						Iterator<NoteEvent> iter = notesOn[c].iterator();
						while (iter.hasNext()) {
							NoteEvent ne = iter.next();
							if (ne.note.id == noteId) {
								iter.remove();
								ne.endMicros = micros;
								break;
							}
						}
					}
				}
				else if (cmd == ShortMessage.PITCH_BEND && !drums) {
					final int STEP_SIZE = ((1 << 14) - 1) / 4;
					int bendTmp = ((m.getData1() | (m.getData2() << 7)) + STEP_SIZE / 2) / STEP_SIZE - 2;
					long micros = MidiUtils.tick2microsecond(song, evt.getTick(), tempoCache);

					if (bendTmp != pitchBend[c]) {
						List<NoteEvent> bentNotes = new ArrayList<NoteEvent>();
						for (NoteEvent ne : notesOn[c]) {
							ne.endMicros = micros;
							if (ne.getLength() < TimingInfo.SHORTEST_NOTE_MICROS) {
								noteEvents.remove(ne);
							}

							Note bn = Note.fromId(ne.note.id + bendTmp - pitchBend[c]);
							NoteEvent bne = new NoteEvent(bn, ne.velocity, micros, micros);
							noteEvents.add(bne);
							bentNotes.add(bne);
						}
						notesOn[c] = bentNotes;
						pitchBend[c] = bendTmp;
					}
				}
			}
			else if (msg instanceof MetaMessage) {
				MetaMessage m = (MetaMessage) msg;
				int type = m.getType();

				if (type == META_TRACK_NAME && name == null) {
					try {
						byte[] data = m.getData();
						String tmp = new String(data, 0, data.length, "US-ASCII").trim();
						if (tmp.length() > 0 && !tmp.equalsIgnoreCase("untitled")
								&& !tmp.equalsIgnoreCase("WinJammer Demo"))
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
		int ctNotesOn = 0;
		for (List<NoteEvent> notesOnChannel : notesOn) {
			if (notesOnChannel != null)
				ctNotesOn += notesOnChannel.size();
		}
		if (ctNotesOn > 0) {
			System.err.println((ctNotesOn + notesNotTurnedOff) + " note(s) not turned off at the end of the track.");

			for (List<NoteEvent> notesOnChannel : notesOn) {
				if (notesOnChannel != null)
					noteEvents.removeAll(notesOnChannel);
			}
			drumEvents.removeAll(notesOn[DRUM_CHANNEL]);
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

	public int[] addNoteVelocities(int[] velocities) {
		if (velocities == null)
			velocities = new int[this.noteVelocities.length];
		for (int i = 0; i < this.noteVelocities.length; i++) {
			velocities[i] += this.noteVelocities[i];
		}
		return velocities;
	}
}
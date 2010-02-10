package com.digero.maestro.midi;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.sun.media.sound.MidiUtils;

public class TrackInfo implements MidiConstants {
	private SequenceInfo sequenceInfo;

	private Track track;
	private int trackNumber;
	private String name;
	private TimeSignature timeSignature = null;
	private KeySignature keySignature = null;
	private List<Integer> instruments = new ArrayList<Integer>();
	private List<NoteEvent> noteEvents;
	private List<NoteEvent> drumEvents;

	TrackInfo(SequenceInfo parent, Track track, int trackNumber, MidiUtils.TempoCache tempoCache) {
		this.sequenceInfo = parent;
		this.track = track;
		this.trackNumber = trackNumber;

		Sequence song = sequenceInfo.getSequence();

		noteEvents = new ArrayList<NoteEvent>();
		drumEvents = new ArrayList<NoteEvent>();
		List<NoteEvent> notesOn = new ArrayList<NoteEvent>();
		List<NoteEvent> drumsOn = new ArrayList<NoteEvent>();

		for (int j = 0, sz = track.size(); j < sz; j++) {
			MidiEvent evt = track.get(j);
			MidiMessage msg = evt.getMessage();

			if (msg instanceof ShortMessage) {
				ShortMessage m = (ShortMessage) msg;
				boolean drums = (m.getChannel() == DRUM_CHANNEL);

				switch (m.getCommand()) {
				case ShortMessage.NOTE_ON:
					if (m.getData2()/* speed */!= 0) {
						Note note = Note.fromId(m.getData1());
						if (note != null) {
							long micros = MidiUtils.tick2microsecond(song, evt.getTick(), tempoCache);
							NoteEvent ne = new NoteEvent(note, trackNumber, micros);
							if (drums) {
								if (noteEvents.isEmpty())
									instruments.clear();
								drumEvents.add(ne);
								drumsOn.add(ne);
							}
							else {
								noteEvents.add(ne);
								notesOn.add(ne);
							}
						}
					}
					break;

				case ShortMessage.NOTE_OFF:
					Iterator<NoteEvent> iter = drums ? drumsOn.iterator() : notesOn.iterator();
					while (iter.hasNext()) {
						NoteEvent ne = iter.next();
						if (ne.note.id == m.getData1()) {
							iter.remove();
							ne.endMicros = MidiUtils.tick2microsecond(song, evt.getTick(), tempoCache);
							break;
						}
					}
					break;

				case ShortMessage.PROGRAM_CHANGE:
					if (!drums) {
						if (noteEvents.isEmpty())
							instruments.clear();

						int instrument = m.getData1();
						if (!instruments.contains(instrument)) {
							instruments.add(instrument);
						}
					}
					break;
				}
			}
			else if (msg instanceof MetaMessage) {
				MetaMessage m = (MetaMessage) msg;

				switch (m.getType()) {
				case META_TRACK_NAME:
					try {
						byte[] data = m.getData();
						String nameTmp = new String(data, 0, data.length, "US-ASCII").trim();
						if (name != null)
							name += " " + nameTmp;
						else
							name = nameTmp;
					}
					catch (UnsupportedEncodingException ex) {
						throw new RuntimeException(ex);
					}
					break;

				case META_KEY_SIGNATURE:
					if (keySignature == null)
						keySignature = new KeySignature(m);
					break;

				case META_TIME_SIGNATURE:
					if (timeSignature == null)
						timeSignature = new TimeSignature(m);
					break;
				}
			}
		}

		// Turn off notes that are on at the end of the song.  This shouldn't happen...
		for (NoteEvent ne : notesOn)
			ne.endMicros = song.getMicrosecondLength();

		for (NoteEvent ne : drumsOn)
			ne.endMicros = song.getMicrosecondLength();

		noteEvents = Collections.unmodifiableList(noteEvents);
		drumEvents = Collections.unmodifiableList(drumEvents);
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
			if (hasDrums())
				return "Drums";
			else if (hasNotes())
				return MIDI_INSTRUMENTS[0];
			else
				return "<None>";
		}

		String names = getInstrumentName(instruments.get(0));
		for (int i = 1; i < instruments.size(); i++) {
			names += ", " + getInstrumentName(instruments.get(i));
		}

		if (hasDrums())
			names = "Drums, " + names;

		return names;
	}

	public static String getInstrumentName(int id) {
		if (id < 0 || id >= MIDI_INSTRUMENTS.length) {
			return "Unknown";
		}
		return MIDI_INSTRUMENTS[id];
	}
}
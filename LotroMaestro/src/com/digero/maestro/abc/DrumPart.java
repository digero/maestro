package com.digero.maestro.abc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.digero.maestro.midi.Note;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequenceInfo;

public class DrumPart extends AbcPart {
	private Map<TrackNoteId, Integer> noteIdMap = new HashMap<TrackNoteId, Integer>();

	public DrumPart(SequenceInfo sequenceInfo, int partNumber) {
		this(sequenceInfo, partNumber, sequenceInfo.getTitle() + " - " + LotroInstrument.DRUMS.toString(), true);
	}

	public DrumPart(SequenceInfo sequenceInfo, int partNumber, String title, boolean enabled) {
		super(sequenceInfo, 0, partNumber, title, LotroInstrument.DRUMS, enabled);
	}

	public void setNoteMapping(int track, int srcNote, int dstNote) {
		if (getNoteMapping(track, srcNote) != dstNote) {
			noteIdMap.put(new TrackNoteId(track, srcNote), dstNote);
			fireChangeEvent();
		}
	}

	public int getNoteMapping(int track, int srcNote) {
		Integer dstNote = noteIdMap.get(new TrackNoteId(track, srcNote));
		return (dstNote == null) ? 0 : dstNote;
	}

	@Override
	protected Note mapNote(int track, int noteId) {
		int dstNote = getNoteMapping(track, noteId);
		return (dstNote == 0) ? null : Note.fromId(dstNote);
	}

	@Override
	protected List<NoteEvent> getTrackEvents(int track) {
		return getSequenceInfo().getTrackInfo(track).getDrumEvents();
	}

	@Override
	public void setBaseTranspose(int baseTranspose) {
		// Ignore
	}

	@Override
	public int getBaseTranspose() {
		return 0;
	}

	@Override
	public void setTrackTranspose(int track, int transpose) {
		// Ignore
	}

	@Override
	public int getTrackTranspose(int track) {
		return 0;
	}

	@Override
	public LotroInstrument[] getSupportedInstruments() {
		return new LotroInstrument[] {
			LotroInstrument.DRUMS
		};
	}

	private static class TrackNoteId {
		public final int track;
		public final int noteId;

		public TrackNoteId(int track, int noteId) {
			this.track = track;
			this.noteId = noteId;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TrackNoteId) {
				TrackNoteId that = (TrackNoteId) obj;
				return this.track == that.track && this.noteId == that.noteId;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return (track << 16) ^ noteId;
		}

		@Override
		public String toString() {
			return track + ":" + noteId;
		}
	}
}

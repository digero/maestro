package com.digero.maestro.midi;

import javax.sound.midi.MidiUnavailableException;

import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;

public class NoteFilterSequencerWrapper extends SequencerWrapper {
	private NoteFilterTransceiver filter;

	public NoteFilterSequencerWrapper() throws MidiUnavailableException {
	}

	@Override
	protected void connectTransmitter() {
		if (filter == null)
			filter = new NoteFilterTransceiver();
		transmitter.setReceiver(filter);
		filter.setReceiver(receiver);
	}

	public NoteFilterTransceiver getFilter() {
		return filter;
	}

	public void setNoteSolo(int track, int noteId, boolean solo) {
		if (filter != null && solo != getNoteSolo(track, noteId)) {
			sequencer.setTrackSolo(track, solo);
			filter.setNoteSolo(noteId, solo);
			fireChangeEvent(SequencerProperty.TRACK_ACTIVE);
		}
	}

	public boolean getNoteSolo(int track, int noteId) {
		if (filter == null)
			return false;

		return filter.getNoteSolo(noteId) && sequencer.getTrackSolo(track);
	}

	@Override
	public boolean isNoteActive(int noteId) {
		return filter == null || filter.isNoteActive(noteId);
	}
}

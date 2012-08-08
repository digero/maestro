package com.digero.maestro.midi;

import java.util.List;

import javax.sound.midi.MidiUnavailableException;

import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.midi.Transceiver;
import com.digero.common.midi.VolumeTransceiver;

public class NoteFilterSequencerWrapper extends SequencerWrapper {
	private NoteFilterTransceiver filter;

	public NoteFilterSequencerWrapper() throws MidiUnavailableException {
	}

	public NoteFilterSequencerWrapper(VolumeTransceiver volumeTransceiver) throws MidiUnavailableException {
		super(volumeTransceiver);
	}

	@Override
	protected List<Transceiver> getTransceivers() {
		if (filter == null)
			filter = new NoteFilterTransceiver();

		List<Transceiver> transceivers = super.getTransceivers();
		transceivers.add(0, filter);
		return transceivers;
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

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

	public void setNoteSolo(int track, int drumId, boolean solo) {
		if (filter != null && solo != getNoteSolo(track, drumId)) {
			sequencer.setTrackSolo(track, solo);
			filter.setNoteSolo(drumId, solo);
			fireChangeEvent(SequencerProperty.TRACK_ACTIVE);
		}
	}

	public boolean getNoteSolo(int track, int drumId) {
		if (filter == null)
			return false;

		return filter.getNoteSolo(drumId) && sequencer.getTrackSolo(track);
	}

	public boolean isNoteActive(int track, int drumId) {
		return isTrackActive(track) && (filter == null || filter.isNoteActive(drumId));
	}
}

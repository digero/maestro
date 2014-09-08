package com.digero.common.midi;

import javax.sound.midi.MidiUnavailableException;

import com.digero.common.midi.SequencerEvent.SequencerProperty;

public class NoteFilterSequencerWrapper extends SequencerWrapper
{
	private NoteFilterTransceiver filter;

	public NoteFilterSequencerWrapper() throws MidiUnavailableException
	{
		filter = new NoteFilterTransceiver();
		addTransceiver(filter);
	}

	public NoteFilterTransceiver getFilter()
	{
		return filter;
	}

	public void setNoteSolo(int track, int noteId, boolean solo)
	{
		if (solo != getNoteSolo(track, noteId))
		{
			sequencer.setTrackSolo(track, solo);
			filter.setNoteSolo(noteId, solo);
			fireChangeEvent(SequencerProperty.TRACK_ACTIVE);
		}
	}

	public boolean getNoteSolo(int track, int noteId)
	{
		return filter.getNoteSolo(noteId) && sequencer.getTrackSolo(track);
	}

	@Override public boolean isNoteActive(int noteId)
	{
		return filter.isNoteActive(noteId);
	}
}

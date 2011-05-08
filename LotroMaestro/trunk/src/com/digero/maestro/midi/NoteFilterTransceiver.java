package com.digero.maestro.midi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

import com.digero.common.midi.IMidiConstants;
import com.digero.common.midi.MidiFactory;
import com.digero.common.util.ICompileConstants;
import com.digero.maestro.abc.AbcPart;

public class NoteFilterTransceiver implements Transmitter, Receiver, IMidiConstants, ICompileConstants {
	private Receiver receiver = null;
	private AbcPart abcPart = null;
	private Set<Integer>[] notesOn;
	private Set<Integer> solos = new HashSet<Integer>();

	@SuppressWarnings("unchecked")
	public void setAbcPart(AbcPart activePart) {
		this.abcPart = activePart;
		notesOn = new Set[CHANNEL_COUNT];
	}

	public AbcPart getAbcPart() {
		return abcPart;
	}

	public void setNoteSolo(int drumId, boolean solo) {
		if (solo) {
			solos.add(drumId);
			turnOffInactiveNotes();
		}
		else {
			solos.remove(drumId);
		}
	}

	public boolean getNoteSolo(int noteId) {
		return solos.contains(noteId);
	}

	public boolean isNoteActive(int noteId) {
		return (!solos.isEmpty()) ? solos.contains(noteId) : true;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public Receiver getReceiver() {
		return receiver;
	}

	@Override
	public void setReceiver(Receiver receiver) {
		this.receiver = receiver;
	}

	private boolean isNoteOn(int channel, int noteId) {
		if (notesOn[channel] == null)
			return false;

		return notesOn[channel].contains(noteId);
	}

	private void setNoteOn(int channel, int noteId, boolean on) {
		if (notesOn[channel] == null)
			notesOn[channel] = new HashSet<Integer>();

		if (on)
			notesOn[channel].add(noteId);
		else
			notesOn[channel].remove(noteId);
	}

	private void turnOffInactiveNotes() {
		if (receiver == null)
			return;

		for (int c = 0; c < CHANNEL_COUNT; c++) {
			if (notesOn[c] == null)
				continue;

			Iterator<Integer> iter = notesOn[c].iterator();
			while (iter.hasNext()) {
				int noteId = iter.next();
				if (!isNoteActive(noteId)) {
					iter.remove();
					MidiEvent evt = MidiFactory.createNoteOffEvent(noteId, c, -1);
					receiver.send(evt.getMessage(), evt.getTick());
				}
			}
		}
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (receiver == null)
			return;

		if (abcPart != null && message instanceof ShortMessage) {
			ShortMessage m = (ShortMessage) message;
			int c = m.getChannel();
			int cmd = m.getCommand();
			int noteId = m.getData1();
			int speed = m.getData2();
			boolean noteOnMsg = (cmd == ShortMessage.NOTE_ON) && speed > 0;
			boolean noteOffMsg = (cmd == ShortMessage.NOTE_OFF) || (cmd == ShortMessage.NOTE_ON && speed == 0);

			if (!isNoteActive(noteId) && !(isNoteOn(c, noteId) && noteOffMsg))
				return;

			if (noteOnMsg)
				setNoteOn(c, noteId, true);
			else if (noteOffMsg)
				setNoteOn(c, noteId, false);
		}

		receiver.send(message, timeStamp);
	}
}

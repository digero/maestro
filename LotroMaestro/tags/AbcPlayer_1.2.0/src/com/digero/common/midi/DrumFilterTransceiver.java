package com.digero.common.midi;

import java.util.HashSet;
import java.util.Set;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

import com.digero.common.util.ICompileConstants;
import com.digero.maestro.abc.AbcPart;

public class DrumFilterTransceiver implements Transmitter, Receiver, IMidiConstants, ICompileConstants {
	private Receiver receiver = null;
	private AbcPart abcPart = null;
	private Set<Integer> drumsOn = new HashSet<Integer>();
	private Set<Integer> drumSolos = new HashSet<Integer>();

	public void setAbcPart(AbcPart activePart) {
		this.abcPart = activePart;
	}

	public AbcPart getAbcPart() {
		return abcPart;
	}

	public void setDrumSolo(int drumId, boolean solo) {
		if (solo)
			drumSolos.add(drumId);
		else
			drumSolos.remove(drumId);
	}

	public boolean getDrumSolo(int drumId) {
		return drumSolos.contains(drumId);
	}

	public boolean isDrumActive(int drumId) {
//		if (MUTE_DISABLED_TRACKS)
//			return (!drumSolos.isEmpty()) ? drumSolos.contains(drumId) : abcPart.isDrumEnabled(track, drumId);
//		else
			return (!drumSolos.isEmpty()) ? drumSolos.contains(drumId) : true;
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

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (receiver == null)
			return;

		if (abcPart != null && message instanceof ShortMessage) {
			ShortMessage m = (ShortMessage) message;
			if (m.getChannel() == DRUM_CHANNEL) {
				int cmd = m.getCommand();
				int noteId = m.getData1();
				int speed = m.getData2();
				boolean isNoteOn = (cmd == ShortMessage.NOTE_ON) && speed > 0;
				boolean isNoteOff = (cmd == ShortMessage.NOTE_OFF) || (cmd == ShortMessage.NOTE_ON && speed == 0);

				if (!isDrumActive(noteId) && !(drumsOn.contains(noteId) && isNoteOff))
					return;

				if (isNoteOn)
					drumsOn.add(noteId);
				else if (isNoteOff)
					drumsOn.remove(noteId);
			}
		}

		receiver.send(message, timeStamp);
	}
}

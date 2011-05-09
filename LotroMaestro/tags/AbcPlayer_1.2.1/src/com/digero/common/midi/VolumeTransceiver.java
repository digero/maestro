package com.digero.common.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

public class VolumeTransceiver implements Transmitter, Receiver {
	public static final int MAX_VOLUME = 127;

	private Receiver receiver;
	private int volume = MAX_VOLUME;

	public void setVolume(int volume) {
		if (volume < 0 || volume > MAX_VOLUME)
			throw new IllegalArgumentException();
		this.volume = volume;
	}

	public int getVolume() {
		return volume;
	}

	@Override
	public void close() {
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
		if (receiver != null) {
			if (message instanceof ShortMessage) {
				ShortMessage m = (ShortMessage) message;
				int cmd = m.getCommand();
				if (cmd == ShortMessage.NOTE_ON || cmd == ShortMessage.NOTE_OFF || cmd == ShortMessage.POLY_PRESSURE
						|| cmd == ShortMessage.CHANNEL_PRESSURE) {
					try {
						m.setMessage(cmd, m.getChannel(), m.getData1(), m.getData2() * volume / MAX_VOLUME);
					}
					catch (InvalidMidiDataException e) {
						e.printStackTrace();
					}
				}
			}

			receiver.send(message, timeStamp);
		}
	}
}

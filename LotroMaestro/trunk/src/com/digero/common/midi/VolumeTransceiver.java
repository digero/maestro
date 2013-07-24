package com.digero.common.midi;

import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class VolumeTransceiver implements Transceiver, IMidiConstants {
	public static final int MAX_VOLUME = 127;

	private Receiver receiver;
	private int volume = MAX_VOLUME;
	private int[] channelVolume = new int[CHANNEL_COUNT];

	public VolumeTransceiver() {
		Arrays.fill(channelVolume, MAX_VOLUME);
	}

	public void setVolume(int volume) {
		if (volume < 0 || volume > MAX_VOLUME)
			throw new IllegalArgumentException();

		this.volume = volume;
		sendVolumeAllChannels();
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
		sendVolumeAllChannels();
	}

	private void sendVolumeAllChannels() {
		if (receiver != null) {
			for (int c = 0; c < CHANNEL_COUNT; c++) {
				int actualVolume = channelVolume[c] * volume / MAX_VOLUME;
				MidiEvent evt = MidiFactory.createChannelVolumeEvent(actualVolume, c, 0);
				receiver.send(evt.getMessage(), 0);
			}
		}
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		boolean systemReset = false;
		if (message instanceof ShortMessage) {
			ShortMessage m = (ShortMessage) message;
			if (m.getCommand() == ShortMessage.SYSTEM_RESET) {
				Arrays.fill(channelVolume, MAX_VOLUME);
				systemReset = true;
			}
			else if (m.getCommand() == ShortMessage.CONTROL_CHANGE && m.getData1() == CHANNEL_VOLUME_CONTROLLER_COARSE) {
				try {
					int c = m.getChannel();
					channelVolume[c] = m.getData2();
					int actualVolume = channelVolume[c] * volume / MAX_VOLUME;
					m.setMessage(m.getCommand(), c, CHANNEL_VOLUME_CONTROLLER_COARSE, actualVolume);
				}
				catch (InvalidMidiDataException e) {
					e.printStackTrace();
				}
			}
		}

		if (receiver != null) {
			receiver.send(message, timeStamp);
			if (systemReset)
				sendVolumeAllChannels();
		}
	}
}

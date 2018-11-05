package com.digero.common.midi;

import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class VolumeTransceiver implements Transceiver, MidiConstants
{
	private static final int UNSET_CHANNEL_VOLUME = -1;

	private Receiver receiver;
	private int volume = MAX_VOLUME;
	private int[] channelVolume = new int[CHANNEL_COUNT];
	private boolean goesToEleven = false;

	public VolumeTransceiver()
	{
		Arrays.fill(channelVolume, UNSET_CHANNEL_VOLUME);
	}

	public void itGoesToEleven(boolean goesToEleven)
	{
		if (this.goesToEleven != goesToEleven)
		{
			this.goesToEleven = goesToEleven;
			sendVolumeAllChannels();
		}
	}

	public void setVolume(int volume)
	{
		if (volume < 0 || volume > MAX_VOLUME)
			throw new IllegalArgumentException();

		this.volume = volume;
		sendVolumeAllChannels();
	}

	public int getVolume()
	{
		return volume;
	}

	@Override public void close()
	{
	}

	@Override public Receiver getReceiver()
	{
		return receiver;
	}

	@Override public void setReceiver(Receiver receiver)
	{
		this.receiver = receiver;
		sendVolumeAllChannels();
	}

	private int getActualVolume(int channel)
	{
		int controllerVolume = channelVolume[channel];
		if (controllerVolume == UNSET_CHANNEL_VOLUME)
			controllerVolume = goesToEleven ? MAX_VOLUME : DEFAULT_CHANNEL_VOLUME;

		return controllerVolume * volume / MAX_VOLUME;
	}

	private void sendVolumeAllChannels()
	{
		if (receiver != null)
		{
			for (int c = 0; c < CHANNEL_COUNT; c++)
			{
				MidiEvent evt = MidiFactory.createChannelVolumeEvent(getActualVolume(c), c, 0);
				receiver.send(evt.getMessage(), -1);
			}
		}
	}

	@Override public void send(MidiMessage message, long timeStamp)
	{
		boolean systemReset = false;
		if (message instanceof ShortMessage)
		{
			ShortMessage m = (ShortMessage) message;
			if (m.getCommand() == ShortMessage.SYSTEM_RESET)
			{
				Arrays.fill(channelVolume, UNSET_CHANNEL_VOLUME);
				systemReset = true;
			}
			else if (m.getCommand() == ShortMessage.CONTROL_CHANGE && m.getData1() == CHANNEL_VOLUME_CONTROLLER_COARSE)
			{
				try
				{
					int c = m.getChannel();
					channelVolume[c] = m.getData2();
					m.setMessage(m.getCommand(), c, CHANNEL_VOLUME_CONTROLLER_COARSE, getActualVolume(c));
				}
				catch (InvalidMidiDataException e)
				{
					e.printStackTrace();
				}
			}
		}

		if (receiver != null)
		{
			receiver.send(message, timeStamp);
			if (systemReset)
				sendVolumeAllChannels();
		}
	}
}

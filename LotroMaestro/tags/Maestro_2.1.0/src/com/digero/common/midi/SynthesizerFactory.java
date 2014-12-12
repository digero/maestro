package com.digero.common.midi;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import com.sun.media.sound.AudioSynthesizer;

public class SynthesizerFactory
{
	private static Soundbank lotroSoundbank = null;
	private static File soundFontFile = new File("LotroInstruments.sf2");

	public static void setSoundFontLocation(File soundFontFile)
	{
		if (SynthesizerFactory.soundFontFile != soundFontFile)
		{
			SynthesizerFactory.soundFontFile = soundFontFile;
			lotroSoundbank = null;
		}
	}

	public static Synthesizer getLotroSynthesizer() throws MidiUnavailableException, InvalidMidiDataException,
			IOException
	{
		Synthesizer synth = MidiSystem.getSynthesizer();
		if (synth != null)
			initLotroSynthesizer(synth);
		return synth;
	}

	public static AudioSynthesizer getLotroAudioSynthesizer() throws MidiUnavailableException,
			InvalidMidiDataException, IOException
	{
		AudioSynthesizer synth = findAudioSynthesizer();
		if (synth != null)
			initLotroSynthesizer(synth);
		return synth;
	}

	public static void initLotroSynthesizer(Synthesizer synth) throws MidiUnavailableException,
			InvalidMidiDataException, IOException
	{
		synth.open();
		synth.unloadAllInstruments(getLotroSoundbank());
		synth.loadAllInstruments(getLotroSoundbank());
	}

	public static Soundbank getLotroSoundbank() throws InvalidMidiDataException, IOException
	{
		if (lotroSoundbank == null)
		{
			try
			{
				lotroSoundbank = MidiSystem.getSoundbank(soundFontFile);
			}
			catch (NullPointerException npe)
			{
				// JARSoundbankReader throws a NullPointerException if the file doesn't exist
				StackTraceElement trace = npe.getStackTrace()[0];
				if (trace.getClassName().equals("com.sun.media.sound.JARSoundbankReader")
						&& trace.getMethodName().equals("isZIP"))
				{
					throw new IOException("Soundbank file not found");
				}
				else
				{
					throw npe;
				}
			}
		}
		return lotroSoundbank;
	}

	/**
	 * Find available AudioSynthesizer
	 */
	public static AudioSynthesizer findAudioSynthesizer() throws MidiUnavailableException
	{
		// First check if default synthesizer is AudioSynthesizer.
		Synthesizer synth = MidiSystem.getSynthesizer();
		if (synth instanceof AudioSynthesizer)
			return (AudioSynthesizer) synth;

		// If default synhtesizer is not AudioSynthesizer, check others.
		for (Info info : MidiSystem.getMidiDeviceInfo())
		{
			MidiDevice dev = MidiSystem.getMidiDevice(info);
			if (dev instanceof AudioSynthesizer)
				return (AudioSynthesizer) dev;
		}

		// No AudioSynthesizer was found, return null.
		return null;
	}
}

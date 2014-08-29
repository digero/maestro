package com.digero.common.midi;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;

public class LotroSequencerWrapper extends NoteFilterSequencerWrapper
{
	private static Synthesizer lotroSynth;
	private static String loadLotroSynthError;

	static
	{
		try
		{
			lotroSynth = SynthesizerFactory.getLotroSynthesizer();
		}
		catch (InvalidMidiDataException e)
		{
			loadLotroSynthError = e.getMessage();
		}
		catch (IOException e)
		{
			loadLotroSynthError = e.getMessage();
		}
		catch (MidiUnavailableException e)
		{
			loadLotroSynthError = e.getMessage();
		}
	}

	public static String getLoadLotroSynthError()
	{
		return loadLotroSynthError;
	}

	public LotroSequencerWrapper() throws MidiUnavailableException
	{
	}

	public boolean isUsingLotroInstruments()
	{
		return lotroSynth != null;
	}

	@Override protected Receiver createReceiver() throws MidiUnavailableException
	{
		return (lotroSynth != null) ? lotroSynth.getReceiver() : MidiSystem.getReceiver();
	}
}

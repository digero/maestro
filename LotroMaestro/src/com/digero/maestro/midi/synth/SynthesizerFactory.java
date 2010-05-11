package com.digero.maestro.midi.synth;

import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

public class SynthesizerFactory {
	private static Soundbank lotroSoundbank;
	private static Soundbank lotroDrumbank;

	public static Synthesizer getLotroSynthesizer() throws MidiUnavailableException, InvalidMidiDataException,
			IOException {
		if (lotroSoundbank == null) {
			try {
				lotroSoundbank = MidiSystem.getSoundbank(SynthesizerFactory.class.getResource("LotroInstruments.sf2"));
				lotroDrumbank = MidiSystem.getSoundbank(SynthesizerFactory.class.getResource("LotroDrums.sf2"));
			}
			catch (NullPointerException npe) {
				// JARSoundbankReader throws a NullPointerException if the file doesn't exist
				StackTraceElement trace = npe.getStackTrace()[0];
				if (trace.getClassName().equals("com.sun.media.sound.JARSoundbankReader")
						&& trace.getMethodName().equals("isZIP")) {
					throw new IOException("Soundbank file not found");
				}
				else {
					throw npe;
				}
			}
		}
		Synthesizer synth = MidiSystem.getSynthesizer();
		synth.open();
		synth.unloadAllInstruments(lotroSoundbank);
		synth.loadAllInstruments(lotroSoundbank);
		synth.unloadAllInstruments(lotroDrumbank);
		synth.loadAllInstruments(lotroDrumbank);
		return synth;
	}
}

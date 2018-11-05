package com.digero.common.midi;

public interface MidiConstants
{
	public static final int META_TEXT = 0x01;
	public static final int META_COPYRIGHT = 0x02;
	public static final int META_TRACK_NAME = 0x03;
	public static final int META_INSTRUMENT = 0x04;
	public static final int META_PROGRAM_NAME = 0x08;
	public static final int META_END_OF_TRACK = 0x2F;
	public static final int META_TEMPO = 0x51;
	public static final int META_TIME_SIGNATURE = 0x58;
	public static final int META_KEY_SIGNATURE = 0x59;

	public static final byte CHANNEL_VOLUME_CONTROLLER_COARSE = 0x07;
	public static final int ALL_CONTROLLERS_OFF = 0x79;
	public static final int REGISTERED_PARAMETER_NUMBER_MSB = 0x65;
	public static final int REGISTERED_PARAMETER_NUMBER_LSB = 0x64;
	public static final int DATA_ENTRY_COARSE = 0x06;
	public static final int DATA_ENTRY_FINE = 0x26;
	public static final int REGISTERED_PARAM_PITCH_BEND_RANGE = 0x0000;
	public static final int REGISTERED_PARAM_NONE = 0x3FFF;

	public static final int DRUM_CHANNEL = 9;
	public static final int CHANNEL_COUNT = 16;
	public static final int LOWEST_NOTE_ID = 0;
	public static final int HIGHEST_NOTE_ID = 127;
	public static final int NOTE_COUNT = HIGHEST_NOTE_ID - LOWEST_NOTE_ID + 1;
	public static final int MAX_VOLUME = 127;

	public static final byte PAN_CONTROL = 0x0A;
	public static final byte REVERB_CONTROL = 0x5B;
	public static final byte TREMOLO_CONTROL = 0x5C;
	public static final byte CHORUS_CONTROL = 0x5D;
	public static final byte DETUNE_CONTROL = 0x5E;
	public static final byte PHASER_CONTROL = 0x5F;

	public static final int DEFAULT_TEMPO_BPM = 120;
	public static final int DEFAULT_TEMPO_MPQ = 500000;
	public static final int DEFAULT_INSTRUMENT = 0;
	public static final int DEFAULT_CHANNEL_VOLUME = 100;
	public static final int DEFAULT_PITCH_BEND_RANGE_SEMITONES = 2;
	public static final int DEFAULT_PITCH_BEND_RANGE_CENTS = 0;
}

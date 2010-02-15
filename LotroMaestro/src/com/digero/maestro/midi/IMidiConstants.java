package com.digero.maestro.midi;

public interface IMidiConstants {
	public static final int META_TEXT = 0x01;
	public static final int META_COPYRIGHT = 0x02;
	public static final int META_TRACK_NAME = 0x03;
	public static final int META_INSTRUMENT = 0x04;
	public static final int META_PROGRAM_NAME = 0x08;
	public static final int META_END_OF_TRACK = 0x2F;
	public static final int META_TEMPO = 0x51;
	public static final int META_TIME_SIGNATURE = 0x58;
	public static final int META_KEY_SIGNATURE = 0x59;

	public static final int DRUM_CHANNEL = 9;
	public static final int CHANNEL_COUNT = 16;
}

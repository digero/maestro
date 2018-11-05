package com.digero.common.midi;

public enum MidiDrum
{
	SYNTH_ZAP("Synth Zap"), // 27
	UNKNOWN_28("Unknown"), // 28
	SCRATCH_1("Scratch 1"), // 29
	SCRATCH_2("Scratch 2"), // 30
	DRUM_STICKS("Drum Sticks"), // 31
	UNKNOWN_32("Unknown"), // 32
	METR_CLICK("Metr. Click"), // 33
	METR_BELL("Metr. Bell"), // 34
	ACOU_BASS("Acou. Bass"), // 35
	BASS_DRUM("Bass Drum"), // 36
	RIM_SHOT("Rim Shot"), // 37
	ACOU_SNARE("Acou. Snare"), // 38
	HAND_CLAP("Hand Clap"), // 39
	ELEC_SNARE("Elec. Snare"), // 40
	LOW_TOM_A("Low Tom A"), // 41
	CLOSED_HI_HAT("Closed Hi-Hat"), // 42
	LOW_TOM_B("Low Tom B"), // 43
	PEDAL_HI_HAT("Pedal Hi-Hat"), // 44
	MID_TOM_A("Mid Tom A"), // 45
	OPEN_HI_HAT("Open Hi-Hat"), // 46
	MID_TOM_B("Mid Tom B"), // 47
	HIGH_TOM_A("High Tom A"), // 48
	CRASH_CYM_1("Crash Cym. 1"), // 49
	HIGH_TOM_B("High Tom B"), // 50
	RIDE_CYM_1("Ride Cym. 1"), // 51
	CHINESE_CYM("Chinese Cym."), // 52
	RIDE_BELL("Ride Bell"), // 53
	TAMBOURINE("Tambourine"), // 54
	SPLASH_CYM("Splash Cym."), // 55
	COWBELL("Cowbell"), // 56
	CRASH_CYM_2("Crash Cym. 2"), // 57
	VIBRASLAP("Vibraslap"), // 58
	RIDE_CYM_2("Ride Cym. 2"), // 59
	HI_BONGO("Hi Bongo"), // 60
	LOW_BONGO("Low Bongo"), // 61
	MUTE_HI_CONGA("Mute Hi Conga"), // 62
	OPEN_HI_CONGA("Open Hi Conga"), // 63
	LOW_CONGA("Low Conga"), // 64
	HIGH_TIMBALE("High Timbale"), // 65
	LOW_TIMBALE("Low Timbale"), // 66
	HIGH_AGOGO("High Agogo"), // 67
	LOW_AGOGO("Low Agogo"), // 68
	CABASA("Cabasa"), // 69
	MARACAS("Maracas"), // 70
	SHORT_WHISTLE("Short Whistle"), // 71
	LONG_WHISTLE("Long Whistle"), // 72
	SHORT_GUIRO("Short Guiro"), // 73
	LONG_GUIRO("Long Guiro"), // 74
	CLAVES("Claves"), // 75
	HIGH_BLOCK("High Block"), // 76
	LOW_BLOCK("Low Block"), // 77
	MUTE_CUICA("Mute Cuica"), // 78
	OPEN_CUICA("Open Cuica"), // 79
	MUTE_TRIANGLE("Mute Triangle"), // 80
	OPEN_TRIANGLE("Open Triangle"), // 81
	CABASA_2("Cabasa 2"), // 82
	BELLS("Bells"), // 83
	CHIMES("Chimes"), // 84
	CASTANET("Castanet"), // 85
	MUTED_LARGE_DRUM("Muted Lg Drum"), // 86
	LARGE_DRUM("Large Drum"), // 87
	INVALID("Unknown");

	private static final MidiDrum[] values = values();
	public static final int DRUM_ID_OFFSET = 27;

	public static MidiDrum fromId(int id)
	{
		id -= DRUM_ID_OFFSET;
		if (id < 0 || id >= values.length)
			return INVALID;

		return values[id];
	}

	public final String name;

	private MidiDrum(String name)
	{
		this.name = name;
	}

	public int id()
	{
		return ordinal() + DRUM_ID_OFFSET;
	}

	@Override public String toString()
	{
		return name;
	}
}

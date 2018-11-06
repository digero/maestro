package com.digero.common.midi;

public enum MidiInstrument
{
	PIANO("Piano"), // 0
	BRIGHT_PIANO("Bright Piano"), // 1
	ELEC_PIANO("Elec Piano"), // 2
	HONKY_TONK_PIANO("Honky-tonk Piano"), // 3
	RHODES_PIANO("Rhodes Piano"), // 4
	CHORUS_PIANO("Chorus Piano"), // 5
	HARPSCHORD("Harpschord"), // 6
	CLAVINET("Clavinet"), // 7
	CELESTA("Celesta"), // 8
	GLOCKENSPIEL("Glockenspiel"), // 9
	MUSIC_BOX("Music Box"), // 10
	VIBRAPHONE("Vibraphone"), // 11
	MARIMBA("Marimba"), // 12
	XYLOPHONE("Xylophone"), // 13
	TUBULAR_BELLS("Tubular Bells"), // 14
	DULCIMER("Dulcimer"), // 15
	HAMMOND_ORGAN("Hammond Organ"), // 16
	PERC_ORGAN("Perc Organ"), // 17
	ROCK_ORGAN("Rock Organ"), // 18
	CHURCH_ORGAN("Church Organ"), // 19
	REED_ORGAN("Reed Organ"), // 20
	ACCORDION("Accordion"), // 21
	HARMONICA("Harmonica"), // 22
	TANGO_ACORDN("Tango Acordn"), // 23
	NYLON_GUITAR("Nylon Guitar"), // 24
	STEEL_STRING_GUITAR("Steel String Guitar"), // 25
	JAZZ_GUITAR("Jazz Guitar"), // 26
	CLEAN_ELEC_GUITAR("Clean Elec. Guitar"), // 27
	MUTE_ELEC_GUITAR("Mute Elec. Guitar"), // 28
	OVRDRIVE_GUITAR("Ovrdrive Guitar"), // 29
	DISTORTED_GUITAR("Distorted Guitar"), // 30
	HARMONICS("Harmonics"), // 31
	ACOUSTIC_BASS("Acoustic Bass"), // 32
	FINGERED_ELEC_BASS("Fingered Elec. Bass"), // 33
	PICKED_ELEC_BASS("Picked Elec. Bass"), // 34
	FRETLESS_BASS("Fretless Bass"), // 35
	SLAP_BASS_1("Slap Bass 1"), // 36
	SLAP_BASS_2("Slap Bass 2"), // 37
	SYNTH_BASS_1("Synth Bass 1"), // 38
	SYNTH_BASS_2("Synth Bass 2"), // 39
	VIOLIN("Violin"), // 40
	VIOLA("Viola"), // 41
	CELLO("Cello"), // 42
	CONTRABASS("Contrabass"), // 43
	TREMOLO_STRINGS("Tremolo Strings"), // 44
	PIZZICATO_STRINGS("Pizzicato Strings"), // 45
	ORCHESTRA_HARP("Orchestra Harp"), // 46
	TIMPANI("Timpani"), // 47
	STRING_ENSEMBLE_1("String Ensemble 1"), // 48
	STRING_ENSEMBLE_2("String Ensemble 2"), // 49
	SYNTH_STRING_1("Synth String 1"), // 50
	SYNTH_STRING_2("Synth String 2"), // 51
	CHOIR_AAHS("Choir Aahs"), // 52
	VOICE_OOHS("Voice Oohs"), // 53
	SYNTH_VOICE("Synth Voice"), // 54
	ORCHESTRA_HIT("Orchestra Hit"), // 55
	TRUMPET("Trumpet"), // 56
	TROMBONE("Trombone"), // 57
	TUBA("Tuba"), // 58
	MUTE_TRUMPET("Mute Trumpet"), // 59
	FRENCH_HORN("French Horn"), // 60
	BRASS_SECTION("Brass Section"), // 61
	SYNTH_BRASS_1("Synth Brass 1"), // 62
	SYNTH_BRASS_2("Synth Brass 2"), // 63
	SOPRANO_SAX("Soprano Sax"), // 64
	ALTO_SAX("Alto Sax"), // 65
	TENOR_SAX("Tenor Sax"), // 66
	BARI_SAX("Bari Sax"), // 67
	OBOE("Oboe"), // 68
	ENGLISH_HORN("English Horn"), // 69
	BASSOON("Bassoon"), // 70
	CLARINET("Clarinet"), // 71
	PICCOLO("Piccolo"), // 72
	FLUTE("Flute"), // 73
	RECORDER("Recorder"), // 74
	PAN_FLUTE("Pan Flute"), // 75
	BOTTLE_BLOW("Bottle Blow"), // 76
	SHAKUHACHI("Shakuhachi"), // 77
	WHISTLE("Whistle"), // 78
	OCARINA("Ocarina"), // 79
	SQUARE_WAVE("Square Wave"), // 80
	SAW_TOOTH("Saw Tooth"), // 81
	CALIOPE("Caliope"), // 82
	CHIFF_LEAD("Chiff Lead"), // 83
	CHARANG("Charang"), // 84
	SOLO_SYNTH_VOX("Solo Synth Vox"), // 85
	BRITE_SAW("Brite Saw"), // 86
	BRASS_AND_LEAD("Brass & Lead"), // 87
	FANTASA_PAD("Fantasa Pad"), // 88
	WARM_PAD("Warm Pad"), // 89
	POLY_SYNTH_PAD("Poly Synth Pad"), // 90
	SPACE_VOX_PAD("Space Vox Pad"), // 91
	BOW_GLASS_PAD("Bow Glass Pad"), // 92
	METAL_PAD("Metal Pad"), // 93
	HALO_PAD("Halo Pad"), // 94
	SWEEP_PAD("Sweep Pad"), // 95
	ICE_RAIN("Ice Rain"), // 96
	SOUND_TRACK("Sound Track"), // 97
	CRYSTAL("Crystal"), // 98
	ATMOSPHERE("Atmosphere"), // 99
	BRIGHTNESS("Brightness"), // 100
	GOBLIN("Goblin"), // 101
	ECHO_DROPS("Echo Drops"), // 102
	STAR_THEME("Star Theme"), // 103
	SITAR("Sitar"), // 104
	BANJO("Banjo"), // 105
	SHAMISEN("Shamisen"), // 106
	KOTO("Koto"), // 107
	KALIMBA("Kalimba"), // 108
	BAG_PIPE("Bag Pipe"), // 109
	FIDDLE("Fiddle"), // 110
	SHANAI("Shanai"), // 111
	TINKLE_BELL("Tinkle Bell"), // 112
	AGOGO("Agogo"), // 113
	STEEL_DRUMS("Steel Drums"), // 114
	WOODBLOCK("Woodblock"), // 115
	TAIKO_DRUM("Taiko Drum"), // 116
	MELODIC_TOM("Melodic Tom"), // 117
	SYNTH_DRUM("Synth Drum"), // 118
	REVERSE_CYMBAL("Reverse Cymbal"), // 119
	GUITAR_FRET_NOISE("Guitar Fret Noise"), // 120
	BREATH_NOISE("Breath Noise"), // 121
	SEA_SHORE("Sea Shore"), // 122
	BIRD_TWEET("Bird Tweet"), // 123
	TELEPHONE_RING("Telephone Ring"), // 124
	HELICOPTER("Helicopter"), // 125
	APPLAUSE("Applause"), // 126
	GUN_SHOT("Gun Shot"), // 127
	INVALID("Unknown");

	private static final MidiInstrument[] values = values();

	public static MidiInstrument fromId(int id)
	{
		if (id < 0 || id >= values.length)
			return INVALID;

		return values[id];
	}

	public final String name;

	private MidiInstrument(String name)
	{
		this.name = name;
	}

	public int id()
	{
		return ordinal();
	}

	@Override public String toString()
	{
		return name;
	}
}

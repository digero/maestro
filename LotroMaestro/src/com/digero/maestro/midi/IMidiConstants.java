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

	public static final String[] MIDI_INSTRUMENTS = {
			"Piano", // 0
			"Bright Piano", // 1 
			"Elec Piano", // 2
			"Honky-tonk Piano", // 3
			"Rhodes Piano", // 4
			"Chorus Piano", // 5
			"Harpschord", // 6
			"Clavinet", // 7
			"Celesta", // 8
			"Glockenspiel", // 9
			"Music Box", // 10
			"Vibraphone", // 11
			"Marimba", // 12
			"Xylophone", // 13
			"Tubular Bells", // 14
			"Dulcimer", // 15
			"Hammond Organ", // 16
			"Perc Organ", // 17
			"Rock Organ", // 18
			"Church Organ", // 19
			"Reed Organ", // 20
			"Accordion", // 21
			"Harmonica", // 22
			"Tango Acordn", // 23
			"Nylon Guitar", // 24
			"Steel String Guitar", // 25
			"Jazz Guitar", // 26
			"Clean Elec. Guitar", // 27
			"Mute Elec. Guitar", // 28
			"Ovrdrive Guitar", // 29
			"Distorted Guitar", // 30
			"Harmonics", // 31
			"Acoustic Bass", // 32
			"Fingered Elec. Bass", // 33
			"Picked Elec. Bass", // 34
			"Fretless Bass", // 35
			"Slap Bass 1", // 36
			"Slap Bass 2", // 37
			"Synth Bass 1", // 38
			"Synth Bass 2", // 39
			"Violin", // 40
			"Viola", // 41
			"Cello", // 42
			"Contrabass", // 43
			"Tremolo Strings", // 44
			"Pizzicato Strings", // 45
			"Orchestra Harp", // 46
			"Timpani", // 47
			"String Ensemble 1", // 48
			"String Ensemble 2", // 49
			"Synth String 1", // 50
			"Synth String 2", // 51
			"Choir Aahs", // 52
			"Voice Oohs", // 53
			"Synth Voice", // 54
			"Orchestra Hit", // 55
			"Trumpet", // 56
			"Trombone", // 57
			"Tuba", // 58
			"Mute Trumpet", // 59
			"French Horn", // 60
			"Brass Section", // 61
			"Synth Brass 1", // 62
			"Synth Brass 2", // 63
			"Soprano Sax", // 64
			"Alto Sax", // 65
			"Tenor Sax", // 66
			"Bari Sax", // 67
			"Oboe", // 68
			"Englsh Horn", // 69
			"Bassoon", // 70
			"Clarinet", // 71
			"Piccolo", // 72
			"Flute", // 73
			"Recorder", // 74
			"Pan Flute", // 75
			"Bottle Blow", // 76
			"Shakuhachi", // 77
			"Whistle", // 78
			"Ocarina", // 79
			"Square Wave", // 80
			"Saw Tooth", // 81
			"Caliope", // 82
			"Chiff Lead", // 83
			"Charang", // 84
			"Solo Synth Vox", // 85
			"Brite Saw", // 86
			"Brass & Lead", // 87
			"Fantasa Pad", // 88
			"Warm Pad", // 89
			"Poly Synth Pad", // 90
			"Space Vox Pad", // 91
			"Bow Glass Pad", // 92
			"Metal Pad", // 93
			"Halo Pad", // 94
			"Sweep Pad", // 95
			"Ice Rain", // 96
			"Sound Track", // 97
			"Crystal", // 98
			"Atmosphere", // 99
			"Brightness", // 100
			"Goblin", // 101
			"Echo Drops", // 102
			"Star Theme", // 103
			"Sitar", // 104
			"Banjo", // 105
			"Shamisen", // 106
			"Koto", // 107
			"Kalimba", // 108
			"Bag Pipe", // 109
			"Fiddle", // 110
			"Shanai", // 111
			"Tinkle Bell", // 112
			"Agogo", // 113
			"Steel Drums", // 114
			"Woodblock", // 115
			"Taiko Drum", // 116
			"Melodic Tom", // 117
			"Synth Drum", // 118
			"Revrse Cymbal", // 119
			"Guitar Fret Noise", // 120
			"Breath Noise", // 121
			"Sea Shore", // 122
			"Bird Tweet", // 123
			"Telephone Ring", // 124
			"Helicopter", // 125
			"Applause", // 126
			"Gun Shot", // 127
	};
}

/* Copyright (c) 2010 Ben Howell
 * This software is licensed under the MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package com.digero.common.abc;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.digero.common.midi.MidiInstrument;
import com.digero.common.midi.Note;
import com.digero.common.util.Pair;

// @formatter:off
public enum LotroInstrument
{
	//                         friendlyName               sustain  midi                             octave  percussion  dBAdjust nicknameRegexes
	BASIC_HARP               ( "Basic Harp",                false, MidiInstrument.ORCHESTRA_HARP,       0,      false,     6.0f, "Harp"),
	MISTY_MOUNTAIN_HARP      ( "Misty Mountain Harp",       false, MidiInstrument.CLEAN_ELEC_GUITAR,    0,      false,   -12.5f, "Misty Harp", "MM Harp", "MMH"),
	BASIC_LUTE               ( "Basic Lute",                false, MidiInstrument.STEEL_STRING_GUITAR,  0,      false,   -19.0f, "New Lute", "LuteB", "Banjo"),
	LUTE_OF_AGES             ( "Lute of Ages",              false, MidiInstrument.NYLON_GUITAR,         0,      false,     0.0f, "Lute", "Age Lute", "LuteA", "LOA", "Guitar"),
	BASIC_THEORBO            ( "Basic Theorbo",             false, MidiInstrument.ACOUSTIC_BASS,       -1,      false,   -12.0f, "Theorbo", "Theo", "Bass"),
	TRAVELLERS_TRUSTY_FIDDLE ( "Traveller's Trusty Fiddle", false, MidiInstrument.PIZZICATO_STRINGS,    1,      false,     1.5f, "Travell?er'?s? (Trusty)? Fiddle", "Trusty Fiddle", "TT Fiddle"),

	BARDIC_FIDDLE            ( "Bardic Fiddle",              true, MidiInstrument.VIOLIN,               1,      false,     6.0f, "Fiddle", "Violin"),
	BASIC_FIDDLE             ( "Basic Fiddle",               true, MidiInstrument.VIOLA,                1,      false,     4.5f),
	LONELY_MOUNTAIN_FIDDLE   ( "Lonely Mountain Fiddle",     true, MidiInstrument.SYNTH_STRING_2,       1,      false,     1.5f, "Lonely Fiddle", "LM Fiddle"),
	SPRIGHTLY_FIDDLE         ( "Sprightly Fiddle",          false, MidiInstrument.FIDDLE,               1,      false,   -10.0f),
	STUDENT_FIDDLE           ( "Student's Fiddle",           true, MidiInstrument.GUITAR_FRET_NOISE,    1,      false,     0.0f, "Student'?s? Fiddle"),

	BASIC_BAGPIPE            ( "Basic Bagpipe",              true, MidiInstrument.BAG_PIPE,             1,      false,    -1.5f, "Bag pipes?"),
	BASIC_BASSOON            ( "Basic Bassoon",              true, MidiInstrument.BASSOON,              0,      false,     5.0f, "Bassoon"),
	BRUSQUE_BASSOON          ( "Brusque Bassoon",           false, MidiInstrument.OBOE,                 0,      false,     5.0f, "Brusk Bassoon"),
	LONELY_MOUNTAIN_BASSOON  ( "Lonely Mountain Bassoon",    true, MidiInstrument.SYNTH_BRASS_2,        0,      false,     5.0f, "Lonely Bassoon", "LM Bassoon"),
	BASIC_CLARINET           ( "Basic Clarinet",             true, MidiInstrument.CLARINET,             1,      false,    -2.0f, "Clarinet"),
	BASIC_FLUTE              ( "Basic Flute",                true, MidiInstrument.FLUTE,                2,      false,    -3.5f, "Flute"),
	BASIC_HORN               ( "Basic Horn",                 true, MidiInstrument.ENGLISH_HORN,         0,      false,    -2.0f, "Horn"),
	BASIC_PIBGORN            ( "Basic Pibgorn",              true, MidiInstrument.CHARANG,              2,      false,    -3.5f, "Pibgorn"),

	BASIC_COWBELL            ( "Basic Cowbell",             false, MidiInstrument.WOODBLOCK,            0,       true,     0.0f, "Cowbell"),
	MOOR_COWBELL             ( "Moor Cowbell",              false, MidiInstrument.STEEL_DRUMS,          0,       true,     0.0f, "More Cowbell"),
	BASIC_DRUM               ( "Basic Drum",                false, MidiInstrument.SYNTH_DRUM,           0,       true,     0.0f, "Drums?");
// @formatter:on

	private static final LotroInstrument[] values = values();

	public static final LotroInstrument DEFAULT_LUTE = LUTE_OF_AGES;
	public static final LotroInstrument DEFAULT_FIDDLE = BARDIC_FIDDLE;
	public static final LotroInstrument DEFAULT_BASSOON = BASIC_BASSOON;
	public static final LotroInstrument DEFAULT_INSTRUMENT = LUTE_OF_AGES;

	public final Note lowestPlayable;
	public final Note highestPlayable;
	public final String friendlyName;
	public final boolean sustainable;
	public final boolean isPercussion;
	public final MidiInstrument midi;
	public final int octaveDelta;
	public final float dBVolumeAdjust;
	private final String[] nicknameRegexes;

	private LotroInstrument(String friendlyName, boolean sustainable, MidiInstrument midiInstrument, int octaveDelta,
			boolean isPercussion, float dBVolumeAdjust, String... nicknameRegexes)
	{
		this.lowestPlayable = Note.MIN_PLAYABLE;
		this.highestPlayable = Note.MAX_PLAYABLE;
		this.friendlyName = friendlyName;
		this.sustainable = sustainable;
		this.midi = midiInstrument;
		this.octaveDelta = octaveDelta;
		this.isPercussion = isPercussion;
		this.dBVolumeAdjust = dBVolumeAdjust;
		this.nicknameRegexes = nicknameRegexes;
	}

	public boolean isSustainable(int noteId)
	{
		return sustainable && isPlayable(noteId);
	}

	public boolean isPlayable(int noteId)
	{
		return noteId >= lowestPlayable.id && noteId <= highestPlayable.id;
	}

	@Override public String toString()
	{
		return friendlyName;
	}

	private static Pattern instrumentRegex;

	public static Pair<LotroInstrument, MatchResult> matchInstrument(String str)
	{
		if (instrumentRegex == null)
		{
			// Build a regex that contains a single capturing group for each instrument
			// Each instrument's group matches its full name or any nicknames
			StringBuilder regex = new StringBuilder();
			regex.append("\\b(?:");
			for (LotroInstrument instrument : values)
			{
				if (instrument.ordinal() > 0)
					regex.append('|');

				regex.append('(');
				regex.append(instrument.friendlyName.replace(" ", "[\\s_]*"));
				for (String nickname : instrument.nicknameRegexes)
					regex.append('|').append(nickname.replace(" ", "[\\s_]*").replaceAll("\\((?!\\?)", "(?:"));
				regex.append(')');
			}
			regex.append(")\\b");

			instrumentRegex = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
		}

		MatchResult result = null;
		Matcher m = instrumentRegex.matcher(str);

		// Iterate through the matches to find the last one
		for (int i = 0; m.find(i); i = m.end())
			result = m.toMatchResult();

		if (result == null)
			return null;

		LotroInstrument instrument = null;
		for (int g = 0; g < result.groupCount() && g < values.length; g++)
		{
			if (result.group(g + 1) != null)
			{
				instrument = values[g];
				break;
			}
		}

		return new Pair<LotroInstrument, MatchResult>(instrument, result);
	}

	public static LotroInstrument findInstrumentName(String str, LotroInstrument defaultInstrument)
	{
		Pair<LotroInstrument, MatchResult> result = matchInstrument(str);
		return (result != null) ? result.first : defaultInstrument;
	}
}

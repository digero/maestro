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

import com.digero.common.midi.Note;

// @formatter:off
public enum LotroInstrument
{
	//                    sustainable   midiProgramId   octaveDelta   isPercussion   dBVolumeAdjust
	LUTE                ( false,         25,             0,           false,         -6.0f         ),
	LUTE_OF_AGES        ( false,         24,             0,           false,          0.0f         ),
	HARP                ( false,         46,             0,           false,          0.0f         ),
	MISTY_MOUNTAIN_HARP ( false,         27,             0,           false,         -1.0f         ),
	THEORBO             ( false,         32,            -1,           false,         -4.0f         ),
	FLUTE               (  true,         73,             2,           false,          5.0f         ),
	CLARINET            (  true,         71,             1,           false,          2.0f         ),
	HORN                (  true,         69,             0,           false,          0.0f         ),
	BAGPIPE             (  true,        109,             1,           false,         -1.0f         ),
	PIBGORN             (  true,         84,             2,           false,          1.0f         ),
	DRUMS               ( false,        118,             0,            true,          0.0f         ),
	COWBELL             ( false,        115,             0,            true,          0.0f         ),
	MOOR_COWBELL        ( false,        114,             0,            true,          0.0f         );
// @formatter:on

	public static final LotroInstrument DEFAULT = LUTE_OF_AGES;

	public final Note lowestPlayable;
	public final Note highestPlayable;
	public final boolean sustainable;
	public final boolean isPercussion;
	public final int midiProgramId;
	public final int octaveDelta;
	public final float dBVolumeAdjust;

	private LotroInstrument(boolean sustainable, int midiProgramId, int octaveDelta, boolean isPercussion,
			float dBVolumeAdjust)
	{
		this.lowestPlayable = Note.MIN_PLAYABLE;
		this.highestPlayable = Note.MAX_PLAYABLE;
		this.sustainable = sustainable;
		this.midiProgramId = midiProgramId;
		this.octaveDelta = octaveDelta;
		this.isPercussion = isPercussion;
		this.dBVolumeAdjust = dBVolumeAdjust;
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
		if (this == MOOR_COWBELL)
			return "Moor Cowbell";
		else if (this == LUTE_OF_AGES)
			return "Lute of Ages";
		else if (this == MISTY_MOUNTAIN_HARP)
			return "Misty Mountain Harp";

		String name = super.toString();
		return name.substring(0, 1) + name.substring(1).toLowerCase();
	}

	public static LotroInstrument parseInstrument(String string) throws IllegalArgumentException
	{
		string = string.toUpperCase().replace(' ', '_');
		if (string.equals("MM_HARP"))
			return MISTY_MOUNTAIN_HARP;
		else if (string.equals("BASIC_LUTE"))
			return LUTE;

		return LotroInstrument.valueOf(string);
	}
}

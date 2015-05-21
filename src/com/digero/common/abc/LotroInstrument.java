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
	//           low       high      sustainable   midiProgramId   octaveDelta   isPercussion   dBVolumeAdjust
	LUTE        (Note.C2,  Note.C5,  false,         24,             0,           false,         -5.0f         ),
	HARP        (Note.C2,  Note.C5,  false,         46,             0,           false,         -1.0f         ),
	THEORBO     (Note.C2,  Note.C5,  false,         32,            -1,           false,         -4.0f         ),
	FLUTE       (Note.C2,  Note.C5,   true,         73,             2,           false,          4.0f         ),
	CLARINET    (Note.C2,  Note.C5,   true,         71,             1,           false,          2.0f         ),
	HORN        (Note.C2,  Note.C5,   true,         69,             0,           false,          0.0f         ),
	BAGPIPE     (Note.C2,  Note.C5,   true,        109,             1,           false,         -1.0f         ),
	PIBGORN     (Note.C2,  Note.C5,   true,         84,             2,           false,          1.0f         ),
	DRUMS       (Note.C2,  Note.C5,  false,        118,             0,            true,          0.0f         ),
	COWBELL     (Note.C2,  Note.C5,  false,        115,             0,            true,          0.0f         ),
	MOOR_COWBELL(Note.C2,  Note.C5,  false,        114,             0,            true,          0.0f         );

// @formatter:on

	public final Note lowestPlayable;
	public final Note highestPlayable;
	public final boolean sustainable;
	public final boolean isPercussion;
	public final int midiProgramId;
	public final int octaveDelta;
	public final float dbVolumeAdjust;

	private LotroInstrument(Note low, Note high, boolean sustainable, int midiProgramId, int octaveDelta,
			boolean isPercussion, float dbVolumeAdjust)
	{
		this.lowestPlayable = low;
		this.highestPlayable = high;
		this.sustainable = sustainable;
		this.midiProgramId = midiProgramId;
		this.octaveDelta = octaveDelta;
		this.isPercussion = isPercussion;
		this.dbVolumeAdjust = dbVolumeAdjust;
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

		String name = super.toString();
		return name.substring(0, 1) + name.substring(1).toLowerCase();
	}

	public static LotroInstrument parseInstrument(String string) throws IllegalArgumentException
	{
		return LotroInstrument.valueOf(string.toUpperCase().replace(' ', '_'));
	}
}

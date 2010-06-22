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

public enum LotroInstrument {
	LUTE(Note.C2, Note.C5, false, 24, 0), //
	HARP(Note.C2, Note.C5, false, 46, 0), //
	THEORBO(Note.C2, Note.C5, false, 32, -1), //
	FLUTE(Note.C2, Note.C5, true, 73, 2), //
	CLARINET(Note.D2, Note.C5, true, 71, 1), //
	HORN(Note.Cs2, Note.C5, true, 69, 0), //
	BAGPIPE(Note.C2, Note.C5, true, 109, 1), //
	DRUMS(Note.C2, Note.C5, false, 118, 0), //
	COWBELL(Note.C2, Note.C5, false, 115, 0), //
	MOOR_COWBELL(Note.C2, Note.C5, false, 114, 0);

	private static final LotroInstrument[] NON_DRUMS = new LotroInstrument[] {
			LUTE, HARP, THEORBO, BAGPIPE, CLARINET, FLUTE, HORN
	};

	public static final LotroInstrument[] getNonDrumInstruments() {
		return NON_DRUMS;
	}

	public final Note lowestPlayable;
	public final Note highestPlayable;
	public final boolean sustainable;
	public final int midiProgramId;
	public final int octaveDelta; 

	private LotroInstrument(Note low, Note high, boolean sustainable, int midiProgramId, int octaveDelta) {
		this.lowestPlayable = low;
		this.highestPlayable = high;
		this.sustainable = sustainable;
		this.midiProgramId = midiProgramId;
		this.octaveDelta = octaveDelta;
	}

	public boolean isSustainable(int noteId) {
		return sustainable && noteId >= lowestPlayable.id && noteId <= highestPlayable.id;
	}

	@Override
	public String toString() {
		if (this == MOOR_COWBELL)
			return "Moor Cowbell";
		
		String name = super.toString();
		return name.substring(0, 1) + name.substring(1).toLowerCase();
	}
}

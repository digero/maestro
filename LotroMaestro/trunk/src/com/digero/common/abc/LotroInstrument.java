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
	LUTE(Note.C2, Note.C5, false, 24, 0, false), //
	HARP(Note.C2, Note.C5, false, 46, 0, false), //
	THEORBO(Note.C2, Note.C5, false, 32, -1, false), //
	FLUTE(Note.C2, Note.C5, true, 73, 2, false), //
	CLARINET(Note.D2, Note.C5, true, 71, 1, false), //
	HORN(Note.Cs2, Note.C5, true, 69, 0, false), //
	BAGPIPE(Note.C2, Note.C5, true, 109, 1, false), //
	PIBGORN(Note.D2, Note.C5, true, 84, 2, false), //
	DRUMS(Note.C2, Note.C5, false, 118, 0, true), //
	COWBELL(Note.C2, Note.C5, false, 115, 0, true), //
	MOOR_COWBELL(Note.C2, Note.C5, false, 114, 0, true);

	public final Note lowestPlayable;
	public final Note highestPlayable;
	public final boolean sustainable;
	public final boolean isPercussion;
	public final int midiProgramId;
	public final int octaveDelta;

	private LotroInstrument(Note low, Note high, boolean sustainable, int midiProgramId, int octaveDelta,
			boolean isPercussion) {
		this.lowestPlayable = low;
		this.highestPlayable = high;
		this.sustainable = sustainable;
		this.midiProgramId = midiProgramId;
		this.octaveDelta = octaveDelta;
		this.isPercussion = isPercussion;
	}

	public boolean isSustainable(int noteId) {
		return sustainable && isPlayable(noteId);
	}

	public boolean isPlayable(int noteId) {
		return noteId >= lowestPlayable.id && noteId <= highestPlayable.id;
	}

	public boolean isBadNote(int noteId) {
		if (this == PIBGORN) {
			return noteId == Note.C2.id || noteId == Note.Cs2.id || noteId == Note.As2.id || noteId == Note.Gs4.id
					|| noteId == Note.As4.id;
		}

		return false;
	}

	@Override
	public String toString() {
		if (this == MOOR_COWBELL)
			return "Moor Cowbell";

		String name = super.toString();
		return name.substring(0, 1) + name.substring(1).toLowerCase();
	}
}

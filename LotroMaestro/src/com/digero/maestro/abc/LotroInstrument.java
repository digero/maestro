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

package com.digero.maestro.abc;

import com.digero.maestro.midi.Note;


public enum LotroInstrument {
	LUTE(Note.C2, Note.C5, false, 24), //
	HARP(Note.C2, Note.C5, false, 46), //
	THEORBO(Note.C2, Note.C5, false, 32), //
	BAGPIPE(Note.C2, Note.C5, true, 109), //
	CLARINET(Note.D2, Note.C5, true, 71), //
	FLUTE(Note.C2, Note.C5, true, 73), //
	HORN(Note.Cs2, Note.C5, true, 69), //
	DRUMS(Note.C2, Note.C5, false, 118);

	public final Note lowestPlayable;
	public final Note highestPlayable;
	public final boolean sustainable;
	public final int midiProgramId;

	private LotroInstrument(Note low, Note high, boolean sustainable, int midiProgramId) {
		this.lowestPlayable = low;
		this.highestPlayable = high;
		this.sustainable = sustainable;
		this.midiProgramId = midiProgramId;
	}

	public boolean isSustainable(int noteId) {
		return sustainable && noteId >= lowestPlayable.id && noteId <= highestPlayable.id;
	}

	@Override
	public String toString() {
		String name = super.toString();
		return name.substring(0, 1) + name.substring(1).toLowerCase();
	}
}

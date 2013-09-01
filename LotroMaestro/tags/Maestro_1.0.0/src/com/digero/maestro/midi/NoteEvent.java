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

package com.digero.maestro.midi;

import com.digero.common.midi.Note;

public class NoteEvent implements Comparable<NoteEvent> {
	public final Note note;
	public final int velocity;
	public long startMicros;
	public long endMicros;
	public NoteEvent tiesFrom = null;
	public NoteEvent tiesTo = null;

	public NoteEvent(Note note, int velocity, long startMicros, long endMicros) {
		this.note = note;
		this.velocity = velocity;
		this.startMicros = startMicros;
		this.endMicros = endMicros;
	}

	public long getLength() {
		return endMicros - startMicros;
	}

	public void setLength(long length) {
		endMicros = startMicros + length;
	}

	public long getTieLength() {
		return getTieEnd().endMicros - getTieStart().startMicros;
	}

	public NoteEvent getTieStart() {
		if (tiesFrom == null)
			return this;
		assert tiesFrom.startMicros < this.startMicros;
		return tiesFrom.getTieStart();
	}

	public NoteEvent getTieEnd() {
		if (tiesTo == null)
			return this;
		assert tiesTo.endMicros > this.endMicros;
		return tiesTo.getTieEnd();
	}
	
	/**
	 * Splits the NoteEvent into two events with a tie between them. 
	 * 
	 * @param splitPointMicros The time index to split the NoteEvent.
	 * @return The new NoteEvent that was created starting at splitPointMicros.
	 */
	public NoteEvent splitWithTie(long splitPointMicros) {
		assert splitPointMicros > startMicros && splitPointMicros < endMicros;
		
		NoteEvent next = new NoteEvent(note, velocity, splitPointMicros, endMicros);
		this.endMicros = splitPointMicros;
		
		if (note != Note.REST) {
			if (this.tiesTo != null) {
				next.tiesTo = this.tiesTo;
				this.tiesTo.tiesFrom = next;
			}
			next.tiesFrom = this;
			this.tiesTo = next;
		}
		
		return next;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NoteEvent) {
			NoteEvent that = (NoteEvent) obj;
			return (this.startMicros == that.startMicros) && (this.endMicros == that.endMicros)
					&& (this.note.id == that.note.id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ((int) startMicros) ^ ((int) endMicros) ^ note.id;
	}

	@Override
	public int compareTo(NoteEvent that) {
		if (that == null)
			return 1;

		if (this.startMicros != that.startMicros)
			return (this.startMicros > that.startMicros) ? 1 : -1;

		if (this.note.id != that.note.id)
			return this.note.id - that.note.id;

		if (this.endMicros != that.endMicros)
			return (this.endMicros > that.endMicros) ? 1 : -1;

		return 0;
	}
}
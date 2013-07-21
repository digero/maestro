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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.digero.common.abc.AbcConstants;
import com.digero.common.abc.Dynamics;
import com.digero.common.midi.Note;

public class Chord implements AbcConstants {
	private long startMicros;
	private long endMicros;
	private boolean hasTooManyNotes = false;
	private List<NoteEvent> notes = new ArrayList<NoteEvent>();

	public Chord(NoteEvent firstNote) {
		startMicros = firstNote.startMicros;
		endMicros = firstNote.endMicros;
		notes.add(firstNote);
	}

	public long getStartMicros() {
		return startMicros;
	}

	public long getEndMicros() {
		return endMicros;
	}

	public int size() {
		return notes.size();
	}

	public boolean hasTooManyNotes() {
		return hasTooManyNotes;
	}

	public NoteEvent get(int i) {
		return notes.get(i);
	}

	public boolean add(NoteEvent ne, boolean force) {
		while (force && size() >= MAX_CHORD_NOTES) {
			remove(size() - 1);
			hasTooManyNotes = true;
		}
		return add(ne);
	}

	public boolean add(NoteEvent ne) {
		if (ne.getLength() == 0) {
			hasTooManyNotes = true;
			return false;
		}

		if (size() >= MAX_CHORD_NOTES) {
			return false;
		}

		notes.add(ne);
		if (ne.endMicros < endMicros) {
			endMicros = ne.endMicros;
		}
		return true;
	}

	public NoteEvent remove(int i) {
		if (size() <= 1)
			return null;

		NoteEvent ne = notes.remove(i);
		if (ne.endMicros == this.endMicros)
			recalcEndMicros();

		return ne;
	}

	public boolean remove(NoteEvent ne) {
		return notes.remove(ne);
	}

	public Dynamics calcDynamics() {
		int velocity = -1;
		for (NoteEvent ne : notes) {
			if (ne.note != Note.REST && ne.tiesFrom == null && ne.velocity > velocity)
				velocity = ne.velocity;
		}

		if (velocity == -1)
			return null;

		return Dynamics.fromMidiVelocity(velocity);
	}

	public void recalcEndMicros() {
		if (!notes.isEmpty()) {
			this.endMicros = notes.get(0).endMicros;
			for (int k = 1; k < notes.size(); k++) {
				if (notes.get(k).endMicros < endMicros) {
					this.endMicros = notes.get(k).endMicros;
				}
			}
		}
	}

	public void sort() {
		Collections.sort(notes);
	}
}
package com.digero.maestro.abc;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.digero.maestro.midi.Note;

public class LotroDrumInfo implements Comparable<LotroDrumInfo> {
	private static Map<Integer, LotroDrumInfo> byId = new HashMap<Integer, LotroDrumInfo>();
	private static SortedMap<String, SortedSet<LotroDrumInfo>> byCategory = new TreeMap<String, SortedSet<LotroDrumInfo>>();

	public static final LotroDrumInfo DISABLED = new LotroDrumInfo(Note.REST, "None", "#None");
	public static final List<LotroDrumInfo> ALL_DRUMS;

	static {
		byCategory.put(DISABLED.category, new TreeSet<LotroDrumInfo>());
		byCategory.get(DISABLED.category).add(DISABLED);
		byId.put(DISABLED.note.id, DISABLED);

		makeCategory("Rim Shot", Note.Ds3, Note.F3);
		makeCategory("Pitch Bend", Note.D4, Note.E4, Note.F4);
		makeCategory("Rattle", Note.G3, Note.A3, Note.B3, Note.C4);
		makeCategory("Rattle Bells", Note.As2);
		makeCategory("Rattle Short", Note.Cs2, Note.Fs2, Note.Gs2);
		makeCategory("Bass Open", Note.Gs3, Note.As3);

		int noteCount = Note.MAX_PLAYABLE.id - Note.MIN_PLAYABLE.id + 1;
		if (byId.keySet().size() < noteCount) {
			List<Integer> unassigned = new ArrayList<Integer>(noteCount);
			for (int id = Note.MIN_PLAYABLE.id; id <= Note.MAX_PLAYABLE.id; id++) {
				unassigned.add(id);
			}
			unassigned.removeAll(byId.keySet());
			for (int id : unassigned) {
				add("Unassigned", Note.fromId(id));
			}
		}

		ALL_DRUMS = Collections.unmodifiableList(new ArrayList<LotroDrumInfo>(new AbstractCollection<LotroDrumInfo>() {
			public Iterator<LotroDrumInfo> iterator() {
				return new DrumInfoIterator();
			}

			public int size() {
				return byId.size();
			}
		}));
	}

	private static final Comparator<Note> noteComparator = new Comparator<Note>() {
		public int compare(Note o1, Note o2) {
			return o1.id - o2.id;
		}
	};

	private static void makeCategory(String category, Note... notes) {
		Arrays.sort(notes, noteComparator);
		for (Note note : notes) {
			add(category, note);
		}
	}

	private static void add(String category, Note note) {
		SortedSet<LotroDrumInfo> categorySet = byCategory.get(category);
		if (categorySet == null)
			byCategory.put(category, categorySet = new TreeSet<LotroDrumInfo>());

		String name = category + " " + (categorySet.size() + 1);
		LotroDrumInfo info = new LotroDrumInfo(note, name, category);

		categorySet.add(info);
		byId.put(note.id, info);
	}

	public static LotroDrumInfo getById(int noteId) {
		return byId.get(noteId);
	}

	private static class DrumInfoIterator implements Iterator<LotroDrumInfo> {
		private Iterator<SortedSet<LotroDrumInfo>> outerIter;
		private Iterator<LotroDrumInfo> innerIter;

		public DrumInfoIterator() {
			outerIter = byCategory.values().iterator();
		}

		public boolean hasNext() {
			return outerIter.hasNext() || (innerIter != null && innerIter.hasNext());
		}

		@Override
		public LotroDrumInfo next() {
			while (innerIter == null || !innerIter.hasNext())
				innerIter = outerIter.next().iterator();

			return innerIter.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public final Note note;
	public final String name;
	public final String category;

	private LotroDrumInfo(Note note, String name, String category) {
		this.note = note;
		this.name = name;
		this.category = category;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(LotroDrumInfo that) {
		if (that == null)
			return 1;

		return this.note.id - that.note.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass())
			return false;

		return this.note.id == ((LotroDrumInfo) obj).note.id;
	}

	@Override
	public int hashCode() {
		return this.note.id;
	}
}

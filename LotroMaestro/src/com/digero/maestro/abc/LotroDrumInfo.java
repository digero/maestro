package com.digero.maestro.abc;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

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

		Map<Note, String> drumNames = new HashMap<Note, String>();
		drumNames.put(Note.C2, "Conga Open");
		drumNames.put(Note.Cs2, "Rattle Short");
		drumNames.put(Note.D2, "Muff");
		drumNames.put(Note.Ds2, "Slap");
		drumNames.put(Note.E2, "Slap");
		drumNames.put(Note.F2, "Muted");
		drumNames.put(Note.Fs2, "Rattle Short");
		drumNames.put(Note.G2, "Tom High");
		drumNames.put(Note.Gs2, "Rattle Short");
		drumNames.put(Note.A2, "Tom High");
		drumNames.put(Note.As2, "Rattle High");
		drumNames.put(Note.B2, "Tom Mid");
		drumNames.put(Note.C3, "Muted Mid");
		drumNames.put(Note.Cs3, "Bass");
		drumNames.put(Note.D3, "Bass Slap");
		drumNames.put(Note.Ds3, "Rim Shot");
		drumNames.put(Note.E3, "Slap");
		drumNames.put(Note.F3, "Rim Shot");
		drumNames.put(Note.Fs3, "Slap");
		drumNames.put(Note.G3, "Rattle");
		drumNames.put(Note.Gs3, "Bass");
		drumNames.put(Note.A3, "Rattle");
		drumNames.put(Note.As3, "Bass");
		drumNames.put(Note.B3, "Rattle");
		drumNames.put(Note.C4, "Rattle");
		drumNames.put(Note.Cs4, "Muted");
		drumNames.put(Note.D4, "Conga Bend");
		drumNames.put(Note.Ds4, "Tom Mid");
		drumNames.put(Note.E4, "Conga Bend");
		drumNames.put(Note.F4, "Conga Bend");
		drumNames.put(Note.Fs4, "Slap");
		drumNames.put(Note.G4, "Conga Open");
		drumNames.put(Note.Gs4, "Slap");
		drumNames.put(Note.A4, "Conga Open");
		drumNames.put(Note.As4, "Muff");
		drumNames.put(Note.B4, "Conga Open");
		drumNames.put(Note.C5, "Slap");

		for (Entry<Note, String> entry : drumNames.entrySet()) {
			add(entry.getValue(), entry.getKey());
		}

//		makeCategory("Rim Shot", Note.Ds3, Note.F3);
//		makeCategory("Pitch Bend", Note.D4, Note.E4, Note.F4);
//		makeCategory("Rattle", Note.G3, Note.A3, Note.B3, Note.C4);
//		makeCategory("Rattle Bells", Note.As2);
//		makeCategory("Rattle Short", Note.Cs2, Note.Fs2, Note.Gs2);
//		makeCategory("Bass Open", Note.Gs3, Note.As3);

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

//	private static final Comparator<Note> noteComparator = new Comparator<Note>() {
//		public int compare(Note o1, Note o2) {
//			return o1.id - o2.id;
//		}
//	};

//	private static void makeCategory(String category, Note... notes) {
//		Arrays.sort(notes, noteComparator);
//		for (Note note : notes) {
//			add(category, note);
//		}
//	}

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

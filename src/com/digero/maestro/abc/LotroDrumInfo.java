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

import com.digero.common.midi.Note;

public class LotroDrumInfo implements Comparable<LotroDrumInfo>
{
	private static Map<Integer, LotroDrumInfo> byId = new HashMap<Integer, LotroDrumInfo>();
	private static SortedMap<String, SortedSet<LotroDrumInfo>> byCategory = new TreeMap<String, SortedSet<LotroDrumInfo>>();

	public static final LotroDrumInfo DISABLED = new LotroDrumInfo(Note.REST, "None", "#None");
	public static final List<LotroDrumInfo> ALL_DRUMS;

	static
	{
		byCategory.put(DISABLED.category, new TreeSet<LotroDrumInfo>());
		byCategory.get(DISABLED.category).add(DISABLED);
		byId.put(DISABLED.note.id, DISABLED);

		add(Note.C2, "Conga High");
		add(Note.Cs2, "Rattle Short");
		add(Note.D2, "Bongo High");
		add(Note.Ds2, "Slap");
		add(Note.E2, "Slap");
		add(Note.F2, "Muted");
		add(Note.Fs2, "Rattle Short");
		add(Note.G2, "Tom High");
		add(Note.Gs2, "Rattle Short");
		add(Note.A2, "Tom High");
		add(Note.As2, "Tambourine");
		add(Note.B2, "Tom Mid");
		add(Note.C3, "Muted Mid");
		add(Note.Cs3, "Bass Slap");
		add(Note.D3, "Bass Slap");
		add(Note.Ds3, "Rim Shot");
		add(Note.E3, "Slap");
		add(Note.F3, "Rim Shot");
		add(Note.Fs3, "Slap");
		add(Note.G3, "Rattle");
		add(Note.Gs3, "Bass");
		add(Note.A3, "Rattle Long");
		add(Note.As3, "Bass Open");
		add(Note.B3, "Rattle");
		add(Note.C4, "Rattle");
		add(Note.Cs4, "Muted");
		add(Note.D4, "Bend Low Up");
		add(Note.Ds4, "Tom Mid");
		add(Note.E4, "Bend Mid Down");
		add(Note.F4, "Bend High Up");
		add(Note.Fs4, "Slap");
		add(Note.G4, "Conga Low");
		add(Note.Gs4, "Slap");
		add(Note.A4, "Bongo Low");
		add(Note.As4, "Conga High");
		add(Note.B4, "Conga Mid");
		add(Note.C5, "Slap");

		int noteCount = Note.MAX_PLAYABLE.id - Note.MIN_PLAYABLE.id + 1;
		if (byId.keySet().size() < noteCount)
		{
			List<Integer> unassigned = new ArrayList<Integer>(noteCount);
			for (int id = Note.MIN_PLAYABLE.id; id <= Note.MAX_PLAYABLE.id; id++)
			{
				unassigned.add(id);
			}
			unassigned.removeAll(byId.keySet());
			for (int id : unassigned)
			{
				add(Note.fromId(id), "Unassigned");
			}
		}

		ALL_DRUMS = Collections.unmodifiableList(new ArrayList<LotroDrumInfo>(new AbstractCollection<LotroDrumInfo>()
		{
			@Override public Iterator<LotroDrumInfo> iterator()
			{
				return new DrumInfoIterator();
			}

			@Override public int size()
			{
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

	private static void add(Note note, String category)
	{
		SortedSet<LotroDrumInfo> categorySet = byCategory.get(category);
		if (categorySet == null)
		{
			byCategory.put(category, categorySet = new TreeSet<LotroDrumInfo>());
		}
		else if (categorySet.size() == 1)
		{
			// We're about to add a second one to the category...
			// add the "1" to the name of the existing element
			Note prevNote = categorySet.first().note;
			String prevName = category + " 1 (" + prevNote.abc + ")";
			LotroDrumInfo prevInfo = new LotroDrumInfo(prevNote, prevName, category);
			categorySet.clear();
			categorySet.add(prevInfo);
			byId.put(prevNote.id, prevInfo);
		}

		String name;
		if (categorySet.isEmpty())
		{
			// If this is the first item in the category, don't add its number to the list
			name = category + " (" + note.abc + ")";
		}
		else
		{
			name = category + " " + (categorySet.size() + 1) + " (" + note.abc + ")";
		}
		LotroDrumInfo info = new LotroDrumInfo(note, name, category);

		categorySet.add(info);
		byId.put(note.id, info);
	}

	public static LotroDrumInfo getById(int noteId)
	{
		return byId.get(noteId);
	}

	private static class DrumInfoIterator implements Iterator<LotroDrumInfo>
	{
		private Iterator<SortedSet<LotroDrumInfo>> outerIter;
		private Iterator<LotroDrumInfo> innerIter;

		public DrumInfoIterator()
		{
			outerIter = byCategory.values().iterator();
		}

		@Override public boolean hasNext()
		{
			return outerIter.hasNext() || (innerIter != null && innerIter.hasNext());
		}

		@Override public LotroDrumInfo next()
		{
			while (innerIter == null || !innerIter.hasNext())
				innerIter = outerIter.next().iterator();

			return innerIter.next();
		}

		@Override public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	public final Note note;
	public final String name;
	public final String category;

	private LotroDrumInfo(Note note, String name, String category)
	{
		this.note = note;
		this.name = name;
		this.category = category;
	}

	@Override public String toString()
	{
		return name;
	}

	@Override public int compareTo(LotroDrumInfo that)
	{
		if (that == null)
			return 1;

		return this.note.id - that.note.id;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj == null || obj.getClass() != this.getClass())
			return false;

		return this.note.id == ((LotroDrumInfo) obj).note.id;
	}

	@Override public int hashCode()
	{
		return this.note.id;
	}
}

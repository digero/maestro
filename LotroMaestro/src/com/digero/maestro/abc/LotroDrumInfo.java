package com.digero.maestro.abc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.digero.maestro.midi.Note;

public class LotroDrumInfo {
	private static Map<Integer, LotroDrumInfo> byId = new HashMap<Integer, LotroDrumInfo>();
	private static Map<String, List<LotroDrumInfo>> byCategory = new HashMap<String, List<LotroDrumInfo>>();

	static {
		makeCategory("Rim Shot", Note.Ds3);
		add("Rim Shot", Note.F3);

		makeCategory("Pitch Bend", Note.D4);
		add("Pitch Bend", Note.E4);
		add("Pitch Bend", Note.F4);

		makeCategory("Rattle", Note.G3);
		add("Rattle", Note.A3);
		add("Rattle", Note.B3);
		add("Rattle", Note.C4);

		makeCategory("Rattle Bells", Note.As2);

		makeCategory("Rattle Short", Note.Cs2);
		add("Rattle Short", Note.Fs2);
		add("Rattle Short", Note.Gs2);

		makeCategory("Bass Open", Note.As3);
		add("Bass Open", Note.Gs3);

		makeCategory("Bass", Note.As3, Note.Gs3);
	}

	private static void makeCategory(String category, Note... notes) {
		for (Note note : notes) {
			add(category, note);
		}
	}

	private static void add(String category, Note note) {
		List<LotroDrumInfo> categoryList = byCategory.get(category);
		if (categoryList == null)
			byCategory.put(category, categoryList = new ArrayList<LotroDrumInfo>());

		String name = category + " " + (categoryList.size() + 1);
		LotroDrumInfo info = new LotroDrumInfo(note, name, category);

		categoryList.add(info);
		byId.put(note.id, info);
	}

	public static LotroDrumInfo getById(int noteId) {
		throw new RuntimeException("Not implemented");
	}

	public final Note note;
	public final String name;
	public final String category;

	private LotroDrumInfo(Note note, String name, String category) {
		this.note = note;
		this.name = name;
		this.category = category;
	}
}

package com.digero.tools.soundfont;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.digero.common.abc.AbcConstants;
import com.digero.common.abc.LotroInstrument;
import com.digero.tools.soundfont.SampleInfo.Key;

public class StandardInstrumentInfo extends InstrumentInfo
{
	public final SortedSet<SampleInfo> usedSamples;
	private final int notesBelowSample;
	private final int notesAboveSample;

	public StandardInstrumentInfo(LotroInstrument lotroInstrument, int notesPerSample, Map<Key, SampleInfo> samples)
	{
		this(lotroInstrument, lotroInstrument.toString(), lotroInstrument.lowestPlayable.id,
				lotroInstrument.highestPlayable.id, notesPerSample, samples);
	}

	public StandardInstrumentInfo(LotroInstrument lotroInstrument, String name, int lowestNoteId, int highestNoteId,
			int notesPerSample, Map<Key, SampleInfo> samples)
	{
		super(lotroInstrument, name, lowestNoteId, highestNoteId);

		this.notesBelowSample = (notesPerSample - 1) / 2;
		this.notesAboveSample = (notesPerSample - 1) - notesBelowSample;

		SortedSet<SampleInfo> usedSamples = new TreeSet<SampleInfo>();
		int startId = lowestNoteId + notesBelowSample;
		for (int id = startId; id <= highestNoteId; id += notesPerSample)
			usedSamples.add(samples.get(new Key(lotroInstrument, id)));

		this.usedSamples = Collections.unmodifiableSortedSet(usedSamples);
	}

	@Override public void print(PrintStream out)
	{
		out.println();
		out.println("    InstrumentName=" + name);
		out.println();

		int i = 0;
		for (SampleInfo sample : usedSamples)
		{
			int lowKey = sample.key.noteId - notesBelowSample;
			int highKey = sample.key.noteId + notesAboveSample;
			if (++i == usedSamples.size())
				highKey = highestNoteId;

			boolean isLooped = sample.key.lotroInstrument == LotroInstrument.BAGPIPE
					&& sample.key.noteId <= AbcConstants.BAGPIPE_LAST_DRONE_NOTE_ID;

			out.println("        Sample=" + sample.name);
			out.println("            Z_LowKey=" + lowKey);
			out.println("            Z_HighKey=" + highKey);
			out.println("            Z_LowVelocity=0");
			out.println("            Z_HighVelocity=127");
			if (isLooped)
				out.println("            Z_sampleModes=1");
			out.println();
		}
	}
}
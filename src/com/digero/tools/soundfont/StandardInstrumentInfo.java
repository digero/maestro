package com.digero.tools.soundfont;

import java.io.PrintStream;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.digero.common.abc.AbcConstants;
import com.digero.common.abc.LotroInstrument;
import com.digero.tools.soundfont.SampleInfo.Key;

public class StandardInstrumentInfo extends InstrumentInfo
{
	public final NavigableSet<SampleInfo> usedSamples;

	public StandardInstrumentInfo(LotroInstrument lotroInstrument, int notesPerSample, Map<Key, SampleInfo> samples)
	{
		this(lotroInstrument, lotroInstrument.toString(), lotroInstrument.lowestPlayable.id,
				lotroInstrument.highestPlayable.id, notesPerSample, samples);
	}

	public StandardInstrumentInfo(LotroInstrument lotroInstrument, String name, int lowestNoteId, int highestNoteId,
			int notesPerSample, Map<Key, SampleInfo> samples)
	{
		super(lotroInstrument, name, lowestNoteId, highestNoteId);

		this.usedSamples = new TreeSet<SampleInfo>();
		int startId = lowestNoteId + (notesPerSample - 1) / 2;

		for (int id = startId; id <= highestNoteId; id++)
		{
			SampleInfo sample = samples.get(new Key(lotroInstrument, id));
			if (sample != null)
			{
				usedSamples.add(sample);
				id += notesPerSample - 1;
			}
		}
	}

	@Override public void print(PrintStream out)
	{
		out.println();
		out.println("    InstrumentName=" + name);
		out.println();

		int prevHighKey = lowestNoteId - 1;
		for (SampleInfo sample : usedSamples)
		{
			SampleInfo next = usedSamples.higher(sample);

			int lowKey = prevHighKey + 1;
			int highKey = (next == null) ? highestNoteId : (sample.key.noteId + next.key.noteId) / 2;
			prevHighKey = highKey;

			LotroInstrument instrument = sample.key.lotroInstrument;
			boolean isLooped = instrument == LotroInstrument.BASIC_BAGPIPE
					&& sample.key.noteId <= AbcConstants.BAGPIPE_LAST_DRONE_NOTE_ID;

			int releaseTime = SoundFontUtil.secondsToTimecents(AbcConstants.NOTE_RELEASE_SECONDS);

			out.println("        Sample=" + sample.name);
			out.println("            Z_LowKey=" + lowKey);
			out.println("            Z_HighKey=" + highKey);
			out.println("            Z_LowVelocity=0");
			out.println("            Z_HighVelocity=127");
			out.println("            Z_releaseVolEnv=" + releaseTime);
			if (isLooped)
				out.println("            Z_sampleModes=1");
			out.println();
		}
	}
}
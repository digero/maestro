package com.digero.tools.soundfont;

import java.io.PrintStream;
import java.util.Random;

import com.digero.common.abc.AbcConstants;

public class CowbellInfo extends InstrumentInfo
{
	public final SampleInfo sample;

	public CowbellInfo(SampleInfo sample)
	{
		super(sample.key.lotroInstrument);
		this.sample = sample;
	}

	@Override public void print(PrintStream out)
	{
		out.println();
		out.println("    InstrumentName=" + lotroInstrument);
		out.println();

		Random rand = new Random(lotroInstrument.midi.id());
		for (int id = lowestNoteId; id <= highestNoteId; id++)
		{
			out.println("        Sample=" + sample.name);
			out.println("            Z_LowKey=" + id);
			out.println("            Z_HighKey=" + id);
			out.println("            Z_LowVelocity=0");
			out.println("            Z_HighVelocity=127");
			if (id != AbcConstants.COWBELL_NOTE_ID)
			{
				int fineTune = rand.nextInt(100) - 50;
				if (fineTune < 0)
					fineTune += 65536;

				out.println("            Z_fineTune=" + fineTune);
				out.println("            Z_overridingRootKey=" + id);
			}
			out.println();
		}
	}
}
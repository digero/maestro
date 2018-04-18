package com.digero.tools.soundfont;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.digero.common.abc.LotroInstrument;

public class PresetInfo implements Comparable<PresetInfo>
{
	public final SortedSet<InstrumentInfo> instruments;
	public final LotroInstrument lotroInstrument;

	public PresetInfo(InstrumentInfo... instruments)
	{
		this.lotroInstrument = instruments[0].lotroInstrument;
		this.instruments = Collections.unmodifiableSortedSet(new TreeSet<InstrumentInfo>(Arrays.asList(instruments)));
	}

	public void print(PrintStream out)
	{
		print(out, lotroInstrument.toString(), lotroInstrument.midiProgramId);
	}

	public void print(PrintStream out, String name, int programId)
	{
		out.println();
		out.println("    PresetName=" + name);
		out.println("        Bank=0");
		out.println("        Program=" + programId);
		out.println();

		for (InstrumentInfo instrument : instruments)
		{
			out.println("        Instrument=" + instrument.name);
			out.println("            L_LowKey=" + instrument.lowestNoteId);
			out.println("            L_HighKey=" + instrument.highestNoteId);
			out.println("            L_LowVelocity=0");
			out.println("            L_HighVelocity=127");
			int attenuation = SoundFontUtil.dBToAttenuationValue(instrument.lotroInstrument.dBVolumeAdjust);
			if (attenuation != 0)
				out.println("            L_initialAttenuation=" + attenuation);
			out.println();
		}
	}

	@Override public int compareTo(PresetInfo o)
	{
		Iterator<InstrumentInfo> self = instruments.iterator();
		Iterator<InstrumentInfo> that = o.instruments.iterator();
		while (self.hasNext() && that.hasNext())
		{
			int result = self.next().compareTo(that.next());
			if (result != 0)
				return result;
		}
		if (self.hasNext())
			return 1;
		if (that.hasNext())
			return -1;
		return 0;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof PresetInfo))
			return false;

		return instruments.equals(((PresetInfo) obj).instruments);
	}

	@Override public int hashCode()
	{
		return instruments.hashCode();
	}
}
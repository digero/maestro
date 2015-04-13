package com.digero.tools.soundfont;

import java.io.PrintStream;

import com.digero.common.abc.LotroInstrument;

public abstract class InstrumentInfo implements Comparable<InstrumentInfo>
{
	public final LotroInstrument lotroInstrument;
	public final String name;
	public final int lowestNoteId;
	public final int highestNoteId;

	public InstrumentInfo(LotroInstrument lotroInstrument)
	{
		this(lotroInstrument, lotroInstrument.toString(), lotroInstrument.lowestPlayable.id,
				lotroInstrument.highestPlayable.id);
	}

	public InstrumentInfo(LotroInstrument lotroInstrument, String name, int lowestNoteId, int highestNoteId)
	{
		this.lotroInstrument = lotroInstrument;
		this.name = name;
		this.lowestNoteId = lowestNoteId;
		this.highestNoteId = highestNoteId;
	}

	public abstract void print(PrintStream out);

	@Override public int compareTo(InstrumentInfo o)
	{
		if (o == null)
			return 1;

		if (lotroInstrument != o.lotroInstrument)
			return lotroInstrument.name().compareTo(o.lotroInstrument.name());

		return lowestNoteId - o.lowestNoteId;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof InstrumentInfo))
			return false;

		return compareTo((InstrumentInfo) obj) == 0;
	}

	@Override public int hashCode()
	{
		return lotroInstrument.hashCode() ^ lowestNoteId;
	}
}
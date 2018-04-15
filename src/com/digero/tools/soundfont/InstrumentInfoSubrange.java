package com.digero.tools.soundfont;

import java.io.PrintStream;

import com.digero.common.abc.LotroInstrument;

public class InstrumentInfoSubrange extends InstrumentInfo
{
	public InstrumentInfoSubrange(LotroInstrument instrument, InstrumentInfo base, int lowestNoteId, int hightestNoteId)
	{
		super(instrument, base.name, lowestNoteId, hightestNoteId);
	}

	@Override public void print(PrintStream out)
	{
		// Don't write anything; this is just a reference to another 
		// instrument that's already been written
	}
}

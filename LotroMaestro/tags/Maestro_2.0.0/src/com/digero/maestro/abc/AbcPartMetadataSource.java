package com.digero.maestro.abc;

import com.digero.common.abc.LotroInstrument;

public interface AbcPartMetadataSource
{
	public String getTitle();

	public int getPartNumber();

	public LotroInstrument getInstrument();
}

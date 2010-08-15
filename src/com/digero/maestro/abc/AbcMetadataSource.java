package com.digero.maestro.abc;

import com.digero.common.abc.LotroInstrument;

public interface AbcMetadataSource {
	public String getSongTitle();

	public String getTitleTag();

	public String getComposer();

	public String getTranscriber();

	public int findPartNumber(LotroInstrument instrument, int currentPartNumber);
}

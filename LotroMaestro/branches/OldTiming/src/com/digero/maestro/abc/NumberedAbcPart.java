package com.digero.maestro.abc;

import com.digero.common.abc.LotroInstrument;

public interface NumberedAbcPart
{
	public LotroInstrument getInstrument();

	public void setInstrument(LotroInstrument instrument);

	public int getPartNumber();

	public void setPartNumber(int partNumber);
}

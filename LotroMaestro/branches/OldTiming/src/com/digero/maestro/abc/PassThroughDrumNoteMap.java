package com.digero.maestro.abc;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.MidiConstants;

public class PassThroughDrumNoteMap extends DrumNoteMap
{
	@Override protected byte getDefaultMapping(byte noteId)
	{
		if (LotroInstrument.DRUMS.isPlayable(noteId))
			return noteId;
		else
			return DISABLED_NOTE_ID;
	}

	@Override public byte[] getFailsafeDefault()
	{
		byte[] failsafe = new byte[MidiConstants.NOTE_COUNT];

		for (int i = 0; i < failsafe.length; i++)
		{
			failsafe[i] = getDefaultMapping((byte) i);
		}

		return failsafe;
	}
}

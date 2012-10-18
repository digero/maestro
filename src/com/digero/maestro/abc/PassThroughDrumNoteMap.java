package com.digero.maestro.abc;

import com.digero.common.abc.LotroInstrument;

public class PassThroughDrumNoteMap extends DrumNoteMap {
	@Override
	protected byte getDefaultMapping(byte noteId) {
		if (LotroInstrument.DRUMS.isPlayable(noteId))
			return noteId;
		else
			return DISABLED_NOTE_ID;
	}
}

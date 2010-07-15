package com.digero.maestro.midi;

import java.util.EventListener;

public interface SequencerListener extends EventListener {
	public void propertyChanged(SequencerEvent evt);
}

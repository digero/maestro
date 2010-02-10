package com.digero.maestro.midi;

import java.util.EventObject;

@SuppressWarnings("serial")
public class SequencerEvent extends EventObject {
	private SequencerProperty property;
	private SequencerWrapper sequencerWrapper;

	public SequencerEvent(Object source, SequencerWrapper sequencerWrapper, SequencerProperty property) {
		super(source);
		this.sequencerWrapper = sequencerWrapper;
		this.property = property;
	}
	
	
	public SequencerWrapper getSequencerWrapper() {
		return sequencerWrapper;
	}

	public SequencerProperty getProperty() {
		return property;
	}
}

package com.digero.common.midi;

import java.util.EventObject;

@SuppressWarnings("serial")
public class SequencerEvent extends EventObject
{
	private SequencerProperty property;

	public SequencerEvent(SequencerWrapper sequencerWrapper, SequencerProperty property)
	{
		super(sequencerWrapper);
		this.property = property;
	}

	@Override public SequencerWrapper getSource()
	{
		return (SequencerWrapper) super.getSource();
	}

	public SequencerProperty getProperty()
	{
		return property;
	}
}

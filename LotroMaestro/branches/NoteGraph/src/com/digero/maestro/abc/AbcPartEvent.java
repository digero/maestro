package com.digero.maestro.abc;

import java.util.EventObject;

public class AbcPartEvent extends EventObject {
	private final boolean previewRelated;
	private final boolean isPartNumber;

	public AbcPartEvent(AbcPart source, boolean previewRelated, boolean isPartNumber) {
		super(source);
		this.previewRelated = previewRelated;
		this.isPartNumber = isPartNumber;
	}
	
	public boolean isPreviewRelated() {
		return previewRelated;
	}
	
	public boolean isPartNumber() {
		return isPartNumber;
	}

	@Override
	public AbcPart getSource() {
		return (AbcPart) super.getSource();
	}
}

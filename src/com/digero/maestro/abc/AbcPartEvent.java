package com.digero.maestro.abc;

import java.util.EventObject;

public class AbcPartEvent extends EventObject {
	private final boolean abcPreviewRelated;
	private final AbcPartProperty property;

	public AbcPartEvent(AbcPart source, AbcPartProperty property, boolean abcPreviewRelated) {
		super(source);
		this.property = property;
		this.abcPreviewRelated = abcPreviewRelated;
	}

	public AbcPartProperty getProperty() {
		return property;
	}

	public boolean isNoteGraphRelated() {
		return property.isNoteGraphRelated();
	}

	public boolean isAbcPreviewRelated() {
		return abcPreviewRelated;
	}

	@Override
	public AbcPart getSource() {
		return (AbcPart) super.getSource();
	}
}

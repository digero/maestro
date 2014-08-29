package com.digero.maestro.abc;

public enum AbcPartProperty {
	TITLE(false), //
	PART_NUMBER(false), //
	ENABLED, //
	INSTRUMENT, //
	BASE_TRANSPOSE, //
	TRACK_ENABLED, //
	TRACK_TRANSPOSE, //
	DRUM_ENABLED, //
	DRUM_MAPPING, //
	VOLUME_ADJUST; //

	private final boolean renderRelated;

	private AbcPartProperty() {
		this.renderRelated = true;
	}

	private AbcPartProperty(boolean renderRelated) {
		this.renderRelated = renderRelated;
	}

	public boolean isNoteGraphRelated() {
		return renderRelated;
	}

	public boolean isAbcPreviewRelated() {
		return renderRelated;
	}
}

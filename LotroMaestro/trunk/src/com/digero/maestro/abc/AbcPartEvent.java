package com.digero.maestro.abc;

import java.util.EventObject;

public class AbcPartEvent extends EventObject {
	public static final int NO_TRACK_NUMBER = -1;

	private final boolean abcPreviewRelated;
	private final AbcPartProperty property;
	private final int trackNumber;

	public AbcPartEvent(AbcPart source, AbcPartProperty property, boolean abcPreviewRelated, int trackNumber) {
		super(source);
		this.property = property;
		this.abcPreviewRelated = abcPreviewRelated;
		this.trackNumber = trackNumber;
	}

	public AbcPartProperty getProperty() {
		return property;
	}

	public boolean hasTrackNumber() {
		return trackNumber != NO_TRACK_NUMBER;
	}

	public int getTrackNumber() {
		return trackNumber;
	}

	public boolean matchesTrack(int track) {
		return !hasTrackNumber() || trackNumber == track;
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

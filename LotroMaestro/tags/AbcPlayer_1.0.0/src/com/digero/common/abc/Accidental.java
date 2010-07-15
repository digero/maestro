package com.digero.common.abc;

public enum Accidental {
	NONE("", 0), DOUBLE_FLAT("__", -2), FLAT("_", -1), NATURAL("=", 0), SHARP("^", 1), DOUBLE_SHARP("^^", 2);

	public final String abc;
	public final int deltaNoteId;

	private Accidental(String abc, int deltaNoteId) {
		this.abc = abc;
		this.deltaNoteId = deltaNoteId;
	}

	@Override
	public String toString() {
		return abc;
	}
}

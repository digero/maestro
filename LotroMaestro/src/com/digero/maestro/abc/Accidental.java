package com.digero.maestro.abc;

public enum Accidental {
	NONE(""), FLAT("_"), NATURAL("="), SHARP("^");

	public final String abc;

	private Accidental(String abc) {
		this.abc = abc;
	}

	@Override
	public String toString() {
		return abc;
	}
}

package com.digero.common.view;

import java.awt.Color;

public enum ColorTable {
	NOTE_ON(Color.WHITE), //
	NOTE_ON_BORDER(new Color(0xAA000000, true)), //
	INDICATOR(new Color(0x66FFFFFF, true)), //
	INDICATOR_ACTIVE(new Color(0xAAFFFFFF, true)), //

	NOTE_ENABLED /*      */(Color.getHSBColor(0.62f, 0.85f, 1.00f)), //
	NOTE_DISABLED /*     */(Color.getHSBColor(0.60f, 0.80f, 0.80f)), //
	NOTE_OFF /*          */(Color.getHSBColor(0.62f, 0.00f, 0.50f)), //

	NOTE_BAD_ENABLED /*  */(Color.getHSBColor(0.05f, 1.00f, 1.00f)), //
	NOTE_BAD_DISABLED /* */(Color.getHSBColor(0.95f, 0.65f, 0.75f)), //
	NOTE_BAD_OFF /*      */(Color.getHSBColor(0.00f, 0.00f, 0.70f)), //

	NOTE_ABC_ENABLED /*  */(Color.getHSBColor(0.12f, 0.85f, 1.00f)), //
	NOTE_ABC_DISABLED /* */(Color.getHSBColor(0.12f, 0.60f, 0.75f)), //
	NOTE_ABC_OFF /*      */(Color.getHSBColor(0.12f, 0.00f, 0.50f)), //

//	NOTE_ABC_ENABLED(new Color(0x99FFBB00, true)), //
//	NOTE_ABC_ENABLED(NOTE_ENABLED), //
//	NOTE_ABC_DISABLED(NOTE_DISABLED), //
//	NOTE_ABC_OFF(NOTE_OFF), //

	NOTE_DRUM_ENABLED(NOTE_ENABLED), //
	NOTE_DRUM_DISABLED(NOTE_DISABLED), //
	NOTE_DRUM_OFF(NOTE_OFF), //

	GRAPH_BACKGROUND_ENABLED(Color.BLACK), //
	GRAPH_BACKGROUND_DISABLED(new Color(0x222222)), //
	GRAPH_BACKGROUND_OFF(new Color(0x222222)), //

	GRAPH_BORDER_ENABLED(Color.DARK_GRAY), //
	GRAPH_BORDER_DISABLED(Color.DARK_GRAY), //
	GRAPH_BORDER_OFF(Color.DARK_GRAY), //

	PANEL_BACKGROUND_ENABLED(GRAPH_BACKGROUND_ENABLED), //
	PANEL_BACKGROUND_DISABLED(GRAPH_BACKGROUND_DISABLED), //

	PANEL_BORDER(new Color(0xEEEEEE)), //

	PANEL_TEXT_ENABLED(Color.getHSBColor(0.28f, 0.70f, 1.00f)), //
	PANEL_TEXT_DISABLED(Color.WHITE), //
	PANEL_TEXT_OFF(new Color(0x777777)), //

	CONTROLS_TEXT(Color.WHITE), //
	CONTROLS_BACKGROUND(new Color(0x222222));

	private Color value;

	private ColorTable(Color value) {
		this.value = value;
	}

	private ColorTable(ColorTable copyFrom) {
		this.value = copyFrom.value;
	}

	public void set(Color value) {
		this.value = value;
	}

	public Color get() {
		return this.value;
	}
}

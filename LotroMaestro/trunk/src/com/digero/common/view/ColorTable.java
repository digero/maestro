package com.digero.common.view;

import java.awt.Color;

// Disable auto-formatting in this file
// @formatter:off

public enum ColorTable
{
	NOTE_ON(Color.WHITE),
	NOTE_ON_BORDER(new Color(0xAA000000, true)),
	INDICATOR(new Color(0x66FFFFFF, true)),
	INDICATOR_ACTIVE(new Color(0xAAFFFFFF, true)),
	OCTAVE_LINE(new Color(0xAA3C3C3C, true)),
	BAR_LINE(new Color(0xAA3C3C3C, true)),
	LINK(new Color(0x336699)),

	NOTE_ENABLED     (Color.getHSBColor(0.61f, 0.75f, 1.00f)),
	NOTE_DISABLED    (Color.getHSBColor(0.60f, 0.67f, 0.95f)),
	NOTE_OFF         (Color.getHSBColor(0.62f, 0.00f, 0.50f)),

	NOTE_BAD_ENABLED (Color.getHSBColor(0.05f, 1.00f, 1.00f)),
	NOTE_BAD_DISABLED(Color.getHSBColor(0.95f, 0.65f, 0.75f)),
	NOTE_BAD_OFF     (Color.getHSBColor(0.00f, 0.00f, 0.70f)),

	NOTE_ABC_ENABLED (Color.getHSBColor(0.12f, 0.77f, 0.90f)),
	NOTE_ABC_DISABLED(Color.getHSBColor(0.12f, 0.60f, 0.75f)),
	NOTE_ABC_OFF     (Color.getHSBColor(0.12f, 0.00f, 0.50f)),

	NOTE_TEMPO       (new Color(0x999999)),
	NOTE_TEMPO_ON    (new Color(0xF2F2F2)),

	NOTE_DRUM_ENABLED(NOTE_ENABLED),
	NOTE_DRUM_DISABLED(NOTE_DISABLED),
	NOTE_DRUM_OFF(NOTE_OFF),

	GRAPH_BACKGROUND_ENABLED(Color.BLACK),
	GRAPH_BACKGROUND_DISABLED(new Color(0x222222)),
	GRAPH_BACKGROUND_OFF(new Color(0x222222)),

	GRAPH_BORDER_ENABLED(Color.DARK_GRAY),
	GRAPH_BORDER_DISABLED(Color.DARK_GRAY),
	GRAPH_BORDER_OFF(Color.DARK_GRAY),

	PANEL_BACKGROUND_ENABLED(GRAPH_BACKGROUND_ENABLED),
	PANEL_BACKGROUND_DISABLED(GRAPH_BACKGROUND_DISABLED),

	PANEL_BORDER(new Color(0xEEEEEE)),

	PANEL_HIGHLIGHT(new Color(0xFFD83C)), //(Color.getHSBColor(0.60f, 0.50f, 1.00f)),
	PANEL_HIGHLIGHT_OTHER_PART(new Color(0xDDDDDD)),

	PANEL_TEXT_ENABLED(new Color(0xFFD83C)), //(Color.getHSBColor(0.60f, 0.40f, 1.00f)),
	PANEL_TEXT_DISABLED(new Color(0xEEEEEE)),
	PANEL_TEXT_OFF(new Color(0x777777)),
	PANEL_TEXT_ERROR(Color.getHSBColor(0.01f, 0.98f, 1.00f)),
	PANEL_LINK(Color.getHSBColor(0.60f, 0.70f, 1.00f)),

	ABC_BORDER_SELECTED_ENABLED(PANEL_TEXT_ENABLED),
	ABC_BORDER_SELECTED_OFF(PANEL_TEXT_ENABLED),
	ABC_BORDER_UNSELECTED_ENABLED(GRAPH_BACKGROUND_ENABLED),
	ABC_BORDER_UNSELECTED_OFF(GRAPH_BACKGROUND_OFF),

	CONTROLS_TEXT(Color.WHITE),
	CONTROLS_BACKGROUND(new Color(0x222222));

	private Color value;

	private ColorTable(Color value)
	{
		this.value = value;
	}

	private ColorTable(ColorTable copyFrom)
	{
		this.value = copyFrom.value;
	}

	public void set(Color value)
	{
		this.value = value;
	}

	public Color get()
	{
		return this.value;
	}

	public String getHtml()
	{
		return String.format("#%02X%02X%02X", value.getRed(), value.getGreen(), value.getBlue());
	}
}

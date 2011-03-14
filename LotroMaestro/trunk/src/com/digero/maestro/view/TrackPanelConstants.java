package com.digero.maestro.view;

import java.awt.Color;

public interface TrackPanelConstants {
	public static final Color NOTE_ON = Color.WHITE;
	public static final Color INDICATOR_COLOR = new Color(0xAAFFFFFF, true);

	public static final Color NOTE_ENABLED /*      */= Color.getHSBColor(0.62f, 0.90f, 1.00f);
	public static final Color NOTE_DISABLED /*     */= Color.getHSBColor(0.62f, 0.70f, 0.80f);
	public static final Color NOTE_OFF /*          */= Color.getHSBColor(0.62f, 0.00f, 0.50f);

	public static final Color NOTE_BAD_ENABLED /*  */= Color.getHSBColor(0.05f, 1.00f, 1.00f);
	public static final Color NOTE_BAD_DISABLED /* */= Color.getHSBColor(0.95f, 0.65f, 0.75f);
	public static final Color NOTE_BAD_OFF /*      */= Color.getHSBColor(0.00f, 0.00f, 0.70f);

	public static final Color NOTE_DRUM_ENABLED /* */= NOTE_ENABLED;// Color.getHSBColor(0.28f, 0.60f, 1.00f);
	public static final Color NOTE_DRUM_DISABLED /**/= NOTE_DISABLED;//Color.getHSBColor(0.28f, 0.30f, 0.90f);
	public static final Color NOTE_DRUM_OFF /*     */= NOTE_OFF;

	public static final Color GRAPH_BACKGROUND_ENABLED = Color.BLACK;
	public static final Color GRAPH_BACKGROUND_DISABLED = new Color(0x333333);
	public static final Color GRAPH_BACKGROUND_OFF = new Color(0x333333);

	public static final Color GRAPH_BORDER_ENABLED = Color.DARK_GRAY;
	public static final Color GRAPH_BORDER_DISABLED = Color.DARK_GRAY;
	public static final Color GRAPH_BORDER_OFF = Color.DARK_GRAY;

	public static final Color PANEL_BACKGROUND_ENABLED = GRAPH_BACKGROUND_ENABLED;//new Color(0xEEEEEE);
	public static final Color PANEL_BACKGROUND_DISABLED = GRAPH_BACKGROUND_DISABLED;

	public static final Color PANEL_BORDER = new Color(0xEEEEEE);

	public static final Color PANEL_TEXT_ENABLED = Color.getHSBColor(0.28f, 0.60f, 1.00f);//Color.YELLOW;
	public static final Color PANEL_DRUM_TEXT_ENABLED = PANEL_TEXT_ENABLED;//Color.getHSBColor(0.14f, 0.20f, 1.00f);
	public static final Color PANEL_TEXT_DISABLED = Color.WHITE;//new Color(0xE0E0E0);
	public static final Color PANEL_TEXT_OFF = new Color(0x777777);

	public static final boolean GRAPH_HAS_BORDER = false;
	public static final int GRAPH_BORDER_SIZE = 2;
	public static final int GRAPH_BORDER_ROUNDED = 0; // 5
	public static final double NOTE_WIDTH_PX = 3;
	public static final double NOTE_HEIGHT_PX = 2;
}

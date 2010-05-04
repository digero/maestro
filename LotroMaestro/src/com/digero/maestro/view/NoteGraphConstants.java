package com.digero.maestro.view;

import java.awt.Color;

import com.digero.maestro.util.Util;

public interface NoteGraphConstants {
	public static final Color NOTE = new Color(29, 95, 255);
	public static final Color XNOTE = Color.RED;
	public static final Color NOTE_DISABLED = Util.grayscale(NOTE).darker(); // Color.GRAY;
	public static final Color XNOTE_DISABLED = Util.grayscale(XNOTE); // Color.LIGHT_GRAY;
	public static final Color NOTE_DRUM = new Color(239, 228, 176);
	public static final Color NOTE_DRUM_DISABLED = Util.grayscale(NOTE_DRUM).darker();
	public static final Color NOTE_ON = Color.WHITE;
	public static final Color BKGD_COLOR = Color.BLACK;
	public static final Color BKGD_DISABLED = Color.DARK_GRAY.darker();
	public static final Color BORDER_COLOR = Color.DARK_GRAY;
	public static final Color BORDER_DISABLED = Color.DARK_GRAY;
	public static final Color INDICATOR_COLOR = new Color(0xAAFFFFFF, true);

	public static final int BORDER_SIZE = 2;
	public static final double NOTE_WIDTH_PX = 3;
	public static final double NOTE_HEIGHT_PX = 2;
}

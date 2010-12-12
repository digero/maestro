package com.digero.maestro.view;

import java.awt.Color;

public interface NoteGraphConstants {
	public static final Color NOTE /*         */= new Color(Color.HSBtoRGB(0.62f, 0.90f, 1.00f)); //new Color(29, 95, 255);
	public static final Color NOTE_DISABLED /**/= new Color(Color.HSBtoRGB(0.62f, 0.50f, 0.60f)); //new Color(48, 72, 143);
	public static final Color NOTE_OFF /*     */= new Color(Color.HSBtoRGB(0.62f, 0.00f, 0.40f)); //new Color(0x555555);

	public static final Color NOTE_BAD /*     */= new Color(Color.HSBtoRGB(0.00f, 1.00f, 1.00f));
	public static final Color NOTE_BAD_DISABLED = new Color(Color.HSBtoRGB(0.00f, 0.50f, 0.60f)); //new Color(143, 48, 48);
	public static final Color NOTE_BAD_OFF /* */= new Color(Color.HSBtoRGB(0.00f, 0.00f, 0.60f)); //new Color(0x888888);

	public static final Color NOTE_DRUM = new Color(239, 228, 176);
	public static final Color NOTE_DRUM_DISABLED = new Color(215, 213, 200); //Util.grayscale(NOTE_DRUM).darker();
	public static final Color NOTE_DRUM_OFF = new Color(0x888888);//Util.grayscale(NOTE_DRUM).darker();

	public static final Color BKGD_COLOR = Color.BLACK;
	public static final Color BKGD_DISABLED = new Color(0x222222);
	public static final Color BKGD_OFF = new Color(0x333333);

	public static final Color BORDER_COLOR = Color.DARK_GRAY;
	public static final Color BORDER_DISABLED = Color.DARK_GRAY;
	public static final Color BORDER_OFF = Color.DARK_GRAY;

	public static final Color NOTE_ON = Color.WHITE;
	public static final Color INDICATOR_COLOR = new Color(0xAAFFFFFF, true);

	public static final int BORDER_SIZE = 2;
	public static final double NOTE_WIDTH_PX = 3;
	public static final double NOTE_HEIGHT_PX = 2;
}

package com.digero.maestro.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Util {
	private Util() {
		// Can't instantiate class
	}

	public static Color grayscale(Color orig) {
		float[] hsb = Color.RGBtoHSB(orig.getRed(), orig.getGreen(), orig.getBlue(), null);
		return Color.getHSBColor(0.0f, 0.0f, hsb[2]);
	}

	public static final String ELLIPSIS = "...";

	@SuppressWarnings("deprecation")
	public static String ellipsis(String text, float maxWidth, Font font) {
		FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
		Pattern prevWord = Pattern.compile("\\w*\\W*$");
		Matcher matcher = prevWord.matcher(text);

		float width = metrics.stringWidth(text);
		if (width < maxWidth)
			return text;

		int len = 0;
		int seg = text.length();
		String fit = "";

		// find the longest string that fits into
		// the control boundaries using bisection method 
		while (seg > 1) {
			seg -= seg / 2;

			int left = len + seg;
			int right = text.length();

			if (left > right)
				continue;

			// trim at a word boundary using regular expressions 
			matcher.region(0, left);
			if (matcher.find())
				left = matcher.start();

			// build and measure a candidate string with ellipsis
			String tst = text.substring(0, left) + ELLIPSIS;

			width = metrics.stringWidth(tst);

			// candidate string fits into boundaries, try a longer string
			// stop when seg <= 1
			if (width <= maxWidth) {
				len += seg;
				fit = tst;
			}
		}

		// string can't fit
		if (len == 0)
			return ELLIPSIS;

		return fit;
	}
}

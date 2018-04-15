package com.digero.common.midi;

import java.util.Arrays;
import java.util.regex.Pattern;

import com.digero.common.abc.LotroInstrument;

public class PanGenerator
{
	public static final int CENTER = 64;

	private int[] count;

	public PanGenerator()
	{
		count = new int[LotroInstrument.values().length];
	}

	public void reset()
	{
		Arrays.fill(count, 0);
	}

	static final Pattern leftRegex = Pattern.compile("\\b(left)\\b");
	static final Pattern rightRegex = Pattern.compile("\\b(right)\\b");
	static final Pattern centerRegex = Pattern.compile("\\b(middle|center)\\b");

	public int get(LotroInstrument instrument, String partTitle)
	{
		int pan = get(instrument);

		String titleLower = partTitle.toLowerCase();
		if (leftRegex.matcher(titleLower).find())
			pan = CENTER - Math.abs(pan - CENTER);
		else if (rightRegex.matcher(titleLower).find())
			pan = CENTER + Math.abs(pan - CENTER);
		else if (centerRegex.matcher(titleLower).find())
			pan = CENTER;

		return pan;
	}

	public int get(LotroInstrument instrument)
	{
		switch (instrument)
		{
			case BASIC_LUTE:
				instrument = LotroInstrument.LUTE_OF_AGES;
				break;
			case SPRIGHTLY_FIDDLE:
			case MISTY_MOUNTAIN_HARP:
				instrument = LotroInstrument.HARP;
				break;
			case MOOR_COWBELL:
				instrument = LotroInstrument.COWBELL;
				break;
			case STUDENT_FIDDLE:
			case LONELY_MOUNTAIN_FIDDLE:
				instrument = LotroInstrument.BASIC_FIDDLE;
				break;
			//$CASES-OMITTED$
			default:
				break;
		}

		int sign;
		int c = count[instrument.ordinal()]++;

		switch (c % 3)
		{
			case 0:
				sign = 1;
				break;
			case 1:
				sign = -1;
				break;
			default:
				sign = 0;
				break;
		}

		switch (instrument)
		{
			case BASIC_FIDDLE:
			case LONELY_MOUNTAIN_FIDDLE:
			case STUDENT_FIDDLE:
				return CENTER + sign * -50;
			case HARP:
			case MISTY_MOUNTAIN_HARP:
			case SPRIGHTLY_FIDDLE:
				return CENTER + sign * -45;
			case FLUTE:
				return CENTER + sign * -40;
			case BAGPIPE:
				return CENTER + sign * -30;
			case THEORBO:
				return CENTER + sign * -25;
			case COWBELL:
			case MOOR_COWBELL:
				return CENTER + sign * -15;
			case DRUMS:
				return CENTER + sign * 15;
			case PIBGORN:
				return CENTER + sign * 20;
			case HORN:
				return CENTER + sign * 25;
			case BASIC_LUTE:
			case LUTE_OF_AGES:
				return CENTER + sign * 35;
			case CLARINET:
				return CENTER + sign * 45;
		}

		return CENTER;
	}
}

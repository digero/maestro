package com.digero.common.midi;

import java.util.Arrays;

import com.digero.common.abc.LotroInstrument;

public class PanGenerator {
	public static final int CENTER = 64;

	private int[] count;

	public PanGenerator() {
		count = new int[LotroInstrument.values().length];
	}

	public void reset() {
		Arrays.fill(count, 0);
	}

	public int get(LotroInstrument instrument, String partTitle) {
		int pan = get(instrument);
		
		String titleLower = partTitle.toLowerCase();
		if (titleLower.contains("left"))
			pan = CENTER - Math.abs(pan - CENTER);
		else if (titleLower.contains("right"))
			pan = CENTER + Math.abs(pan - CENTER);
		else if (titleLower.contains("center") || titleLower.contains("middle"))
			pan = CENTER;
		
		return pan;
	}

	public int get(LotroInstrument instrument) {
		if (instrument == LotroInstrument.MOOR_COWBELL)
			instrument = LotroInstrument.COWBELL;

		int sign;
		int c = count[instrument.ordinal()]++;

		switch (c % 3) {
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

		switch (instrument) {
		case HARP:
			return CENTER + sign * -45;
		case FLUTE:
			return CENTER + sign * -40;
		case BAGPIPE:
			return CENTER + sign * -30;
		case THEORBO:
			return CENTER + sign * -25;
		case COWBELL:
			return CENTER + sign * -15;
		case DRUMS:
			return CENTER + sign * 15;
		case HORN:
			return CENTER + sign * 25;
		case LUTE:
			return CENTER + sign * 35;
		case CLARINET:
			return CENTER + sign * 45;
		default:
			return CENTER;
		}
	}
}

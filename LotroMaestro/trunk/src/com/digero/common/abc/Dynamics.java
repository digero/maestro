package com.digero.common.abc;

public enum Dynamics {
	// In LotRO, +pppp+ seems to be 50% volume
	pppp(8, 64), //
	ppp(16, 71), //
	pp(32, 78), //
	p(48, 85), //
	mp(64, 92), //
	mf(80, 100), //
	f(96, 106), //
	ff(112, 113), //
	fff(127, 120), //
	ffff(127, 127);

	public static final Dynamics DEFAULT = mf;
	public static final Dynamics MAXIMUM = ffff;
	public static final Dynamics MINIMUM = pppp;

	public static final Dynamics fromMidiVelocity(int velocity) {
		Dynamics[] values = values();
		Dynamics best = values[0];
		for (int i = 1; i < values.length; i++) {
			if (Math.abs(velocity - values[i].midiVol) < Math.abs(velocity - best.midiVol))
				best = values[i];
			else
				break;
		}
		return best;
	}

	public final int midiVol;
	public final int abcVol;

	public int getVol(boolean abc) {
		return abc ? abcVol : midiVol;
	}

	private Dynamics(int midiVol, int abcVol) {
		this.midiVol = midiVol;
		this.abcVol = abcVol;
	}
}
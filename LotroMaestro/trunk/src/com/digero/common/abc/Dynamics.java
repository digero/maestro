package com.digero.common.abc;

public enum Dynamics {
	// In LotRO, +ppp+ seems to be 50% volume
	ppp(16, 64), pp(33, 73), p(49, 82), mp(64, 91), mf(80, 100), f(112, 109), ff(112, 118), fff(127, 127);

	public static final Dynamics DEFAULT = mf;

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

	private Dynamics(int midiVol, int abcVol) {
		this.midiVol = midiVol;
		this.abcVol = abcVol;
	}
}
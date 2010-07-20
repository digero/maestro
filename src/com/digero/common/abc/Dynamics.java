package com.digero.common.abc;

public enum Dynamics {
	// In LotRO, +ppp+ seems to be 50% volume
	ppp(64), pp(73), p(82), mp(91), mf(100), f(109), ff(118), fff(127);

	public static final Dynamics DEFAULT = mf;

	public static final Dynamics fromVelocity(int velocity) {
		Dynamics[] values = values();
		Dynamics best = values[0];
		for (int i = 1; i < values.length; i++) {
			if (Math.abs(velocity - values[i].velocity) < Math.abs(velocity - best.velocity))
				best = values[i];
			else
				break;
		}
		return best;
	}

	public final int velocity;

	private Dynamics(int velocity) {
		this.velocity = velocity;
	}
}
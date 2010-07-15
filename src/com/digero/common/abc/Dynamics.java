package com.digero.common.abc;

public enum Dynamics {
	// Velocities referenced form http://en.wikipedia.org/wiki/Dynamics_(music)
	ppp(16), pp(33), p(49), mp(64), mf(80), f(96), ff(112), fff(126);

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
package com.digero.common.abc;

import com.digero.common.midi.MidiConstants;
import com.digero.common.util.Util;

public enum Dynamics
{
	pppp(4, 57), //
	ppp(16, 61), //
	pp(32, 75), //
	p(48, 87), //
	mp(64, 97), //
	mf(80, 106), //
	f(96, 115), //
	ff(112, 123), //
	fff(127, 127), //
	ffff(144, 127);

	public static final Dynamics DEFAULT = mf;
	public static final Dynamics MAXIMUM = ffff;
	public static final Dynamics MINIMUM = pppp;

	public static final Dynamics fromMidiVelocity(int velocity)
	{
		Dynamics[] values = values();
		Dynamics best = values[0];
		int deltaBest = Math.abs(velocity - values[0].midiVol);
		for (int i = 1; i < values.length; i++)
		{
			int delta = Math.abs(velocity - values[i].midiVol);
			if (delta < deltaBest)
			{
				best = values[i];
				deltaBest = delta;
			}
			else
			{
				break;
			}
		}
		return best;
	}

	public final int midiVol;
	public final int abcVol;

	public int getVol(boolean abc)
	{
		return Util.clamp(abc ? abcVol : midiVol, 0, MidiConstants.MAX_VOLUME);
	}

	private Dynamics(int midiVol, int abcVol)
	{
		this.midiVol = midiVol;
		this.abcVol = abcVol;
	}
}
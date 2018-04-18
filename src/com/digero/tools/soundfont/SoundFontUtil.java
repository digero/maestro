package com.digero.tools.soundfont;

import com.digero.common.util.Util;

public final class SoundFontUtil
{
	private SoundFontUtil()
	{
	}

	/** Converts decibels to the unit used by soundfont */
	public static int dBToAttenuationValue(float db)
	{
		int attenuation = (int) Math.round(-db * 10);
		if (attenuation < 0)
			attenuation += 65536;

		return attenuation;
	}

	/** Converts seconds to the timecent unit used by soundfont */
	public static int secondsToTimecents(double seconds)
	{
		if (seconds < 0.001)
			return -12000;

		return Util.clamp((int) (1200 * Math.log(seconds) / Math.log(2.0)), -12000, 8000);
	}

}

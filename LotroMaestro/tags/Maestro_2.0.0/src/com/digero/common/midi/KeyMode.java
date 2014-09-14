package com.digero.common.midi;

public enum KeyMode
{
	MAJOR, MINOR, DORIAN, PHRYGIAN, LYDIAN, MIXOLYDIAN, AEOLIAN, IONIAN, LOCRIAN;

	public static KeyMode parseMode(String modeString)
	{
		if (modeString.equals("") || modeString.equals("M"))
			return MAJOR;
		if (modeString.equals("m"))
			return MINOR;

		if (modeString.length() > 3)
		{
			modeString = modeString.substring(0, 3);
		}
		modeString = modeString.toUpperCase();
		for (KeyMode mode : values())
		{
			if (mode.toShortString().equals(modeString))
				return mode;
		}
		return null;
	}

	public String toShortString()
	{
		return super.toString().substring(0, 3);
	}
}

/* Copyright (c) 2010 Ben Howell
 * This software is licensed under the MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package com.digero.common.midi;

import java.util.HashMap;
import java.util.Map;

public enum Note
{
	REST(-1), //
	CX, CsX, DbX(CsX), DX, DsX, EbX(DsX), EX, FX, FsX, GbX(FsX), GX, GsX, AbX(GsX), AX, AsX, BbX(AsX), BX, //
	C0, Cs0, Db0(Cs0), D0, Ds0, Eb0(Ds0), E0, F0, Fs0, Gb0(Fs0), G0, Gs0, Ab0(Gs0), A0, As0, Bb0(As0), B0, //
	C1, Cs1, Db1(Cs1), D1, Ds1, Eb1(Ds1), E1, F1, Fs1, Gb1(Fs1), G1, Gs1, Ab1(Gs1), A1, As1, Bb1(As1), B1, //
	C2, Cs2, Db2(Cs2), D2, Ds2, Eb2(Ds2), E2, F2, Fs2, Gb2(Fs2), G2, Gs2, Ab2(Gs2), A2, As2, Bb2(As2), B2, //
	C3, Cs3, Db3(Cs3), D3, Ds3, Eb3(Ds3), E3, F3, Fs3, Gb3(Fs3), G3, Gs3, Ab3(Gs3), A3, As3, Bb3(As3), B3, //
	C4, Cs4, Db4(Cs4), D4, Ds4, Eb4(Ds4), E4, F4, Fs4, Gb4(Fs4), G4, Gs4, Ab4(Gs4), A4, As4, Bb4(As4), B4, //
	C5, Cs5, Db5(Cs5), D5, Ds5, Eb5(Ds5), E5, F5, Fs5, Gb5(Fs5), G5, Gs5, Ab5(Gs5), A5, As5, Bb5(As5), B5, //
	C6, Cs6, Db6(Cs6), D6, Ds6, Eb6(Ds6), E6, F6, Fs6, Gb6(Fs6), G6, Gs6, Ab6(Gs6), A6, As6, Bb6(As6), B6, //
	C7, Cs7, Db7(Cs7), D7, Ds7, Eb7(Ds7), E7, F7, Fs7, Gb7(Fs7), G7, Gs7, Ab7(Gs7), A7, As7, Bb7(As7), B7, //
	C8, Cs8, Db8(Cs8), D8, Ds8, Eb8(Ds8), E8, F8, Fs8, Gb8(Fs8), G8, Gs8, Ab8(Gs8), A8, As8, Bb8(As8), B8, //
	C9, Cs9, Db9(Cs9), D9, Ds9, Eb9(Ds9), E9, F9, Fs9, Gb9(Fs9), G9, Gs9, Ab9(Gs9), A9, As9, Bb9(As9), B9;

	public static final Note MIN = CX;
	public static final Note MAX = B9;
	public static final Note MIN_PLAYABLE = C2;
	public static final Note MAX_PLAYABLE = C5;

	/** The MIDI ID for this note. */
	public final int id;
	/** The ABC notation for this file. */
	public final String abc;
	/** The ID of the natural of this note, if it's sharp or flat */
	public final int naturalId;
	public final int octave;

	public boolean isSharp()
	{
		return id > naturalId;
	}

	public boolean isFlat()
	{
		return id < naturalId;
	}

	public Note getEnharmonicNote(boolean sharp)
	{
		if (isWhiteKey() || sharp == isSharp())
			return this;

		// Sharps are listed before flats
		return values()[ordinal() + (sharp ? -1 : 1)];
	}

	/**
	 * Returns true if this note has no accidentals in the key of C major.
	 */
	public boolean isWhiteKey()
	{
		return id == naturalId;
	}

	public static boolean isPlayable(Note n)
	{
		return isPlayable(n.id);
	}

	public static boolean isPlayable(int id)
	{
		return id >= MIN_PLAYABLE.id && id <= MAX_PLAYABLE.id;
	}

	public static Note fromId(int id)
	{
		if (id == REST.id)
			return REST;

		if (lookupId == null)
		{
			lookupId = new Note[B9.id + 1];
			for (Note n : values())
			{
				if (n != REST && lookupId[n.id] == null)
					lookupId[n.id] = n;
			}
		}

		if (id < 0 || id >= lookupId.length)
		{
			return null;
		}
		return lookupId[id];
	}

	public static Note fromAbc(String abc)
	{
		if (lookupAbc == null)
		{
			lookupAbc = new HashMap<String, Note>(values().length * 4 / 3 + 1);
			for (Note n : values())
			{
				lookupAbc.put(n.abc, n);
			}
		}

		return lookupAbc.get(abc);
	}

	public static Note fromName(String name)
	{
		return Enum.valueOf(Note.class, name);
	}

	private static Note[] lookupId = null;
	private static Map<String, Note> lookupAbc = null;

	private static class IdGenerator
	{
		private static int next = 0;
	}

	private Note()
	{
		this(IdGenerator.next);
	}

	private Note(Note copyFrom)
	{
		this(copyFrom.id);
	}

	private Note(int id)
	{
		this.id = id;
		IdGenerator.next = id + 1;

		if (id == -1)
		{
			this.abc = "z";
			naturalId = id;
			octave = 0;
		}
		else
		{
			String s = toString();
			String octaveString = s.substring(s.length() - 1);
			if (octaveString.equals("X"))
				octave = -1;
			else
				octave = Integer.parseInt(octaveString);
			StringBuilder abc = new StringBuilder(2 + Math.abs(octave - 3));

			if (s.indexOf('s') == 1)
			{
				abc.append('^');
				naturalId = id - 1;
			}
			else if (s.indexOf('b') == 1)
			{
				abc.append('_');
				naturalId = id + 1;
			}
			else
			{
				//abc.append('=');
				naturalId = id;
			}

			if (octave <= 3)
				abc.append(Character.toUpperCase(s.charAt(0)));
			else
				abc.append(Character.toLowerCase(s.charAt(0)));

			// Add commas for octaves below 3
			for (int c = octave; c < 3; c++)
				abc.append(',');

			// Add apostrophes for octaves above 4
			for (int c = octave; c > 4; c--)
				abc.append('\'');

			this.abc = abc.toString();
		}
	}

	public String getDisplayName()
	{
		return toString().replace('s', '#');
	}
}
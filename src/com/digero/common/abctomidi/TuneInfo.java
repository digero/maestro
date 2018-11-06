package com.digero.common.abctomidi;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.digero.common.abc.Dynamics;
import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.TimeSignature;

class TuneInfo
{
	private int partNumber;
	private String title;
	private boolean titleIsFromExtendedInfo;
	private KeySignature key;
	private long ppqn;
	private int primaryTempoBPM;
	private NavigableMap<Long, Integer> curPartTempoMap = new TreeMap<Long, Integer>(); // Tick -> BPM
	private NavigableMap<Long, Integer> allPartsTempoMap = new TreeMap<Long, Integer>(); // Tick -> BPM
	private LotroInstrument instrument;
	private boolean instrumentSet;
	private Dynamics dynamics;
	private boolean compoundMeter;
	private int meterNumerator;
	private int meterDenominator;

	public TuneInfo()
	{
		partNumber = 0;
		title = "";
		titleIsFromExtendedInfo = false;
		key = KeySignature.C_MAJOR;
		meterNumerator = 4;
		meterDenominator = 4;
		ppqn = 8 * AbcToMidi.DEFAULT_NOTE_TICKS / meterDenominator;
		primaryTempoBPM = 120;
		instrument = LotroInstrument.DEFAULT_INSTRUMENT;
		instrumentSet = false;
		dynamics = Dynamics.mf;
		compoundMeter = false;
	}

	public void newPart(int partNumber)
	{
		this.partNumber = partNumber;
		instrument = LotroInstrument.DEFAULT_INSTRUMENT;
		instrumentSet = false;
		dynamics = Dynamics.mf;
		title = "";
		titleIsFromExtendedInfo = false;
		curPartTempoMap.clear();
	}

	public void setTitle(String title, boolean fromExtendedInfo)
	{
		if (fromExtendedInfo || !titleIsFromExtendedInfo)
		{
			this.title = title;
			titleIsFromExtendedInfo = fromExtendedInfo;
		}
	}

	public void setKey(String str)
	{
		this.key = new KeySignature(str);
	}

	public void setNoteDivisor(String str)
	{
		this.ppqn = parseDivisor(str) * AbcToMidi.DEFAULT_NOTE_TICKS / meterDenominator;
	}

	public void setMeter(String str)
	{
		str = str.trim();
		if (str.equals("C"))
		{
			meterNumerator = 4;
			meterDenominator = 4;
		}
		else if (str.equals("C|"))
		{
			meterNumerator = 2;
			meterDenominator = 2;
		}
		else
		{
			String[] parts = str.split("[/:| ]");
			if (parts.length != 2)
			{
				throw new IllegalArgumentException("The string: \"" + str
						+ "\" is not a valid time signature (expected format: 4/4)");
			}
			meterNumerator = Integer.parseInt(parts[0]);
			meterDenominator = Integer.parseInt(parts[1]);
		}

		this.ppqn = ((4 * meterNumerator / meterDenominator) < 3 ? 16 : 8) * AbcToMidi.DEFAULT_NOTE_TICKS
				/ meterDenominator;
		this.compoundMeter = (meterNumerator % 3) == 0;
	}

	public TimeSignature getMeter()
	{
		try
		{
			return new TimeSignature(meterNumerator, meterDenominator);
		}
		catch (IllegalArgumentException e)
		{
			return TimeSignature.FOUR_FOUR;
		}
	}

	private int parseTempo(String str)
	{
		try
		{
			// Apparently LotRO ignores the tempo note length (e.g. Q: 1/4=120)
			String[] parts = str.split("=");
			int bpm;
			if (parts.length == 1)
			{
				bpm = Integer.parseInt(parts[0]);
			}
			else if (parts.length == 2)
			{
				bpm = Integer.parseInt(parts[1]);
			}
			else
			{
				throw new IllegalArgumentException("Unable to read tempo");
			}

			if (bpm < 1 || bpm > 10000)
				throw new IllegalArgumentException("Tempo \"" + bpm + "\" is out of range (expected 1-10000)");

			return bpm;
		}
		catch (NumberFormatException nfe)
		{
			throw new IllegalArgumentException("Unable to read tempo");
		}
	}

	public void setPrimaryTempoBPM(String str)
	{
		this.primaryTempoBPM = parseTempo(str);
		if (!allPartsTempoMap.containsKey(0L))
			allPartsTempoMap.put(0L, this.primaryTempoBPM);
		if (!curPartTempoMap.containsKey(0L))
			curPartTempoMap.put(0L, this.primaryTempoBPM);
	}

	public void addTempoEvent(long tick, String str)
	{
		allPartsTempoMap.put(tick, parseTempo(str));
		curPartTempoMap.put(tick, parseTempo(str));
	}

	public int getCurrentTempoBPM(long tick)
	{
		Entry<Long, Integer> entry = curPartTempoMap.floorEntry(tick);
		if (entry == null)
			return getPrimaryTempoBPM();

		return entry.getValue();
	}

	public NavigableMap<Long, Integer> getAllPartsTempoMap()
	{
		return allPartsTempoMap;
	}

	private int parseDivisor(String str)
	{
		String[] parts = str.trim().split("[/:| ]");
		if (parts.length != 2)
		{
			throw new IllegalArgumentException("\"" + str + "\" is not a valid note length"
					+ " (example of valid note length: 1/4)");
		}
		int numerator = Integer.parseInt(parts[0]);
		int denominator = Integer.parseInt(parts[1]);
		if (numerator != 1)
		{
			throw new IllegalArgumentException("The numerator of the note length must be 1"
					+ " (example of valid note length: 1/4)");
		}
		if (denominator < 1)
		{
			throw new IllegalArgumentException("The denominator of the note length must be positive"
					+ " (example of valid note length: 1/4)");
		}

		return denominator;
	}

	public void setInstrument(LotroInstrument instrument)
	{
		this.instrument = instrument;
		this.instrumentSet = true;
	}

	public boolean isInstrumentSet()
	{
		return instrumentSet;
	}

	public void setDynamics(String str)
	{
		dynamics = Dynamics.valueOf(str);
	}

	public int getPartNumber()
	{
		return partNumber;
	}

	public String getTitle()
	{
		return title;
	}

	public KeySignature getKey()
	{
		return key;
	}

	public long getPpqn()
	{
		return ppqn;
	}

	public boolean isCompoundMeter()
	{
		return compoundMeter;
	}

	public int getPrimaryTempoBPM()
	{
		return primaryTempoBPM;
	}

	public LotroInstrument getInstrument()
	{
		return instrument;
	}

	public Dynamics getDynamics()
	{
		return dynamics;
	}
}
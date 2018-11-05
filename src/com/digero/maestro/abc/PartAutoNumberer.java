package com.digero.maestro.abc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.prefs.Preferences;

import com.digero.common.abc.LotroInstrument;

public class PartAutoNumberer
{
	public static class Settings
	{
		private Map<LotroInstrument, Integer> firstNumber = new HashMap<LotroInstrument, Integer>();
		private boolean incrementByTen;

		private Settings(Preferences prefs)
		{
			incrementByTen = prefs.getBoolean("incrementByTen", true);
			int x10 = incrementByTen ? 1 : 10;

			if (!prefs.getBoolean("newCowbellDefaults", false))
			{
				prefs.putBoolean("newCowbellDefaults", true);
				prefs.remove(LotroInstrument.COWBELL.toString());
				prefs.remove(LotroInstrument.MOOR_COWBELL.toString());
			}

			init(prefs, LotroInstrument.LUTE_OF_AGES, prefs.getInt("Lute", 1 * x10)); // Lute was renamed to Lute of Ages
			init(prefs, LotroInstrument.BASIC_LUTE, LotroInstrument.LUTE_OF_AGES);
			init(prefs, LotroInstrument.HARP, 2 * x10);
			init(prefs, LotroInstrument.MISTY_MOUNTAIN_HARP, LotroInstrument.HARP);
			init(prefs, LotroInstrument.THEORBO, 3 * x10);
			init(prefs, LotroInstrument.FLUTE, 4 * x10);
			init(prefs, LotroInstrument.CLARINET, 5 * x10);
			init(prefs, LotroInstrument.HORN, 6 * x10);
			init(prefs, LotroInstrument.BAGPIPE, 7 * x10);
			init(prefs, LotroInstrument.PIBGORN, LotroInstrument.BAGPIPE);
			init(prefs, LotroInstrument.BASIC_BASSOON, LotroInstrument.BAGPIPE);
			init(prefs, LotroInstrument.LONELY_MOUNTAIN_BASSOON, LotroInstrument.BAGPIPE);
			init(prefs, LotroInstrument.BRUSQUE_BASSOON, LotroInstrument.BAGPIPE);
			init(prefs, LotroInstrument.DRUMS, 8 * x10);
			init(prefs, LotroInstrument.COWBELL, LotroInstrument.DRUMS);
			init(prefs, LotroInstrument.MOOR_COWBELL, LotroInstrument.DRUMS);
			init(prefs, LotroInstrument.BASIC_FIDDLE, 9 * x10);
			init(prefs, LotroInstrument.BARDIC_FIDDLE, LotroInstrument.BASIC_FIDDLE);
			init(prefs, LotroInstrument.STUDENT_FIDDLE, LotroInstrument.BASIC_FIDDLE);
			init(prefs, LotroInstrument.LONELY_MOUNTAIN_FIDDLE, LotroInstrument.BASIC_FIDDLE);
			init(prefs, LotroInstrument.SPRIGHTLY_FIDDLE, LotroInstrument.BASIC_FIDDLE);
			init(prefs, LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE, LotroInstrument.BASIC_FIDDLE);

			assert (firstNumber.size() == LotroInstrument.values().length);
		}

		private void init(Preferences prefs, LotroInstrument instrument, int defaultValue)
		{
			firstNumber.put(instrument, prefs.getInt(instrument.toString(), defaultValue));
		}

		private void init(Preferences prefs, LotroInstrument instruments, LotroInstrument copyDefaultFrom)
		{
			init(prefs, instruments, firstNumber.get(copyDefaultFrom));
		}

		private void save(Preferences prefs)
		{
			for (Entry<LotroInstrument, Integer> entry : firstNumber.entrySet())
			{
				prefs.putInt(entry.getKey().toString(), entry.getValue());
			}
			prefs.putBoolean("incrementByTen", incrementByTen);
		}

		public Settings(Settings source)
		{
			copyFrom(source);
		}

		public void copyFrom(Settings source)
		{
			firstNumber = new HashMap<LotroInstrument, Integer>(source.firstNumber);
			incrementByTen = source.incrementByTen;
		}

		public int getIncrement()
		{
			return incrementByTen ? 10 : 1;
		}

		public boolean isIncrementByTen()
		{
			return incrementByTen;
		}

		public void setIncrementByTen(boolean incrementByTen)
		{
			this.incrementByTen = incrementByTen;
		}

		public void setFirstNumber(LotroInstrument instrument, int number)
		{
			firstNumber.put(instrument, number);
		}

		public int getFirstNumber(LotroInstrument instrument)
		{
			return firstNumber.get(instrument);
		}
	}

	private Settings settings;
	private Preferences prefsNode;
	private List<? extends NumberedAbcPart> parts = null;

	public PartAutoNumberer(Preferences prefsNode)
	{
		this.prefsNode = prefsNode;
		this.settings = new Settings(prefsNode);
	}

	public Settings getSettingsCopy()
	{
		return new Settings(settings);
	}

	public boolean isIncrementByTen()
	{
		return settings.isIncrementByTen();
	}

	public int getIncrement()
	{
		return settings.getIncrement();
	}

	public int getFirstNumber(LotroInstrument instrument)
	{
		return settings.getFirstNumber(instrument);
	}

	public void setSettings(Settings settings)
	{
		this.settings.copyFrom(settings);
		this.settings.save(prefsNode);
	}

	public void setParts(List<? extends NumberedAbcPart> parts)
	{
		this.parts = parts;
	}

	public void renumberAllParts()
	{
		if (parts == null)
			return;

		Set<Integer> numbersInUse = new HashSet<Integer>(parts.size());

		for (NumberedAbcPart part : parts)
		{
			int partNumber = getFirstNumber(part.getInstrument());
			while (numbersInUse.contains(partNumber))
			{
				partNumber += getIncrement();
			}
			numbersInUse.add(partNumber);
			part.setPartNumber(partNumber);
		}
	}

	public void onPartAdded(NumberedAbcPart partAdded)
	{
		if (parts == null)
			return;

		int newPartNumber = settings.getFirstNumber(partAdded.getInstrument());

		boolean conflict;
		do
		{
			conflict = false;
			for (NumberedAbcPart part : parts)
			{
				if (part != partAdded && part.getPartNumber() == newPartNumber)
				{
					newPartNumber += getIncrement();
					conflict = true;
				}
			}
		} while (conflict);

		partAdded.setPartNumber(newPartNumber);
	}

	public void onPartDeleted(NumberedAbcPart partDeleted)
	{
		if (parts == null)
			return;

		for (NumberedAbcPart part : parts)
		{
			int partNumber = part.getPartNumber();
			int deletedNumber = partDeleted.getPartNumber();
			int partFirstNumber = getFirstNumber(part.getInstrument());
			int deletedFirstNumber = getFirstNumber(partDeleted.getInstrument());
			if (part != partDeleted && partNumber > deletedNumber && partNumber > partFirstNumber
					&& partFirstNumber == deletedFirstNumber)
			{
				part.setPartNumber(partNumber - getIncrement());
			}
		}
	}

	public void setPartNumber(NumberedAbcPart partToChange, int newPartNumber)
	{
		if (parts == null)
			return;

		for (NumberedAbcPart part : parts)
		{
			if (part != partToChange && part.getPartNumber() == newPartNumber)
			{
				part.setPartNumber(partToChange.getPartNumber());
				break;
			}
		}
		partToChange.setPartNumber(newPartNumber);
	}

	public void setInstrument(NumberedAbcPart partToChange, LotroInstrument newInstrument)
	{
		if (newInstrument != partToChange.getInstrument())
		{
			onPartDeleted(partToChange);
			partToChange.setInstrument(newInstrument);
			onPartAdded(partToChange);
		}
	}

	public LotroInstrument[] getSortedInstrumentList()
	{
		LotroInstrument[] instruments = LotroInstrument.values();
		Arrays.sort(instruments, new Comparator<LotroInstrument>()
		{
			@Override public int compare(LotroInstrument a, LotroInstrument b)
			{
				int diff = getFirstNumber(a) - getFirstNumber(b);
				if (diff != 0)
					return diff;

				return a.toString().compareTo(b.toString());
			}
		});
		return instruments;
	}
}

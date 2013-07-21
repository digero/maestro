package com.digero.maestro.abc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.prefs.Preferences;

import com.digero.common.abc.LotroInstrument;

public class PartAutoNumberer {
	public static class Settings {
		private Map<LotroInstrument, Integer> firstNumber;
		private boolean incrementByTen;

		private Settings(Preferences prefs) {
			firstNumber = new HashMap<LotroInstrument, Integer>(LotroInstrument.values().length);
			int i = 0;
			for (LotroInstrument instrument : LotroInstrument.values()) {
				if (instrument != LotroInstrument.MOOR_COWBELL && instrument != LotroInstrument.PIBGORN)
					firstNumber.put(instrument, prefs.getInt(instrument.toString(), ++i));
			}
			// Pibgorn defaults to the bagpipe number
			firstNumber.put(LotroInstrument.PIBGORN,
					prefs.getInt(LotroInstrument.PIBGORN.toString(), firstNumber.get(LotroInstrument.BAGPIPE)));
			// Moor cowbell defaults to the cowbell number
			firstNumber.put(LotroInstrument.MOOR_COWBELL,
					prefs.getInt(LotroInstrument.MOOR_COWBELL.toString(), firstNumber.get(LotroInstrument.COWBELL)));

			incrementByTen = prefs.getBoolean("incrementByTen", true);
		}

		private void save(Preferences prefs) {
			for (Entry<LotroInstrument, Integer> entry : firstNumber.entrySet()) {
				prefs.putInt(entry.getKey().toString(), entry.getValue());
			}
			prefs.putBoolean("incrementByTen", incrementByTen);
		}

		public Settings(Settings source) {
			copyFrom(source);
		}

		public void copyFrom(Settings source) {
			firstNumber = new HashMap<LotroInstrument, Integer>(source.firstNumber);
			incrementByTen = source.incrementByTen;
		}

		public int getIncrement() {
			return incrementByTen ? 10 : 1;
		}

		public boolean isIncrementByTen() {
			return incrementByTen;
		}

		public void setIncrementByTen(boolean incrementByTen) {
			this.incrementByTen = incrementByTen;
		}

		public void setFirstNumber(LotroInstrument instrument, int number) {
			firstNumber.put(instrument, number);
		}

		public int getFirstNumber(LotroInstrument instrument) {
			return firstNumber.get(instrument);
		}
	}

	private Settings settings;
	private Preferences prefsNode;
	private List<AbcPart> parts;

	public PartAutoNumberer(Preferences prefsNode, List<AbcPart> parts) {
		this.prefsNode = prefsNode;
		this.parts = parts;
		settings = new Settings(prefsNode);
	}

	public Settings getSettingsCopy() {
		return new Settings(settings);
	}

	public boolean isIncrementByTen() {
		return settings.isIncrementByTen();
	}

	public int getIncrement() {
		return settings.getIncrement();
	}

	public int getFirstNumber(LotroInstrument instrument) {
		return settings.getFirstNumber(instrument);
	}

	public void setSettings(Settings settings) {
		this.settings.copyFrom(settings);
		this.settings.save(prefsNode);
	}

	public void renumberAllParts() {
		Set<Integer> numbersInUse = new HashSet<Integer>(parts.size());

		for (AbcPart part : parts) {
			int partNumber = getFirstNumber(part.getInstrument());
			while (numbersInUse.contains(partNumber)) {
				partNumber += getIncrement();
			}
			numbersInUse.add(partNumber);
			part.setPartNumber(partNumber);
		}
	}

	public void onPartAdded(AbcPart partAdded) {
		int newPartNumber = settings.getFirstNumber(partAdded.getInstrument());

		boolean conflict;
		do {
			conflict = false;
			for (AbcPart part : parts) {
				if (part != partAdded && part.getPartNumber() == newPartNumber) {
					newPartNumber += getIncrement();
					conflict = true;
				}
			}
		} while (conflict);

		partAdded.setPartNumber(newPartNumber);
	}

	public void onPartDeleted(AbcPart partDeleted) {
		for (AbcPart part : parts) {
			int partNumber = part.getPartNumber();
			int deletedNumber = partDeleted.getPartNumber();
			int partFirstNumber = getFirstNumber(part.getInstrument());
			int deletedFirstNumber = getFirstNumber(partDeleted.getInstrument());
			if (part != partDeleted && partNumber > deletedNumber && partNumber > partFirstNumber
					&& partFirstNumber == deletedFirstNumber) {
				part.setPartNumber(partNumber - getIncrement());
			}
		}
	}

	public void setPartNumber(AbcPart partToChange, int newPartNumber) {
		for (AbcPart part : parts) {
			if (part != partToChange && part.getPartNumber() == newPartNumber) {
				part.setPartNumber(partToChange.getPartNumber());
				break;
			}
		}
		partToChange.setPartNumber(newPartNumber);
	}

	public void setInstrument(AbcPart partToChange, LotroInstrument newInstrument) {
		if (newInstrument != partToChange.getInstrument()) {
			onPartDeleted(partToChange);
			partToChange.setInstrument(newInstrument);
			onPartAdded(partToChange);
		}
	}
}

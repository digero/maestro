package com.digero.maestro.project;

import com.digero.maestro.abc.LotroInstrument;
import com.digero.maestro.midi.SequenceInfo;

public class AbcPart {
	private SequenceInfo sequenceInfo;
	private boolean enabled;
	private int partNumber;
	private String name;
	private LotroInstrument instrument;
	private int[] trackTranspose;
	private boolean[] trackDisabled;

	public AbcPart(SequenceInfo sequenceInfo, int partNumber) {
		this(sequenceInfo, partNumber, LotroInstrument.LUTE.toString(), LotroInstrument.LUTE, true);
	}

	public AbcPart(SequenceInfo sequenceInfo, int partNumber, String name, LotroInstrument instrument, boolean enabled) {
		this.sequenceInfo = sequenceInfo;
		this.partNumber = partNumber;
		this.name = name;
		this.instrument = instrument;
		this.enabled = enabled;

		this.trackTranspose = new int[getTrackCount()];
		this.trackDisabled = new boolean[getTrackCount()];
	}

	public SequenceInfo getSequenceInfo() {
		return sequenceInfo;
	}

	public int getTrackCount() {
		return sequenceInfo.getTrackCount();
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

	public void setName(String name) {
		if (name == null)
			throw new NullPointerException();

		this.name = name;
	}

	public LotroInstrument getInstrument() {
		return instrument;
	}

	public void setInstrument(LotroInstrument instrument) {
		if (instrument == null)
			throw new NullPointerException();

		this.instrument = instrument;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean partEnabled) {
		this.enabled = partEnabled;
	}

	public int getTrackTranspose(int track) {
		return trackTranspose[track];
	}

	public void setTrackTranspose(int track, int transpose) {
		trackTranspose[track] = transpose;
	}

	public boolean isTrackEnabled(int track) {
		return !trackDisabled[track];
	}

	public void setTrackEnabled(int track, boolean enabled) {
		trackDisabled[track] = !enabled;
	}

	public int getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(int partNumber) {
		this.partNumber = partNumber;
	}
}

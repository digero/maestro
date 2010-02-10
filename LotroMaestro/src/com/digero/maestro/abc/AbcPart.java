package com.digero.maestro.abc;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.maestro.midi.SequenceInfo;

public class AbcPart {
	private SequenceInfo sequenceInfo;
	private boolean enabled;
	private int partNumber;
	private String name;
	private LotroInstrument instrument;
	private int baseTranspose;
	private int[] trackTranspose;
	private boolean[] trackDisabled;
	private final List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

	public AbcPart(SequenceInfo sequenceInfo, int baseTranspose, int partNumber) {
		this(sequenceInfo, baseTranspose, partNumber,
				sequenceInfo.getTitle() + " - " + LotroInstrument.LUTE.toString(), LotroInstrument.LUTE, true);
	}

	public AbcPart(SequenceInfo sequenceInfo, int baseTranspose, int partNumber, String name,
			LotroInstrument instrument, boolean enabled) {
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
		return getPartNumber() + ". " + getName();
	}

	public void setName(String name) {
		if (name == null)
			throw new NullPointerException();

		if (!this.name.equals(name)) {
			this.name = name;
			fireChangeEvent();
		}
	}

	public LotroInstrument getInstrument() {
		return instrument;
	}

	public void setInstrument(LotroInstrument instrument) {
		if (instrument == null)
			throw new NullPointerException();

		if (this.instrument != instrument) {
			this.instrument = instrument;
			fireChangeEvent();
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean partEnabled) {
		if (this.enabled != partEnabled) {
			this.enabled = partEnabled;
			fireChangeEvent();
		}
	}

	public int getBaseTranspose() {
		return baseTranspose;
	}

	public void setBaseTranspose(int baseTranspose) {
		if (this.baseTranspose != baseTranspose) {
			this.baseTranspose = baseTranspose;
			fireChangeEvent();
		}
	}

	public int getTrackTranspose(int track) {
		return trackTranspose[track];
	}

	public void setTrackTranspose(int track, int transpose) {
		if (trackTranspose[track] != transpose) {
			trackTranspose[track] = transpose;
			fireChangeEvent();
		}
	}

	public boolean isTrackEnabled(int track) {
		return !trackDisabled[track];
	}

	public void setTrackEnabled(int track, boolean enabled) {
		if (trackDisabled[track] != !enabled) {
			trackDisabled[track] = !enabled;
			fireChangeEvent();
		}
	}

	public int getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(int partNumber) {
		if (this.partNumber != partNumber) {
			this.partNumber = partNumber;
			fireChangeEvent();
		}
	}

	public void addChangeListener(ChangeListener l) {
		changeListeners.add(l);
	}

	public void removeChangeListener(ChangeListener l) {
		changeListeners.remove(l);
	}

	protected void fireChangeEvent() {
		if (changeListeners.size() > 0) {
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener l : changeListeners) {
				l.stateChanged(e);
			}
		}
	}
}

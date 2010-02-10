package com.digero.maestro.project;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.maestro.abc.LotroInstrument;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.TrackInfo;

public class AbcPart {
	private AbcProject project;
	private boolean enabled;
	private int partNumber;
	private String name;
	private LotroInstrument instrument;
	private int[] trackTranspose;
	private boolean[] trackEnabled;

	public AbcPart(AbcProject project) {
		this(project, LotroInstrument.LUTE, project.calcNextPartNumber(), true);
	}

	public AbcPart(AbcProject project, LotroInstrument instrument, int partNumber, boolean partEnabled) {
		this.project = project;
		this.instrument = instrument;
		this.partNumber = partNumber;
		this.enabled = partEnabled;

		name = project.getSequenceInfo().getTitle() + " - " + instrument.toString();

		trackTranspose = new int[getTrackCount()];
		trackEnabled = new boolean[getTrackCount()];
	}

	public AbcProject getProject() {
		return project;
	}

	public SequenceInfo getSequenceInfo() {
		return project.getSequenceInfo();
	}

	public int getTrackCount() {
		return project.getSequenceInfo().getTrackCount();
	}

	public TrackInfo getTrackInfo(int track) {
		return project.getSequenceInfo().getTrackInfo(track);
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return getName();
	}

	public void setName(String name, Object source) {
		if (name == null)
			throw new NullPointerException();

		if (!this.name.equals(name)) {
			this.name = name;
			fireChangeEvent(source);
		}
	}

	public LotroInstrument getInstrument() {
		return instrument;
	}

	public void setInstrument(LotroInstrument instrument, Object source) {
		if (instrument == null)
			throw new NullPointerException();

		if (this.instrument != instrument) {
			this.instrument = instrument;
			fireChangeEvent(source);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean partEnabled, Object source) {
		if (this.enabled != partEnabled) {
			this.enabled = partEnabled;
			fireChangeEvent(source);
		}
	}

	/**
	 * Gets the combined transpose for the track and project (this is the
	 * transpose that will actually be played).
	 */
	public int getTranspose(int track) {
		return trackTranspose[track] + project.getTranspose();
	}

	/**
	 * Gets the octave transpose for the track, independent of the project's
	 * transpose.
	 */
	public int getOctaveTranspose(int track) {
		return trackTranspose[track];
	}

	public void setOctaveTranspose(int track, int transpose, Object source) {
		if (trackTranspose[track] != transpose) {
			trackTranspose[track] = transpose;
			fireChangeEvent(source);
		}
	}

	public boolean isTrackEnabled(int track) {
		return trackEnabled[track];
	}

	public void setTrackEnabled(int track, boolean enabled, Object source) {
		if (trackEnabled[track] != enabled) {
			trackEnabled[track] = enabled;
			fireChangeEvent(source);
		}
	}

	public int getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(int partNumber, Object source) {
		if (this.partNumber != partNumber) {
			this.partNumber = partNumber;
			fireChangeEvent(source);
		}
	}

	private List<ChangeListener> changeListeners = null;

	public void addChangeListener(ChangeListener l) {
		if (changeListeners == null)
			changeListeners = new ArrayList<ChangeListener>();

		changeListeners.add(l);
	}

	public void removeChangeListener(ChangeListener l) {
		if (changeListeners != null)
			changeListeners.remove(l);
	}

	/* package protected */void fireChangeEvent(Object source) {
		if (changeListeners != null) {
			ChangeEvent e = new ChangeEvent(source);
			for (ChangeListener l : changeListeners) {
				l.stateChanged(e);
			}
		}
	}
}

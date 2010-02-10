package com.digero.maestro.project;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.digero.maestro.midi.KeySignature;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.TimeSignature;

public class AbcProjectTemp {
	private File saveFile;

	private SequenceInfo sequenceInfo;
//	private List<AbcPart> parts = new ArrayList<AbcPart>();
	private ArrayListModel<AbcPart> partsModel = new ArrayListModel<AbcPart>();

	private int transpose = 0;
	private int tempoBPM = 120;
	private TimeSignature timeSignature = TimeSignature.FOUR_FOUR;
	private KeySignature keySignature = KeySignature.C_MAJOR;

	public static AbcProjectTemp newFromMidi(File midiFile) throws InvalidMidiDataException, IOException {
		SequenceInfo sequenceInfo = new SequenceInfo(midiFile);

		AbcProjectTemp project = new AbcProjectTemp();
		project.sequenceInfo = sequenceInfo;
		project.saveFile = null;
		project.partsModel.add(new AbcPart(project));

		return project;
	}

	public static AbcProjectTemp loadFromFile(File projectFile) throws IOException {
		// TODO loadFromFile
		throw new NotImplementedException();
	}

	private AbcProjectTemp() {
	}

	public SequenceInfo getSequenceInfo() {
		return sequenceInfo;
	}

	public int getTranspose() {
		return transpose;
	}

	public void setTranspose(int transpose, Object source) {
		if (this.transpose != transpose) {
			this.transpose = transpose;
			fireChangeEvent(source);
			firePartsChangeEvent(source);
		}
	}

	public int getTempoBPM() {
		return tempoBPM;
	}

	public void setTempoBPM(int tempoBPM, Object source) {
		if (this.tempoBPM != tempoBPM) {
			this.tempoBPM = tempoBPM;
			fireChangeEvent(source);
		}
	}

	public TimeSignature getTimeSignature() {
		return timeSignature;
	}

	public void setTimeSignature(TimeSignature timeSignature, Object source) {
		if (timeSignature == null)
			throw new NullPointerException();

		if (!this.timeSignature.equals(timeSignature)) {
			this.timeSignature = timeSignature;
			fireChangeEvent(source);
		}
	}

	public KeySignature getKeySignature() {
		return keySignature;
	}

	public void setKeySignature(KeySignature keySignature, Object source) {
		if (keySignature == null)
			throw new NullPointerException();

		if (!this.keySignature.equals(keySignature)) {
			this.keySignature = keySignature;
			fireChangeEvent(source);
		}
	}

	/**
	 * Gets the file where this project should be saved, or <code>null</code> if
	 * none has been set.
	 */
	public File getSaveFile() {
		return saveFile;
	}

	public void setSaveFile(File saveFile) {
		if (saveFile != null && !saveFile.isFile())
			throw new InvalidParameterException("Save file must be a file (not a directory)");

		this.saveFile = saveFile;
	}

	public ArrayListModel<AbcPart> getPartsListModel() {
		return partsModel;
	}

	public List<AbcPart> getParts() {
		return partsModel.getList();
	}

	public int calcNextPartNumber() {
		int size = partsModel.getSize();
		outerLoop: for (int n = 1; n <= size; n++) {
			for (int i = 0; i < size; i++) {
				if (n == partsModel.getElementAt(i).getPartNumber())
					continue outerLoop;
			}
			return n;
		}
		return size + 1;
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

	protected void fireChangeEvent(Object source) {
		if (changeListeners != null) {
			ChangeEvent e = new ChangeEvent(source);
			for (ChangeListener l : changeListeners) {
				l.stateChanged(e);
			}
		}
	}

	protected void firePartsChangeEvent(Object source) {
		// Fire change events for all the parts.  Yeah, it's a hack...
		for (AbcPart part : partsModel.getList()) {
			part.fireChangeEvent(source);
		}
	}
}

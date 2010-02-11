package com.digero.maestro.midi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.JOptionPane;
import javax.swing.Timer;

public class SequencerWrapper {
	private Sequencer sequencer;
	private long dragPosition;
	private boolean isDragging;

	private Timer updateTimer;

	private List<SequencerListener> listeners = null;

	public SequencerWrapper() {
		this(getDefaultSequencer());
	}

	public SequencerWrapper(Sequencer sequencer) {
		this.sequencer = sequencer;

		dragPosition = 0;
		isDragging = false;

		updateTimer = new Timer(50, timerTick);
		updateTimer.start();
	}

	private ActionListener timerTick = new ActionListener() {
		private long lastUpdatePosition = -1;

		public void actionPerformed(ActionEvent e) {
			if (sequencer != null) {
				long songPos = sequencer.getMicrosecondPosition();
				if (lastUpdatePosition != songPos) {
					lastUpdatePosition = songPos;
					fireChangeEvent(updateTimer, SequencerProperty.POSITION);
				}
			}
		}
	};

	public long getPosition() {
		return sequencer.getMicrosecondPosition();
	}

	public void setPosition(long position, Object source) {
		if (position != getPosition()) {
			sequencer.setMicrosecondPosition(position);
			fireChangeEvent(source, SequencerProperty.POSITION);
		}
	}

	public long getLength() {
		return sequencer.getMicrosecondLength();
	}

	public boolean isRunning() {
		return sequencer.isRunning();
	}

	public void setRunning(boolean isRunning, Object source) {
		if (isRunning != this.isRunning()) {
			if (isRunning)
				sequencer.start();
			else
				sequencer.stop();
			fireChangeEvent(source, SequencerProperty.IS_RUNNING);
		}
	}

	public void start(Object source) {
		setRunning(true, source);
	}

	public void stop(Object source) {
		setRunning(false, source);
	}

	public boolean getTrackMute(int track) {
		return sequencer.getTrackMute(track);
	}

	public void setTrackMute(int track, boolean mute, Object source) {
		if (mute != this.getTrackMute(track)) {
			sequencer.setTrackMute(track, mute);
			fireChangeEvent(source, SequencerProperty.TRACK_ACTIVE);
		}
	}

	public boolean getTrackSolo(int track) {
		return sequencer.getTrackSolo(track);
	}

	public void setTrackSolo(int track, boolean solo, Object source) {
		if (solo != this.getTrackSolo(track)) {
			sequencer.setTrackSolo(track, solo);
			fireChangeEvent(source, SequencerProperty.TRACK_ACTIVE);
		}
	}

	/**
	 * Takes into account both muting and solo.
	 */
	public boolean isTrackActive(int track) {
		Sequence song = sequencer.getSequence();

		if (song == null)
			return true;

		if (sequencer.getTrackSolo(track))
			return true;

		for (int i = song.getTracks().length - 1; i >= 0; --i) {
			if (i != track && sequencer.getTrackSolo(i))
				return false;
		}

		return !sequencer.getTrackMute(track);
	}

	/**
	 * If dragging, returns the drag position. Otherwise returns the song
	 * position.
	 */
	public long getThumbPosition() {
		return isDragging() ? getDragPosition() : getPosition();
	}

	public long getDragPosition() {
		return dragPosition;
	}

	public void setDragPosition(long dragPosition, Object source) {
		if (this.dragPosition != dragPosition) {
			this.dragPosition = dragPosition;
			fireChangeEvent(source, SequencerProperty.DRAG_POSITION);
		}
	}

	public boolean isDragging() {
		return isDragging;
	}

	public void setDragging(boolean isDragging, Object source) {
		if (this.isDragging != isDragging) {
			this.isDragging = isDragging;
			fireChangeEvent(source, SequencerProperty.IS_DRAGGING);
		}
	}

	public void addChangeListener(SequencerListener l) {
		if (listeners == null)
			listeners = new ArrayList<SequencerListener>();

		listeners.add(l);
	}

	public void removeChangeListener(SequencerListener l) {
		if (listeners != null)
			listeners.remove(l);
	}

	protected void fireChangeEvent(Object source, SequencerProperty property) {
		if (listeners != null) {
			SequencerEvent e = new SequencerEvent(source, this, property);
			for (SequencerListener l : listeners) {
				l.propertyChanged(e);
			}
		}
	}

	public static Sequencer getDefaultSequencer() {
		try {
			Sequencer sequencer = MidiSystem.getSequencer(false);
			sequencer.open();
			sequencer.getTransmitter().setReceiver(MidiSystem.getReceiver());
			return sequencer;
		}
		catch (MidiUnavailableException e) {
			JOptionPane.showMessageDialog(null, "Failed to initialize MIDI sequencer.\nThe program will now exit.",
					"Failed to initialize MIDI sequencer.", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return null;
		}
	}

	public void setSequence(Sequence sequence) throws InvalidMidiDataException {
		sequencer.setSequence(sequence);
	}

	public Sequence getSequence() {
		return sequencer.getSequence();
	}
}

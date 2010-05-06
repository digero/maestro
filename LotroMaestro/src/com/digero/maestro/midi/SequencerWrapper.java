package com.digero.maestro.midi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.Timer;

public class SequencerWrapper {
	private Sequencer sequencer;
	private DrumFilterTransceiver drumFilter;
	private long dragPosition;
	private boolean isDragging;

	private Timer updateTimer;

	private List<SequencerListener> listeners = null;

	public SequencerWrapper() throws MidiUnavailableException {
		this(getDefaultSequencer());
	}

	public SequencerWrapper(DrumFilterTransceiver drumFilter) throws MidiUnavailableException {
		this((Sequencer) null);
		this.sequencer = getDefaultSequencer(drumFilter);
		this.drumFilter = drumFilter;
	}

	public SequencerWrapper(Sequencer sequencer) {
		this.sequencer = sequencer;
		this.drumFilter = null;

		dragPosition = 0;
		isDragging = false;

		updateTimer = new Timer(50, timerTick);
		updateTimer.start();
	}

	public DrumFilterTransceiver getDrumFilter() {
		return drumFilter;
	}

	public void setDrumSolo(int drumId, boolean solo, Object source) {
		if (drumFilter != null && solo != getDrumSolo(drumId)) {
			drumFilter.setDrumSolo(drumId, solo);
			fireChangeEvent(source, SequencerProperty.TRACK_ACTIVE);
		}
	}

	public boolean getDrumSolo(int drumId) {
		if (drumFilter == null)
			return false;

		return drumFilter.getDrumSolo(drumId);
	}

	private TimerActionListener timerTick = new TimerActionListener();

	private class TimerActionListener implements ActionListener {
		private long lastUpdatePosition = -1;
		private boolean lastRunning = false;

		public void actionPerformed(ActionEvent e) {
			if (sequencer != null) {
				long songPos = sequencer.getMicrosecondPosition();
				boolean running = sequencer.isRunning();
				if (songPos >= getLength()) {
					setPosition(0, updateTimer);
					lastUpdatePosition = songPos;
				}
				else if (lastUpdatePosition != songPos) {
					lastUpdatePosition = songPos;
					fireChangeEvent(updateTimer, SequencerProperty.POSITION);
				}
				if (lastRunning != running) {
					lastRunning = running;
					fireChangeEvent(updateTimer, SequencerProperty.IS_RUNNING);
				}
			}
		}
	};

	public void reset(Object source) {
		stop(source);
		setPosition(0, source);

		// Reset the instruments
		boolean isOpen = sequencer.isOpen();
		Receiver rec = null;
		try {
			if (!isOpen)
				sequencer.open();
			rec = sequencer.getReceiver();
			ShortMessage msg = new ShortMessage();
			for (int i = 0; i < 16; i++) {
				msg.setMessage(ShortMessage.PROGRAM_CHANGE, i, 0, 0);
				rec.send(msg, -1);
			}
		}
		catch (MidiUnavailableException e) {
			// Ignore
		}
		catch (InvalidMidiDataException e) {
			// Ignore
		}
		if (rec != null)
			rec.close();
		if (!isOpen)
			sequencer.close();
	}

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
			timerTick.lastRunning = isRunning;
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

	public boolean isDrumActive(int track, int drumId) {
		return isTrackActive(track) && (drumFilter == null || drumFilter.isDrumActive(drumId));
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

	public static Sequencer getDefaultSequencer() throws MidiUnavailableException {
		Sequencer sequencer = MidiSystem.getSequencer(false);
		sequencer.open();
		sequencer.getTransmitter().setReceiver(MidiSystem.getReceiver());
		return sequencer;
	}

	public static Sequencer getDefaultSequencer(DrumFilterTransceiver drumFilter) throws MidiUnavailableException {
		Sequencer sequencer = MidiSystem.getSequencer(false);
		sequencer.open();
		sequencer.getTransmitter().setReceiver(drumFilter);
		drumFilter.setReceiver(MidiSystem.getReceiver());
		return sequencer;
	}

	public void setSequence(Sequence sequence, Object source) throws InvalidMidiDataException {
		if (sequencer.getSequence() != sequence) {
			sequencer.setSequence(sequence);
			fireChangeEvent(source, SequencerProperty.LENGTH);
		}
	}

	public Sequence getSequence() {
		return sequencer.getSequence();
	}

	public Transmitter getTransmitter() throws MidiUnavailableException {
		return sequencer.getTransmitter();
	}

	public Receiver getReceiver() throws MidiUnavailableException {
		return sequencer.getReceiver();
	}

	public void open() throws MidiUnavailableException {
		sequencer.open();
	}

	public void close() {
		sequencer.close();
	}
}

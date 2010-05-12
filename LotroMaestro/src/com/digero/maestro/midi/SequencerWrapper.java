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
	private Receiver receiver;
	private Transmitter transmitter;
	private DrumFilterTransceiver drumFilter;
	private long dragPosition;
	private boolean isDragging;

	private Timer updateTimer;

	private List<SequencerListener> listeners = null;

	public SequencerWrapper() throws MidiUnavailableException {
		sequencer = MidiSystem.getSequencer(false);
		sequencer.open();
		transmitter = sequencer.getTransmitter();
		receiver = MidiSystem.getReceiver();

		transmitter.setReceiver(receiver);

		updateTimer = new Timer(50, timerTick);
		updateTimer.start();
	}

	public SequencerWrapper(DrumFilterTransceiver drumFilter) throws MidiUnavailableException {
		this.drumFilter = drumFilter;

		sequencer = MidiSystem.getSequencer(false);
		sequencer.open();
		transmitter = sequencer.getTransmitter();
		receiver = MidiSystem.getReceiver();

		transmitter.setReceiver(drumFilter);
		drumFilter.setReceiver(receiver);

		updateTimer = new Timer(50, timerTick);
		updateTimer.start();
	}

	public SequencerWrapper(Sequencer sequencer, Transmitter transmitter, Receiver receiver) {
		this.sequencer = sequencer;
		this.transmitter = transmitter;
		this.receiver = receiver;

		updateTimer = new Timer(50, timerTick);
		updateTimer.start();
	}

	public DrumFilterTransceiver getDrumFilter() {
		return drumFilter;
	}

	public void setDrumSolo(int drumId, boolean solo) {
		if (drumFilter != null && solo != getDrumSolo(drumId)) {
			drumFilter.setDrumSolo(drumId, solo);
			fireChangeEvent(SequencerProperty.TRACK_ACTIVE);
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
					setPosition(0);
					lastUpdatePosition = songPos;
				}
				else if (lastUpdatePosition != songPos) {
					lastUpdatePosition = songPos;
					fireChangeEvent(SequencerProperty.POSITION);
				}
				if (lastRunning != running) {
					lastRunning = running;
					fireChangeEvent(SequencerProperty.IS_RUNNING);
				}
			}
		}
	};

	public void reset() {
		stop();
		setPosition(0);

		// Reset the instruments
		boolean isOpen = sequencer.isOpen();
		try {
			if (!isOpen)
				sequencer.open();

			ShortMessage msg = new ShortMessage();
			for (int i = 0; i < 16; i++) {
				msg.setMessage(ShortMessage.PROGRAM_CHANGE, i, 0, 0);
				receiver.send(msg, -1);
			}
		}
		catch (MidiUnavailableException e) {
			// Ignore
		}
		catch (InvalidMidiDataException e) {
			// Ignore
		}

		if (!isOpen)
			sequencer.close();
	}

	public long getPosition() {
		return sequencer.getMicrosecondPosition();
	}

	public void setPosition(long position) {
		if (position != getPosition()) {
			sequencer.setMicrosecondPosition(position);
			fireChangeEvent(SequencerProperty.POSITION);
		}
	}

	public long getLength() {
		return sequencer.getMicrosecondLength();
	}

	public boolean isRunning() {
		return sequencer.isRunning();
	}

	public void setRunning(boolean isRunning) {
		if (isRunning != this.isRunning()) {
			if (isRunning)
				sequencer.start();
			else
				sequencer.stop();
			timerTick.lastRunning = isRunning;
			fireChangeEvent(SequencerProperty.IS_RUNNING);
		}
	}

	public void start() {
		setRunning(true);
	}

	public void stop() {
		setRunning(false);
	}

	public boolean getTrackMute(int track) {
		return sequencer.getTrackMute(track);
	}

	public void setTrackMute(int track, boolean mute) {
		if (mute != this.getTrackMute(track)) {
			sequencer.setTrackMute(track, mute);
			fireChangeEvent(SequencerProperty.TRACK_ACTIVE);
		}
	}

	public boolean getTrackSolo(int track) {
		return sequencer.getTrackSolo(track);
	}

	public void setTrackSolo(int track, boolean solo) {
		if (solo != this.getTrackSolo(track)) {
			sequencer.setTrackSolo(track, solo);
			fireChangeEvent(SequencerProperty.TRACK_ACTIVE);
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

	public void setDragPosition(long dragPosition) {
		if (this.dragPosition != dragPosition) {
			this.dragPosition = dragPosition;
			fireChangeEvent(SequencerProperty.DRAG_POSITION);
		}
	}

	public boolean isDragging() {
		return isDragging;
	}

	public void setDragging(boolean isDragging) {
		if (this.isDragging != isDragging) {
			this.isDragging = isDragging;
			fireChangeEvent(SequencerProperty.IS_DRAGGING);
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

	protected void fireChangeEvent(SequencerProperty property) {
		if (listeners != null) {
			SequencerEvent e = new SequencerEvent(this, property);
			for (SequencerListener l : listeners) {
				l.propertyChanged(e);
			}
		}
	}

	public void setSequence(Sequence sequence) throws InvalidMidiDataException {
		if (sequencer.getSequence() != sequence) {
			boolean preLoaded = isLoaded();
			sequencer.setSequence(sequence);
			if (preLoaded != isLoaded())
				fireChangeEvent(SequencerProperty.IS_LOADED);
			fireChangeEvent(SequencerProperty.LENGTH);
		}
	}

	public void clearSequence() {
		try {
			setSequence(null);
		}
		catch (InvalidMidiDataException e) {
			// This shouldn't happen
			throw new RuntimeException(e);
		}
	}

	public boolean isLoaded() {
		return sequencer.getSequence() != null;
	}

	public Sequence getSequence() {
		return sequencer.getSequence();
	}

	public Transmitter getTransmitter() {
		return transmitter;
	}

	public Receiver getReceiver() {
		return receiver;
	}

	public void open() throws MidiUnavailableException {
		sequencer.open();
	}

	public void close() {
		sequencer.close();
	}
}

package com.digero.maestro.view;

import javax.swing.JLabel;

import com.digero.maestro.midi.SequencerEvent;
import com.digero.maestro.midi.SequencerListener;
import com.digero.maestro.midi.SequencerProperty;
import com.digero.maestro.midi.SequencerWrapper;

public class SongPositionLabel extends JLabel implements SequencerListener {
	private SequencerWrapper sequencer;

	public SongPositionLabel(SequencerWrapper sequencer) {
		this.sequencer = sequencer;
		sequencer.addChangeListener(this);
	}

	@Override
	public void propertyChanged(SequencerEvent evt) {
		SequencerProperty p = evt.getProperty();
		if (p == SequencerProperty.POSITION || p == SequencerProperty.DRAG_POSITION || p == SequencerProperty.LENGTH) {
			setText(formatDuration(sequencer.getThumbPosition()) + "/" + formatDuration(sequencer.getLength()));
		}
	}

	private static String formatDuration(long micros) {
		StringBuilder s = new StringBuilder(5);

		int t = (int) (micros / (1000 * 1000));

		int hr = t / (60 * 60);
		t %= 60 * 60;
		int min = t / 60;
		t %= 60;
		int sec = t;

		if (hr > 0) {
			s.append(hr).append(':');
			if (min < 10) {
				s.append('0');
			}
		}
		s.append(min).append(':');
		if (sec < 10) {
			s.append('0');
		}
		s.append(sec);

		return s.toString();
	}
}

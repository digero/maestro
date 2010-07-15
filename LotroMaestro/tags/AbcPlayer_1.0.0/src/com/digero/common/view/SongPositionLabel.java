package com.digero.common.view;

import javax.swing.JLabel;

import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;

public class SongPositionLabel extends JLabel implements SequencerListener {
	private SequencerWrapper sequencer;

	public SongPositionLabel(SequencerWrapper sequencer) {
		this.sequencer = sequencer;
		sequencer.addChangeListener(this);
		update();
	}

	@Override
	public void propertyChanged(SequencerEvent evt) {
		SequencerProperty p = evt.getProperty();
		if (p == SequencerProperty.POSITION || p == SequencerProperty.DRAG_POSITION || p == SequencerProperty.LENGTH) {
			update();
		}
	}

	public void update() {
		long len = sequencer.getLength();
		setText(formatDuration(sequencer.getThumbPosition(), len) + "/" + formatDuration(len, len));
	}

	private static String formatDuration(long micros, long maxMicros) {
		StringBuilder s = new StringBuilder(5);

		int t = (int) (micros / (1000 * 1000));
		int hr = t / (60 * 60);
		t %= 60 * 60;
		int min = t / 60;
		t %= 60;
		int sec = t;

		int tMax = (int) (maxMicros / (1000 * 1000));
		int hrMax = tMax / (60 * 60);
		tMax %= 60 * 60;
		int minMax = tMax / 60;

		if (hrMax > 0) {
			s.append(hr).append(':');
			if (min < 10) {
				s.append('0');
			}
		}
		else if (minMax >= 10 && min < 10) {
			s.append('0');
		}
		s.append(min).append(':');
		if (sec < 10) {
			s.append('0');
		}
		s.append(sec);

		return s.toString();
	}
}

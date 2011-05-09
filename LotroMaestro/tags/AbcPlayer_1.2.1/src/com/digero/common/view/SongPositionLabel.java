package com.digero.common.view;

import javax.swing.JLabel;

import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.Util;

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
		setText(Util.formatDuration(sequencer.getThumbPosition(), len) + "/" + Util.formatDuration(len, len));
	}
}

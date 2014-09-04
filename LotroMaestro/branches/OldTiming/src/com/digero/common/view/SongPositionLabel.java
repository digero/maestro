package com.digero.common.view;

import javax.swing.JLabel;

import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.Util;

public class SongPositionLabel extends JLabel implements SequencerListener
{
	private SequencerWrapper sequencer;
	private boolean adjustForTempo;

	public SongPositionLabel(SequencerWrapper sequencer)
	{
		this(sequencer, false);
	}

	public SongPositionLabel(SequencerWrapper sequencer, boolean adjustForTempo)
	{
		this.sequencer = sequencer;
		this.adjustForTempo = adjustForTempo;
		sequencer.addChangeListener(this);
		update();
	}

	@Override public void propertyChanged(SequencerEvent evt)
	{
		SequencerProperty p = evt.getProperty();
		if (p.isInMask(SequencerProperty.THUMB_POSITION_MASK | SequencerProperty.LENGTH.mask
				| SequencerProperty.TEMPO.mask))
		{
			update();
		}
	}

	public void update()
	{
		long pos = sequencer.getThumbPosition();
		long len = sequencer.getLength();

		if (adjustForTempo)
		{
			pos = Math.round(pos / sequencer.getTempoFactor());
			len = Math.round(len / sequencer.getTempoFactor());
		}

		setText(Util.formatDuration(pos, len) + "/" + Util.formatDuration(len, len));
	}
}

package com.digero.maestro.view;

import info.clearthought.layout.TableLayoutConstants;

import javax.swing.JPanel;

import com.digero.maestro.abc.DrumPart;
import com.digero.maestro.midi.SequencerWrapper;
import com.digero.maestro.midi.TrackInfo;

public class DrumPanel extends JPanel implements TableLayoutConstants {
	private static final int TITLE_WIDTH = 160;
	private static final double[] LAYOUT_COLS = new double[] {
			TITLE_WIDTH, 48, FILL, 1
	};
	private static final double[] LAYOUT_ROWS = new double[] {
			4, 48, 4
	};

	public DrumPanel(TrackInfo info, SequencerWrapper sequencer, DrumPart part, int drumId) {
		
	}
}

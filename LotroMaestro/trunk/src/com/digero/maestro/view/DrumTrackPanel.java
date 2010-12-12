package com.digero.maestro.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sun.awt.VerticalBagLayout;

import com.digero.common.midi.SequencerWrapper;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.IDisposable;

public class DrumTrackPanel extends JPanel implements IDisposable {
	public DrumTrackPanel(TrackInfo track, SequencerWrapper sequencer, AbcPart abcPart) {
		super(new BorderLayout());

		setBackground(Color.WHITE);
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));

		JLabel title = new JLabel(track.getTrackNumber() + ". " + track.getName());
		title.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		title.setOpaque(false);
		title.setFont(title.getFont().deriveFont(Font.BOLD | Font.ITALIC));

		JPanel content = new JPanel(new VerticalBagLayout());
		for (int drumId : track.getDrumsInUse()) {
			content.add(new DrumPanel(track, sequencer, abcPart, drumId));
		}

		add(title, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
	}

	public int getUnitIncrement() {
		for (Component child : getComponents()) {
			if (child instanceof DrumPanel) {
				return child.getPreferredSize().height;
			}
		}
		return 20;
	}

	@Override
	public void dispose() {
		for (Component child : getComponents()) {
			if (child instanceof IDisposable) {
				((IDisposable) child).dispose();
			}
		}
	}
}

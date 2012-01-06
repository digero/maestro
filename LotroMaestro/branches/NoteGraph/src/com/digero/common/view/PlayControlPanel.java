package com.digero.common.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.digero.common.icons.IconLoader;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerWrapper;

public class PlayControlPanel extends JPanel implements TableLayoutConstants {
	private SequencerWrapper sequencer;

	private SongPositionBar songPositionBar;
	private SongPositionLabel songPositionLabel;

	private JButton playButton;
	private JButton stopButton;

	private Icon playIcon, pauseIcon;

	public PlayControlPanel(SequencerWrapper sequencer) {
		this(sequencer, "play", "pause", "stop");
	}

	public PlayControlPanel(SequencerWrapper seq, String playIconName, String pauseIconName, String stopIconName) {
		super(new TableLayout(new double[] {
				4, 0.50, PREFERRED, PREFERRED, 0.50, 4, PREFERRED, 4
		}, new double[] {
				4, PREFERRED, 4, PREFERRED, 4
		}));

		this.sequencer = seq;

		this.playIcon = new ImageIcon(IconLoader.class.getResource(playIconName + ".png"));
		this.pauseIcon = new ImageIcon(IconLoader.class.getResource(pauseIconName + ".png"));
		Icon stopIcon = new ImageIcon(IconLoader.class.getResource(stopIconName + ".png"));

		songPositionBar = new SongPositionBar(seq);
		songPositionLabel = new SongPositionLabel(seq);

		playButton = new JButton(playIcon);
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sequencer.setRunning(!sequencer.isRunning());
			}
		});

		stopButton = new JButton(stopIcon);
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sequencer.reset(false);
			}
		});

		sequencer.addChangeListener(new SequencerListener() {
			public void propertyChanged(SequencerEvent evt) {
				updateButtonStates();
			}
		});

		updateButtonStates();

		add(songPositionBar, "1, 1, 4, 1");
		add(songPositionLabel, "6, 1");
		add(playButton, "2, 3");
		add(stopButton, "3, 3");
	}

	private void updateButtonStates() {
		playButton.setIcon(sequencer.isRunning() ? pauseIcon : playIcon);
		playButton.setEnabled(sequencer.isLoaded());
		stopButton.setEnabled(sequencer.isLoaded() && (sequencer.isRunning() || sequencer.getPosition() != 0));
	}
}

package com.digero.common.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.digero.maestro.midi.SequencerEvent;
import com.digero.maestro.midi.SequencerListener;
import com.digero.maestro.midi.SequencerWrapper;
import com.digero.maestro.view.ProjectFrame;

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

		this.playIcon = new ImageIcon(ProjectFrame.class.getResource("icons/" + playIconName + ".png"));
		this.pauseIcon = new ImageIcon(ProjectFrame.class.getResource("icons/" + pauseIconName + ".png"));
		Icon stopIcon = new ImageIcon(ProjectFrame.class.getResource("icons/" + stopIconName + ".png"));

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
				sequencer.reset();
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

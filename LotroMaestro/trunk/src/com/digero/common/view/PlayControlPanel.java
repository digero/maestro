package com.digero.common.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
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

	private NativeVolumeBar volumeBar;

	public PlayControlPanel(SequencerWrapper sequencer, NativeVolumeBar.Callback volumeManager) {
		this(sequencer, volumeManager, "play", "pause", "stop");
	}

	public PlayControlPanel(SequencerWrapper seq, NativeVolumeBar.Callback volumeManager, String playIconName,
			String pauseIconName, String stopIconName) {
		super(new TableLayout(new double[] {
				4, 0.50, 4, PREFERRED, PREFERRED, 4, 0.50, 4, PREFERRED, 4
		}, new double[] {
				4, PREFERRED, 4, PREFERRED, 2
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

		JPanel volumePanel = new JPanel();
		if (volumeManager != null) {
			volumeBar = new NativeVolumeBar(volumeManager);

			TableLayout volumeLayout = new TableLayout(new double[] {
					PREFERRED
			}, new double[] {
					PREFERRED, PREFERRED
			});
			volumePanel.setLayout(volumeLayout);
			volumePanel.add(new JLabel("Volume"), "0, 0, c, c");
			volumePanel.add(volumeBar, "0, 1, f, c");
		}

		updateButtonStates();

		add(songPositionBar, "1, 1, 6, 1");
		add(songPositionLabel, "8, 1");
		add(playButton, "3, 3, c, c");
		add(stopButton, "4, 3, c, c");
		add(volumePanel, "6, 3, c, c");
	}

	public void onVolumeChanged() {
		if (volumeBar != null)
			volumeBar.repaint();
	}

	private void updateButtonStates() {
		playButton.setIcon(sequencer.isRunning() ? pauseIcon : playIcon);
		playButton.setEnabled(sequencer.isLoaded());
		stopButton.setEnabled(sequencer.isLoaded() && (sequencer.isRunning() || sequencer.getPosition() != 0));
	}
}

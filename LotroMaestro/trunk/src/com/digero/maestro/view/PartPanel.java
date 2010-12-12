package com.digero.maestro.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.ICompileConstants;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.IDisposable;

@SuppressWarnings("serial")
public class PartPanel extends JPanel implements ICompileConstants {
	private static final int HGAP = 4, VGAP = 4;

	private AbcPart abcPart;
	private SequencerWrapper sequencer;

	private JSpinner numberSpinner;
	private JTextField nameTextField;
	private JComboBox instrumentComboBox;

	private JScrollPane trackScrollPane;

	private JPanel trackListPanel;
	private GroupLayout trackListLayout;
	private GroupLayout.Group trackListVGroup;
	private GroupLayout.Group trackListHGroup;

	private LotroInstrument lastSelectedInstrument = null;

	public PartPanel(SequencerWrapper sequencer) {
		super(new BorderLayout(HGAP, VGAP));

		this.sequencer = sequencer;

		numberSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
		numberSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (abcPart != null)
					abcPart.setPartNumber((Integer) numberSpinner.getValue());
			}
		});

		nameTextField = new JTextField(32);
		nameTextField.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				if (abcPart != null)
					abcPart.setTitle(nameTextField.getText());
			}

			public void insertUpdate(DocumentEvent e) {
				if (abcPart != null)
					abcPart.setTitle(nameTextField.getText());
			}

			public void changedUpdate(DocumentEvent e) {
				if (abcPart != null)
					abcPart.setTitle(nameTextField.getText());
			}
		});

		instrumentComboBox = new JComboBox();
		instrumentComboBox.setMaximumRowCount(12);
		instrumentComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (abcPart != null) {
					LotroInstrument newInstrument = (LotroInstrument) instrumentComboBox.getSelectedItem();
					abcPart.setInstrument(newInstrument);
					String title = abcPart.getTitle();
					title = title.replace(lastSelectedInstrument.toString(), newInstrument.toString());
					nameTextField.setText(title);
					lastSelectedInstrument = newInstrument;
					updateTracksVisible();
				}
			}
		});

		JPanel dataPanel = new JPanel(new BorderLayout(0, VGAP));
		JPanel dataPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, HGAP, 0));
		dataPanel2.add(new JLabel("X:"));
		dataPanel2.add(numberSpinner);
		dataPanel2.add(new JLabel(" I:"));
		dataPanel2.add(instrumentComboBox);
		dataPanel2.add(new JLabel(" T:"));
		dataPanel.add(dataPanel2, BorderLayout.WEST);
		dataPanel.add(nameTextField, BorderLayout.CENTER);

		trackListPanel = new JPanel();
		trackListLayout = new GroupLayout(trackListPanel);
		trackListLayout.setVerticalGroup(trackListVGroup = trackListLayout.createSequentialGroup());
		trackListLayout.setHorizontalGroup(trackListHGroup = trackListLayout.createParallelGroup());
		trackListLayout.setHonorsVisibility(true);
		trackListPanel.setLayout(trackListLayout);
		trackListPanel.setBackground(Color.WHITE);

		trackScrollPane = new JScrollPane(trackListPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		add(dataPanel, BorderLayout.NORTH);
		add(trackScrollPane, BorderLayout.CENTER);

		setAbcPart(null);
	}

	private AbcPartListener abcPartListener = new AbcPartListener() {
		public void abcPartChanged(AbcPartEvent e) {
			if (e.isPartNumber()) {
				numberSpinner.setValue(abcPart.getPartNumber());
			}
		}
	};

	public void setAbcPart(AbcPart abcPart) {
		if (this.abcPart == abcPart)
			return;

		if (this.abcPart != null) {
			try {
				numberSpinner.commitEdit();
			}
			catch (ParseException e) {}
			this.abcPart.removeAbcListener(abcPartListener);
			this.abcPart = null;
		}

		if (abcPart == null) {
			numberSpinner.setEnabled(false);
			nameTextField.setEnabled(false);
			instrumentComboBox.setEnabled(false);

			numberSpinner.setValue(0);
			nameTextField.setText("");
			instrumentComboBox.setSelectedIndex(0);
			lastSelectedInstrument = null;

			clearTrackListPanel();
		}
		else {
			numberSpinner.setEnabled(true);
			nameTextField.setEnabled(true);
			instrumentComboBox.setEnabled(true);

			numberSpinner.setValue(abcPart.getPartNumber());
			nameTextField.setText(abcPart.getTitle());
			instrumentComboBox.setModel(new DefaultComboBoxModel(abcPart.getSupportedInstruments()));
			instrumentComboBox.setSelectedItem(abcPart.getInstrument());
			lastSelectedInstrument = abcPart.getInstrument();

			clearTrackListPanel();

			boolean gray = false;
			for (TrackInfo track : abcPart.getSequenceInfo().getTrackList()) {
				int trackNumber = track.getTrackNumber();
				if (track.hasNotes() || track.hasDrums()) {
					TrackPanel trackPanel = new TrackPanel(track, sequencer, abcPart);
//					trackPanel.setBackground(gray ? Color.LIGHT_GRAY : Color.WHITE);
					gray = !gray;
					trackScrollPane.getVerticalScrollBar().setUnitIncrement(trackPanel.getPreferredSize().height);
//					trackListPanel.add(trackPanel);
					trackListVGroup.addComponent(trackPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
							GroupLayout.PREFERRED_SIZE);
					trackListHGroup.addComponent(trackPanel);

					if (MUTE_DISABLED_TRACKS)
						sequencer.setTrackMute(trackNumber, !abcPart.isTrackEnabled(trackNumber));
				}

				if ((track.hasNotes() || track.hasDrums()) && MUTE_DISABLED_TRACKS) {
					sequencer.setTrackMute(trackNumber, !abcPart.isTrackEnabled(trackNumber));
				}
				else {
					sequencer.setTrackMute(trackNumber, false);
				}

				sequencer.setTrackSolo(trackNumber, false);
			}

			for (TrackInfo track : abcPart.getSequenceInfo().getTrackList()) {
				if (track.hasDrums()) {
					DrumTrackPanel panel = new DrumTrackPanel(track, sequencer, abcPart);
					trackListVGroup.addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
							GroupLayout.PREFERRED_SIZE);
					trackListHGroup.addComponent(panel);

//					for (int drumId : track.getDrumsInUse()) {
//						DrumPanel drumPanel = new DrumPanel(track, sequencer, abcPart, drumId);
////						trackListPanel.add(drumPanel);
//						trackListVGroup.addComponent(drumPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
//								GroupLayout.PREFERRED_SIZE);
//						trackListHGroup.addComponent(drumPanel);
//					}
				}
			}

		}

		this.abcPart = abcPart;
		if (this.abcPart != null) {
			this.abcPart.addAbcListener(abcPartListener);
		}

		updateTracksVisible();
		validate();
		repaint();
	}

	private void updateTracksVisible() {
		if (abcPart == null)
			return;

		boolean percussion = abcPart.getInstrument().isPercussion;
		boolean setHeight = false;

		for (Component child : trackListPanel.getComponents()) {
			if (child instanceof TrackPanel) {
				child.setVisible(!percussion);
				if (!setHeight && !percussion) {
					trackScrollPane.getVerticalScrollBar().setUnitIncrement(child.getPreferredSize().height);
					setHeight = true;
				}
			}
			else if (child instanceof DrumTrackPanel) {
				child.setVisible(percussion);
				if (!setHeight && percussion) {
					trackScrollPane.getVerticalScrollBar()
							.setUnitIncrement(((DrumTrackPanel) child).getUnitIncrement());
					setHeight = true;
				}
			}
			else if (child instanceof DrumPanel) {
				child.setVisible(percussion);
				if (!setHeight && percussion) {
					trackScrollPane.getVerticalScrollBar().setUnitIncrement(child.getPreferredSize().height);
					setHeight = true;
				}
			}
		}
	}

	private void clearTrackListPanel() {
		for (Component child : trackListPanel.getComponents()) {
			if (child instanceof IDisposable) {
				((IDisposable) child).dispose();
			}
		}
		trackListPanel.removeAll();
		trackListLayout.setVerticalGroup(trackListVGroup = trackListLayout.createSequentialGroup());
		trackListLayout.setHorizontalGroup(trackListHGroup = trackListLayout.createParallelGroup());
	}

	public void setSequencer(SequencerWrapper sequencer) {
		AbcPart abcPartTmp = this.abcPart;
		setAbcPart(null);
		this.sequencer = sequencer;
		setAbcPart(abcPartTmp);
	}
}

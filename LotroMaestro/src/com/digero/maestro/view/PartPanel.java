package com.digero.maestro.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import sun.awt.VerticalBagLayout;

import com.digero.maestro.abc.LotroInstrument;
import com.digero.maestro.midi.SequencerWrapper;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.project.AbcPart;

@SuppressWarnings("serial")
public class PartPanel extends JPanel {
	private static final int HGAP = 4, VGAP = 4;

	private ProjectFrame project;
	private AbcPart abcPart;
	private SequencerWrapper sequencer;

	private JSpinner numberSpinner;
	private JTextField nameTextField;
	private JComboBox instrumentComboBox;

	private JPanel trackListPanel;

	public PartPanel(ProjectFrame project) {
		super(new BorderLayout(HGAP, VGAP));

		this.project = project;

		sequencer = new SequencerWrapper();

		numberSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
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
					abcPart.setName(nameTextField.getText());
			}

			public void insertUpdate(DocumentEvent e) {
				if (abcPart != null)
					abcPart.setName(nameTextField.getText());
			}

			public void changedUpdate(DocumentEvent e) {
				if (abcPart != null)
					abcPart.setName(nameTextField.getText());
			}
		});

		instrumentComboBox = new JComboBox(LotroInstrument.NON_DRUMS);
		instrumentComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (abcPart != null)
					abcPart.setInstrument((LotroInstrument) instrumentComboBox.getSelectedItem());
			}
		});

		JPanel dataPanel = new JPanel(new BorderLayout(HGAP, VGAP));
		dataPanel.add(numberSpinner, BorderLayout.WEST);
		dataPanel.add(nameTextField, BorderLayout.CENTER);
		dataPanel.add(instrumentComboBox, BorderLayout.EAST);

		trackListPanel = new JPanel(new VerticalBagLayout());
		trackListPanel.setBackground(Color.WHITE);

		JScrollPane trackScrollPane = new JScrollPane(trackListPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		add(dataPanel, BorderLayout.NORTH);
		add(trackScrollPane, BorderLayout.CENTER);

		setAbcPart(null);
	}

	public void setAbcPart(AbcPart abcPart) {
		this.abcPart = null;
		
		if (abcPart == null) {
			numberSpinner.setEnabled(false);
			nameTextField.setEnabled(false);
			instrumentComboBox.setEnabled(false);

			numberSpinner.setValue(0);
			nameTextField.setText("");
			instrumentComboBox.setSelectedIndex(0);

			trackListPanel.removeAll();
			trackListPanel.validate();
		}
		else {
			numberSpinner.setEnabled(true);
			nameTextField.setEnabled(true);
			instrumentComboBox.setEnabled(true);

			numberSpinner.setValue(abcPart.getPartNumber());
			nameTextField.setText(abcPart.getName());
			instrumentComboBox.setSelectedItem(abcPart.getInstrument());

			trackListPanel.removeAll();
			for (TrackInfo track : abcPart.getSequenceInfo().getTrackList()) {
				if (track.hasNotes()) {
					trackListPanel.add(new TrackPanel(project, track, sequencer, abcPart));
				}
			}
			trackListPanel.validate();
		}
		
		this.abcPart = abcPart;
	}
}

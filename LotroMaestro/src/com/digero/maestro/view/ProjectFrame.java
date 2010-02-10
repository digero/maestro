package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.digero.maestro.midi.KeySignature;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.TimeSignature;
import com.digero.maestro.project.AbcPart;
import com.digero.maestro.project.ArrayListModel;

@SuppressWarnings("serial")
public class ProjectFrame extends JFrame implements TableLayoutConstants {
	private static final int HGAP = 4, VGAP = 4;
	private static final double[] LAYOUT_COLS = new double[] {

	};
	private static final double[] LAYOUT_ROWS = new double[] {

	};

	private File saveFile;
	private final SequenceInfo sequenceInfo;
	private ArrayListModel<AbcPart> parts = new ArrayListModel<AbcPart>();

	private JPanel content;
	private JSpinner transposeSpinner;
	private JSpinner tempoSpinner;
	private JFormattedTextField keySignatureField;
	private JFormattedTextField timeSignatureField;

	private JList partsList;
	private JButton newPartButton;
	private JButton deletePartButton;

	public ProjectFrame(SequenceInfo sequenceInfo) {
		this(sequenceInfo, 0, sequenceInfo.getTempo(), sequenceInfo.getTimeSignature(), sequenceInfo.getKeySignature());
	}

	public ProjectFrame(SequenceInfo sequenceInfo, int transpose, int tempoBPM, TimeSignature timeSignature,
			KeySignature keySignature) {
		super("LotRO Maestro");

		setBounds(200, 200, 800, 600);

		this.sequenceInfo = sequenceInfo;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		TableLayout tableLayout = new TableLayout(LAYOUT_COLS, LAYOUT_ROWS);
		tableLayout.setHGap(HGAP);
		tableLayout.setVGap(VGAP);

		content = new JPanel(tableLayout);
		setContentPane(content);

		keySignatureField = new MyFormattedTextField(keySignature, 5);
		timeSignatureField = new MyFormattedTextField(timeSignature, 5);
		transposeSpinner = new JSpinner(new SpinnerNumberModel(transpose, -48, 48, 1));
		tempoSpinner = new JSpinner(new SpinnerNumberModel(tempoBPM, 1, 600, 2));

		parts.add(new AbcPart(sequenceInfo, calcNextPartNumber()));

		partsList = new JList(parts);
		partsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		partsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				// TODO Auto-generated method stub
			}
		});

		JScrollPane partsListScrollPane = new JScrollPane(partsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		newPartButton = new JButton("New Part");
		newPartButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbcPart newPart = new AbcPart(ProjectFrame.this.sequenceInfo, calcNextPartNumber());
				parts.add(newPart);
				partsList.setSelectedIndex(parts.getList().indexOf(newPart));
			}
		});

		deletePartButton = new JButton("Delete");
		deletePartButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String title = "Delete Part";
				String msg = "Are you sure you want to delete the selected part?\nThis cannot be undone.";
				int r = JOptionPane.showConfirmDialog(ProjectFrame.this, msg, title, JOptionPane.YES_NO_OPTION);
				if (r == JOptionPane.YES_OPTION) {
					parts.remove(partsList.getSelectedIndex());
				}
			}
		});

		JPanel partsButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
		partsButtonPanel.add(newPartButton);
		partsButtonPanel.add(deletePartButton);

		JPanel partsPanel = new JPanel(new BorderLayout(HGAP, VGAP));
		partsPanel.setBorder(BorderFactory.createTitledBorder("Song Parts"));
		partsPanel.add(partsListScrollPane, BorderLayout.CENTER);
		partsPanel.add(partsButtonPanel, BorderLayout.SOUTH);

		TableLayout settingsLayout = new TableLayout(new double[] {
				/* Cols */PREFERRED, PREFERRED, FILL
		}, new double[] {
				/* Rows */PREFERRED, PREFERRED, PREFERRED, PREFERRED
		});
		settingsLayout.setVGap(VGAP);
		settingsLayout.setHGap(HGAP);

		JPanel settingsPanel = new JPanel(settingsLayout);
		settingsPanel.setBorder(BorderFactory.createTitledBorder("Export Settings"));
		settingsPanel.add(new JLabel("Transpose:"), "0, 0");
		settingsPanel.add(new JLabel("Tempo:"), "0, 1");
		settingsPanel.add(new JLabel("Meter:"), "0, 2");
		settingsPanel.add(new JLabel("Key:"), "0, 3");
		settingsPanel.add(transposeSpinner, "1, 0");
		settingsPanel.add(tempoSpinner, "1, 1");
		settingsPanel.add(timeSignatureField, "1, 2, L, F");
		settingsPanel.add(keySignatureField, "1, 3, L, F");

	}

	public int getTranspose() {
		return (Integer) transposeSpinner.getValue();
	}

	public int getTempo() {
		return (Integer) tempoSpinner.getValue();
	}

	public KeySignature getKeySignature() {
		return (KeySignature) keySignatureField.getValue();
	}

	public TimeSignature getTimeSignature() {
		return (TimeSignature) timeSignatureField.getValue();
	}

	public int calcNextPartNumber() {
		int size = parts.getSize();
		outerLoop: for (int n = 1; n <= size; n++) {
			for (int i = 0; i < size; i++) {
				if (n == parts.getElementAt(i).getPartNumber())
					continue outerLoop;
			}
			return n;
		}
		return size + 1;
	}

	/**
	 * Slight modification to JFormattedTextField to select the contents when it
	 * receives focus.
	 */
	private class MyFormattedTextField extends JFormattedTextField {
		public MyFormattedTextField(Object value, int columns) {
			super(value);
			setColumns(columns);
		}

		@Override
		protected void processFocusEvent(FocusEvent e) {
			super.processFocusEvent(e);
			if (e.getID() == FocusEvent.FOCUS_GAINED)
				selectAll();
		}
	}
}

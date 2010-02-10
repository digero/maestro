package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.digero.maestro.midi.KeySignature;
import com.digero.maestro.midi.TimeSignature;
import com.digero.maestro.project.AbcPart;
import com.digero.maestro.project.AbcProject;

@SuppressWarnings("serial")
public class ProjectFrame extends JFrame implements TableLayoutConstants {
	private static final double[] LAYOUT_COLS = new double[] {

	};
	private static final double[] LAYOUT_ROWS = new double[] {

	};

	private AbcProject abcProject;

	private JPanel content;
	private JSpinner transposeSpinner;
	private JSpinner tempoSpinner;
	private JFormattedTextField keySignatureField;
	private JFormattedTextField timeSignatureField;
	private JList partsList;

	public ProjectFrame(AbcProject project) {
		super("LotRO Maestro");
		this.abcProject = project;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		TableLayout tableLayout = new TableLayout(LAYOUT_COLS, LAYOUT_ROWS);
		tableLayout.setHGap(4);
		tableLayout.setVGap(4);

		content = new JPanel(tableLayout);
		setContentPane(content);

		keySignatureField = new MyFormattedTextField(abcProject.getKeySignature());
		keySignatureField.setColumns(5);
		keySignatureField.addPropertyChangeListener("value", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() instanceof KeySignature) {
					abcProject.setKeySignature((KeySignature) evt.getNewValue(), ProjectFrame.this);
				}
			}
		});

		timeSignatureField = new MyFormattedTextField(abcProject.getTimeSignature());
		timeSignatureField.setColumns(5);
		timeSignatureField.addPropertyChangeListener("value", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() instanceof TimeSignature) {
					abcProject.setTimeSignature((TimeSignature) evt.getNewValue(), ProjectFrame.this);
				}
			}
		});

		transposeSpinner = new JSpinner(new SpinnerNumberModel(0, -48, 48, 1));
		transposeSpinner.setValue(abcProject.getTranspose());
		transposeSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				abcProject.setTranspose((Integer) transposeSpinner.getValue(), ProjectFrame.this);
			}
		});

		tempoSpinner = new JSpinner(new SpinnerNumberModel(120, 1, 600, 2));
		tempoSpinner.setValue(abcProject.getTempoBPM());
		tempoSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				abcProject.setTempoBPM((Integer) tempoSpinner.getValue(), ProjectFrame.this);
			}
		});

		partsList = new JList(abcProject.getPartsListModel());
		partsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		partsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				// TODO Auto-generated method stub
			}
		});
		

		JScrollPane partsListScrollPane = new JScrollPane(partsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		abcProject.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (e.getSource() != ProjectFrame.this) {
					keySignatureField.setValue(abcProject.getKeySignature());
					timeSignatureField.setValue(abcProject.getTimeSignature());
					transposeSpinner.setValue(abcProject.getTranspose());
					tempoSpinner.setValue(abcProject.getTempoBPM());
				}
			}
		});
	}

	/**
	 * Slight modification to JFormattedTextField to select the contents when it
	 * receives focus.
	 */
	private class MyFormattedTextField extends JFormattedTextField {
		public MyFormattedTextField(Object value) {
			super(value);
		}

		@Override
		protected void processFocusEvent(FocusEvent e) {
			super.processFocusEvent(e);
			if (e.getID() == FocusEvent.FOCUS_GAINED)
				selectAll();
		}
	}

	private class PartListPanel extends JPanel {
		public PartListPanel() {
			super(new GridLayout(0, 1));

			for (AbcPart part : abcProject.getParts()) {
				// TODO
			}
		}
	}
}

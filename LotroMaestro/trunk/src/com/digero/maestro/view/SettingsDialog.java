package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.common.abc.LotroInstrument;
import com.digero.maestro.abc.PartAutoNumberer;

public class SettingsDialog extends JDialog implements TableLayoutConstants {
	private boolean success = false;
	private boolean numbererSettingsChanged = false;

	private PartAutoNumberer.Settings numSettings;

	private JPanel mainPanel;

	private JPanel numberingPanel;
	private JComboBox<Integer> incrementComboBox;

	public SettingsDialog(JFrame owner, PartAutoNumberer.Settings numbererSettings) {
		super(owner, "Settings", true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		this.numSettings = numbererSettings;

		mainPanel = new JPanel(new BorderLayout(4, 4));

		TableLayout numberingLayout = new TableLayout(new double[] {
				/* Cols */PREFERRED, PREFERRED
		}, new double[] {
			/* Rows */PREFERRED
		});
		numberingLayout.setHGap(4);
		numberingLayout.setVGap(4);
		numberingPanel = new JPanel(numberingLayout);
		numberingPanel.setBorder(BorderFactory.createTitledBorder("Automatic Part Numbering"));

		JPanel incrementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel incrementLabel = new JLabel("Increment: ");
		incrementComboBox = new JComboBox<Integer>(new Integer[] {
				1, 10
		});
		incrementComboBox.setSelectedItem(numbererSettings.getIncrement());
		incrementComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int oldInc = numSettings.getIncrement();
				int newInc = (Integer) incrementComboBox.getSelectedItem();
				if (oldInc == newInc)
					return;

				numbererSettingsChanged = true;
				for (Component c : numberingPanel.getComponents()) {
					if (c instanceof InstrumentSpinner) {
						InstrumentSpinner spinner = (InstrumentSpinner) c;
						int firstNumber = numSettings.getFirstNumber(spinner.instrument);
						firstNumber = (firstNumber * oldInc) / newInc;
						numSettings.setFirstNumber(spinner.instrument, firstNumber);
						spinner.setValue(firstNumber);

						if (newInc == 1) {
							spinner.getModel().setMaximum(999);
//							spinner.getModel().setStepSize(10);
						}
						else {
							spinner.getModel().setMaximum(10);
//							spinner.getModel().setStepSize(1);
						}
					}
				}

				numSettings.setIncrementByTen(newInc == 10);
			}
		});
		incrementPanel.add(incrementLabel);
		incrementPanel.add(incrementComboBox);

		numberingPanel.add(incrementPanel, "0, 0, 1, 0, f, f");

		for (LotroInstrument inst : LotroInstrument.values()) {
			int i = numberingLayout.getNumRow();
			numberingLayout.insertRow(i, PREFERRED);
			numberingPanel.add(new JLabel(inst.toString() + " "), "0, " + i);
			numberingPanel.add(new InstrumentSpinner(inst), "1, " + i);
		}

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				success = true;
				SettingsDialog.this.setVisible(false);
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				success = false;
				SettingsDialog.this.setVisible(false);
			}
		});
		JPanel buttonsPanel = new JPanel(new TableLayout(new double[] {
				0.50, 0.50
		}, new double[] {
			PREFERRED
		}));
		buttonsPanel.add(okButton, "0, 0, f, f");
		buttonsPanel.add(cancelButton, "1, 0, f, f");
		JPanel buttonsContainerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonsContainerPanel.add(buttonsPanel);

		mainPanel.add(numberingPanel, BorderLayout.CENTER);
		mainPanel.add(buttonsContainerPanel, BorderLayout.SOUTH);

		setContentPane(mainPanel);
		pack();
	}

	private class InstrumentSpinner extends JSpinner implements ChangeListener {
		private LotroInstrument instrument;

		public InstrumentSpinner(LotroInstrument instrument) {
			super(new SpinnerNumberModel(numSettings.getFirstNumber(instrument), 1, numSettings.isIncrementByTen() ? 10
					: 999, numSettings.isIncrementByTen() ? 1 : 10));
			this.instrument = instrument;
			addChangeListener(this);
		}

		@Override
		public SpinnerNumberModel getModel() {
			return (SpinnerNumberModel) super.getModel();
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			numSettings.setFirstNumber(instrument, (Integer) getValue());
			numbererSettingsChanged = true;
		}
	}

	public boolean isSuccess() {
		return success;
	}

	public boolean isNumbererSettingsChanged() {
		return numbererSettingsChanged;
	}

	public PartAutoNumberer.Settings getNumbererSettings() {
		return numSettings;
	}
}

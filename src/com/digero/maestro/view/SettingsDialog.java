package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.util.Util;
import com.digero.maestro.abc.AbcMetadataSource;
import com.digero.maestro.abc.AbcPartMetadataSource;
import com.digero.maestro.abc.PartAutoNumberer;
import com.digero.maestro.abc.PartNameTemplate;

public class SettingsDialog extends JDialog implements TableLayoutConstants {
	public static final int NUMBERING_TAB = 0;
	public static final int NAME_TEMPLATE_TAB = 1;

	private static final int PAD = 4;

	private boolean success = false;
	private boolean numbererSettingsChanged = false;

	private JTabbedPane tabPanel;

	private PartAutoNumberer.Settings numSettings;
	private JPanel numberingPanel;
	private JComboBox<Integer> incrementComboBox;

	private PartNameTemplate.Settings nameTemplateSettings;
	private PartNameTemplate nameTemplate;
	private JPanel nameTemplatePanel;
	private JLabel nameTemplateExampleLabel;
	private JTextField partNameTextField;

	public SettingsDialog(JFrame owner, PartAutoNumberer.Settings numbererSettings, PartNameTemplate nameTemplate) {
		super(owner, "Options", true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		this.numSettings = numbererSettings;

		this.nameTemplate = nameTemplate;
		this.nameTemplateSettings = nameTemplate.getSettingsCopy();

		createNumberingPanel();
		createNameTemplatePanel();

		JButton okButton = new JButton("OK");
		getRootPane().setDefaultButton(okButton);
		okButton.setMnemonic('O');
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				success = true;
				SettingsDialog.this.setVisible(false);
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic('C');
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				success = false;
				SettingsDialog.this.setVisible(false);
			}
		});

		final String CLOSE_WINDOW_ACTION = "com.digero.maestro.view.SettingsDialog:CLOSE_WINDOW_ACTION";
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				CLOSE_WINDOW_ACTION);
		getRootPane().getActionMap().put(CLOSE_WINDOW_ACTION, new AbstractAction() {
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
		((TableLayout) buttonsPanel.getLayout()).setHGap(PAD);
		buttonsPanel.add(okButton, "0, 0, f, f");
		buttonsPanel.add(cancelButton, "1, 0, f, f");
		JPanel buttonsContainerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, PAD / 2));
		buttonsContainerPanel.add(buttonsPanel);

		tabPanel = new JTabbedPane();
		JPanel numberingPanelContainer = new JPanel(new BorderLayout());
		numberingPanelContainer.add(numberingPanel, BorderLayout.WEST);
		tabPanel.addTab("ABC Part Numbering", numberingPanelContainer); // NUMBERING_TAB
		tabPanel.addTab("ABC Part Naming", nameTemplatePanel); // NAME_TEMPLATE_TAB

		JPanel mainPanel = new JPanel(new BorderLayout(PAD, PAD));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
		mainPanel.add(tabPanel, BorderLayout.CENTER);
		mainPanel.add(buttonsContainerPanel, BorderLayout.SOUTH);

		setContentPane(mainPanel);
		pack();

		if (owner != null) {
			int left = owner.getX() + (owner.getWidth() - this.getWidth()) / 2;
			int top = owner.getY() + (owner.getHeight() - this.getHeight()) / 2;
			this.setLocation(left, top);
		}

		updateNameTemplateExample();
	}

	private void createNumberingPanel() {
		TableLayout numberingLayout = new TableLayout(new double[] {
				/* Cols */0.5, PREFERRED, 2 * PAD, 0.5, PREFERRED
		}, new double[] {
			/* Rows */PREFERRED
		});
		numberingLayout.setHGap(PAD);
		numberingLayout.setVGap(PAD / 2);
		numberingPanel = new JPanel(numberingLayout);
		numberingPanel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

		JPanel incrementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		incrementComboBox = new JComboBox<Integer>(new Integer[] {
				1, 10
		});
		incrementComboBox.setSelectedItem(numSettings.getIncrement());
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
		JLabel incrementLabel = new JLabel("<html>Interval between parts of<br>the same instrument type: </html>");
		incrementLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, PAD, 2 * PAD));
		incrementPanel.add(incrementLabel);
		incrementPanel.add(incrementComboBox);

		numberingPanel.add(incrementPanel, "0, 0, 4, 0, f, f");

		LotroInstrument[] instruments = LotroInstrument.values();
		for (int i = 0; i < instruments.length; i++) {
			LotroInstrument inst = instruments[i];

			int row = i + 1;
			int col = 0;
			if (i >= (instruments.length + 1) / 2) {
				row -= (instruments.length + 1) / 2;
				col = 3;
			}
			else {
				numberingLayout.insertRow(row, PREFERRED);
			}
			numberingPanel.add(new InstrumentSpinner(inst), col + ", " + row);
			numberingPanel.add(new JLabel(inst.toString() + " "), (col + 1) + ", " + row);
		}
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

	private void createNameTemplatePanel() {
		partNameTextField = new JTextField(nameTemplateSettings.getPartNamePattern(), 40);
		partNameTextField.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				nameTemplateSettings.setPartNamePattern(partNameTextField.getText());
				updateNameTemplateExample();
			}

			public void insertUpdate(DocumentEvent e) {
				nameTemplateSettings.setPartNamePattern(partNameTextField.getText());
				updateNameTemplateExample();
			}

			public void changedUpdate(DocumentEvent e) {
				nameTemplateSettings.setPartNamePattern(partNameTextField.getText());
				updateNameTemplateExample();
			}
		});

		nameTemplateExampleLabel = new JLabel(" ");

		TableLayout layout = new TableLayout();
		layout.insertColumn(0, PREFERRED);
		layout.insertColumn(1, FILL);
		layout.setVGap(PAD);
		layout.setHGap(PAD * 2);

		nameTemplatePanel = new JPanel(layout);
		nameTemplatePanel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

		int row = 0;
		layout.insertRow(row, PREFERRED);
		nameTemplatePanel.add(new JLabel("Pattern for ABC Part Name:"), "0, " + row + ", 1, " + row);

		layout.insertRow(++row, PREFERRED);
		nameTemplatePanel.add(partNameTextField, "0, " + row + ", 1, " + row);

		layout.insertRow(++row, PREFERRED);
		nameTemplatePanel.add(nameTemplateExampleLabel, "0, " + row + ", 1, " + row + ", F, F");

		layout.insertRow(++row, PREFERRED);

		JLabel nameLabel = new JLabel("<html><u>Variable Name</u></html>");
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
		JLabel exampleLabel = new JLabel("<html><u>Example</u></html>");
		exampleLabel.setFont(nameLabel.getFont());

		layout.insertRow(++row, PREFERRED);
		nameTemplatePanel.add(nameLabel, "0, " + row);
		nameTemplatePanel.add(exampleLabel, "1, " + row);

		AbcMetadataSource originalMetadataSource = nameTemplate.getMetadataSource();
		AbcPartMetadataSource originalAbcPart = nameTemplate.getCurrentAbcPart();

		MockMetadataSource mockMetadata = new MockMetadataSource(originalMetadataSource);
		nameTemplate.setMetadataSource(mockMetadata);
		nameTemplate.setCurrentAbcPart(mockMetadata);
		for (Entry<String, PartNameTemplate.Variable> entry : nameTemplate.getVariables().entrySet()) {
			String tooltipText = "<html><b>" + entry.getKey() + "</b><br>"
					+ entry.getValue().getDescription().replace("\n", "<br>");

			JLabel keyLabel = new JLabel(entry.getKey());
			keyLabel.setFont(nameLabel.getFont());
			keyLabel.setToolTipText(tooltipText);
			JLabel descriptionLabel = new JLabel(entry.getValue().getValue());
			descriptionLabel.setToolTipText(tooltipText);

			layout.insertRow(++row, PREFERRED);
			nameTemplatePanel.add(keyLabel, "0, " + row);
			nameTemplatePanel.add(descriptionLabel, "1, " + row);
		}
		nameTemplate.setMetadataSource(originalMetadataSource);
		nameTemplate.setCurrentAbcPart(originalAbcPart);
	}

	private void updateNameTemplateExample() {
		AbcMetadataSource originalMetadataSource = nameTemplate.getMetadataSource();
		MockMetadataSource mockMetadata = new MockMetadataSource(originalMetadataSource);
		nameTemplate.setMetadataSource(mockMetadata);

		String exampleText = nameTemplate.formatName(nameTemplateSettings.getPartNamePattern(), mockMetadata);
		exampleText = Util.ellipsis(exampleText, nameTemplateExampleLabel.getWidth(),
				nameTemplateExampleLabel.getFont());
		nameTemplateExampleLabel.setText(exampleText);

		nameTemplate.setMetadataSource(originalMetadataSource);
	}

	public void setActiveTab(int tab) {
		if (tab >= 0 && tab < tabPanel.getComponentCount())
			tabPanel.setSelectedIndex(tab);
	}

	public int getActiveTab() {
		return tabPanel.getSelectedIndex();
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

	public PartNameTemplate.Settings getNameTemplateSettings() {
		return nameTemplateSettings;
	}

	private static class MockMetadataSource implements AbcMetadataSource, AbcPartMetadataSource {
		private AbcMetadataSource originalSource;

		public MockMetadataSource(AbcMetadataSource originalSource) {
			this.originalSource = originalSource;
		}

		@Override
		public String getTitle() {
			return "Lute";
		}

		@Override
		public int getPartNumber() {
			return 1;
		}

		@Override
		public String getSongTitle() {
			if (originalSource != null && originalSource.getSongTitle().length() > 0)
				return originalSource.getSongTitle();

			return "Example Title";
		}

		@Override
		public String getComposer() {
			if (originalSource != null && originalSource.getComposer().length() > 0)
				return originalSource.getComposer();

			return "Example Composer";
		}

		@Override
		public String getTranscriber() {
			if (originalSource != null && originalSource.getTranscriber().length() > 0)
				return originalSource.getTranscriber();

			return "Your Name Here";
		}

		@Override
		public long getSongLengthMicros() {
			long length = 0;
			if (originalSource != null)
				length = originalSource.getSongLengthMicros();

			return (length != 0) ? length : 227000000/* 3:47 */;
		}

		@Override
		public File getSaveFile() {
			if (originalSource != null) {
				File saveFile = originalSource.getSaveFile();
				if (saveFile != null)
					return saveFile;
			}

			return new File(Util.getLotroMusicPath(false), "band/examplesong.abc");
		}

		@Override
		public String getPartName(AbcPartMetadataSource abcPart) {
			return null;
		}
	}
}

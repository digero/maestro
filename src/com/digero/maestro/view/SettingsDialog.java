package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import com.digero.maestro.abc.AbcSong;
import com.digero.maestro.abc.PartAutoNumberer;
import com.digero.maestro.abc.PartNameTemplate;

public class SettingsDialog extends JDialog implements TableLayoutConstants
{
	public static final int NUMBERING_TAB = 0;
	public static final int NAME_TEMPLATE_TAB = 1;
	public static final int SAVE_EXPORT_TAB = 2;

	private static final int PAD = 4;

	private boolean success = false;
	private boolean numbererSettingsChanged = false;

	private JTabbedPane tabPanel;

	private PartAutoNumberer.Settings numSettings;

	private PartNameTemplate.Settings nameTemplateSettings;
	private PartNameTemplate nameTemplate;
	private JLabel nameTemplateExampleLabel;

	private SaveAndExportSettings saveSettings;

	public SettingsDialog(JFrame owner, PartAutoNumberer.Settings numbererSettings, PartNameTemplate nameTemplate,
			SaveAndExportSettings saveSettings)
	{
		super(owner, "Options", true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		this.numSettings = numbererSettings;

		this.nameTemplate = nameTemplate;
		this.nameTemplateSettings = nameTemplate.getSettingsCopy();

		this.saveSettings = saveSettings;

		JButton okButton = new JButton("OK");
		getRootPane().setDefaultButton(okButton);
		okButton.setMnemonic('O');
		okButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				success = true;
				SettingsDialog.this.setVisible(false);
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic('C');
		cancelButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				success = false;
				SettingsDialog.this.setVisible(false);
			}
		});

		final String CLOSE_WINDOW_ACTION = "com.digero.maestro.view.SettingsDialog:CLOSE_WINDOW_ACTION";
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				CLOSE_WINDOW_ACTION);
		getRootPane().getActionMap().put(CLOSE_WINDOW_ACTION, new AbstractAction()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				success = false;
				SettingsDialog.this.setVisible(false);
			}
		});

		JPanel buttonsPanel = new JPanel(new TableLayout(//
				new double[] { 0.50, 0.50 },//
				new double[] { PREFERRED }));
		((TableLayout) buttonsPanel.getLayout()).setHGap(PAD);
		buttonsPanel.add(okButton, "0, 0, f, f");
		buttonsPanel.add(cancelButton, "1, 0, f, f");
		JPanel buttonsContainerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, PAD / 2));
		buttonsContainerPanel.add(buttonsPanel);

		tabPanel = new JTabbedPane();
		tabPanel.addTab("ABC Part Numbering", createNumberingPanel()); // NUMBERING_TAB
		tabPanel.addTab("ABC Part Naming", createNameTemplatePanel()); // NAME_TEMPLATE_TAB
		tabPanel.addTab("Save & Export", createSaveAndExportSettingsPanel()); // SAVE_EXPORT_TAB

		JPanel mainPanel = new JPanel(new BorderLayout(PAD, PAD));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
		mainPanel.add(tabPanel, BorderLayout.CENTER);
		mainPanel.add(buttonsContainerPanel, BorderLayout.SOUTH);

		setContentPane(mainPanel);
		pack();

		if (owner != null)
		{
			int left = owner.getX() + (owner.getWidth() - this.getWidth()) / 2;
			int top = owner.getY() + (owner.getHeight() - this.getHeight()) / 2;
			this.setLocation(left, top);
		}

		// This must be done after layout is done: the call to pack() does layout
		updateNameTemplateExample();
	}

	private JPanel createNumberingPanel()
	{
		JLabel instrumentsTitle = new JLabel("<html><b><u>First part number</u></b></html>");

		TableLayout instrumentsLayout = new TableLayout(//
				new double[] { 50, PREFERRED, 2 * PAD, 50, PREFERRED },//
				new double[] { });
		instrumentsLayout.setHGap(PAD);
		instrumentsLayout.setVGap(3);
		JPanel instrumentsPanel = new JPanel(instrumentsLayout);
		instrumentsPanel.setBorder(BorderFactory.createEmptyBorder(0, PAD, 0, 0));

		final List<InstrumentSpinner> instrumentSpinners = new ArrayList<InstrumentSpinner>();
		LotroInstrument[] instruments = LotroInstrument.values();
		for (int i = 0; i < instruments.length; i++)
		{
			LotroInstrument inst = instruments[i];

			int row = i;
			int col = 0;
			if (i >= (instruments.length + 1) / 2)
			{
				row -= (instruments.length + 1) / 2;
				col = 3;
			}
			else
			{
				instrumentsLayout.insertRow(row, PREFERRED);
			}
			InstrumentSpinner spinner = new InstrumentSpinner(inst);
			instrumentSpinners.add(spinner);
			instrumentsPanel.add(spinner, col + ", " + row);
			instrumentsPanel.add(new JLabel(inst.toString() + " "), (col + 1) + ", " + row);
		}

		JLabel incrementTitle = new JLabel("<html><b><u>Increment</u></b></html>");
		JLabel incrementDescr = new JLabel("<html>Interval between multiple parts of the same instrument.<br>"
				+ "<b>1</b>: number Lute parts as 10, 11, 12, etc.<br>"
				+ "<b>10</b>: number Lute parts as 1, 11, 21, etc.</html>");

		final JComboBox<Integer> incrementComboBox = new JComboBox<Integer>(new Integer[] { 1, 10 });
		incrementComboBox.setSelectedItem(numSettings.getIncrement());
		incrementComboBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				int oldInc = numSettings.getIncrement();
				int newInc = (Integer) incrementComboBox.getSelectedItem();
				if (oldInc == newInc)
					return;

				numbererSettingsChanged = true;
				for (InstrumentSpinner spinner : instrumentSpinners)
				{
					int firstNumber = numSettings.getFirstNumber(spinner.instrument);
					firstNumber = (firstNumber * oldInc) / newInc;
					numSettings.setFirstNumber(spinner.instrument, firstNumber);
					spinner.setValue(firstNumber);

					if (newInc == 1)
					{
						spinner.getModel().setMaximum(999);
					}
					else
					{
						spinner.getModel().setMaximum(10);
					}
				}

				numSettings.setIncrementByTen(newInc == 10);
			}
		});

		TableLayout incrementPanelLayout = new TableLayout(//
				new double[] { PREFERRED, FILL },//
				new double[] { PREFERRED });
		incrementPanelLayout.setHGap(10);
		JPanel incrementPanel = new JPanel(incrementPanelLayout);
		incrementPanel.setBorder(BorderFactory.createEmptyBorder(0, PAD, 0, 0));
		incrementPanel.add(incrementComboBox, "0, 0, C, T");
		incrementPanel.add(incrementDescr, "1, 0");

		TableLayout numberingLayout = new TableLayout(//
				new double[] { FILL },//
				new double[] { PREFERRED, PREFERRED, PREFERRED, PREFERRED, PREFERRED });

		numberingLayout.setVGap(PAD);
		JPanel numberingPanel = new JPanel(numberingLayout);
		numberingPanel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

		numberingPanel.add(instrumentsTitle, "0, 0");
		numberingPanel.add(instrumentsPanel, "0, 1, L, F");
		numberingPanel.add(incrementTitle, "0, 3");
		numberingPanel.add(incrementPanel, "0, 4, F, F");
		return numberingPanel;
	}

	private class InstrumentSpinner extends JSpinner implements ChangeListener
	{
		private LotroInstrument instrument;

		public InstrumentSpinner(LotroInstrument instrument)
		{
			super(new SpinnerNumberModel(numSettings.getFirstNumber(instrument), 0, numSettings.isIncrementByTen() ? 10
					: 999, 1));

			this.instrument = instrument;
			addChangeListener(this);
		}

		@Override public SpinnerNumberModel getModel()
		{
			return (SpinnerNumberModel) super.getModel();
		}

		@Override public void stateChanged(ChangeEvent e)
		{
			numSettings.setFirstNumber(instrument, (Integer) getValue());
			numbererSettingsChanged = true;
		}
	}

	private JPanel createNameTemplatePanel()
	{
		final JTextField partNameTextField = new JTextField(nameTemplateSettings.getPartNamePattern(), 40);
		partNameTextField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void removeUpdate(DocumentEvent e)
			{
				nameTemplateSettings.setPartNamePattern(partNameTextField.getText());
				updateNameTemplateExample();
			}

			@Override public void insertUpdate(DocumentEvent e)
			{
				nameTemplateSettings.setPartNamePattern(partNameTextField.getText());
				updateNameTemplateExample();
			}

			@Override public void changedUpdate(DocumentEvent e)
			{
				nameTemplateSettings.setPartNamePattern(partNameTextField.getText());
				updateNameTemplateExample();
			}
		});

		nameTemplateExampleLabel = new JLabel(" ");
		JPanel examplePanel = new JPanel(new BorderLayout());
		examplePanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
		examplePanel.add(nameTemplateExampleLabel, BorderLayout.CENTER);

		TableLayout layout = new TableLayout();
		layout.insertColumn(0, PREFERRED);
		layout.insertColumn(1, FILL);
		layout.setVGap(3);
		layout.setHGap(10);

		JPanel nameTemplatePanel = new JPanel(layout);
		nameTemplatePanel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

		int row = 0;
		layout.insertRow(row, PREFERRED);
		JLabel patternLabel = new JLabel("<html><b><u>Pattern for ABC Part Name</b></u></html>");
		nameTemplatePanel.add(patternLabel, "0, " + row + ", 1, " + row);

		layout.insertRow(++row, PREFERRED);
		nameTemplatePanel.add(partNameTextField, "0, " + row + ", 1, " + row);

		layout.insertRow(++row, PREFERRED);
		nameTemplatePanel.add(examplePanel, "0, " + row + ", 1, " + row + ", F, F");

		layout.insertRow(++row, PREFERRED);

		JLabel nameLabel = new JLabel("<html><u><b>Variable Name</b></u></html>");
		JLabel exampleLabel = new JLabel("<html><u><b>Example</b></u></html>");

		layout.insertRow(++row, PREFERRED);
		nameTemplatePanel.add(nameLabel, "0, " + row);
		nameTemplatePanel.add(exampleLabel, "1, " + row);

		AbcMetadataSource originalMetadataSource = nameTemplate.getMetadataSource();
		AbcPartMetadataSource originalAbcPart = nameTemplate.getCurrentAbcPart();

		MockMetadataSource mockMetadata = new MockMetadataSource(originalMetadataSource);
		nameTemplate.setMetadataSource(mockMetadata);
		nameTemplate.setCurrentAbcPart(mockMetadata);
		for (Entry<String, PartNameTemplate.Variable> entry : nameTemplate.getVariables().entrySet())
		{
			String tooltipText = "<html><b>" + entry.getKey() + "</b><br>"
					+ entry.getValue().getDescription().replace("\n", "<br>") + "</html>";

			JLabel keyLabel = new JLabel(entry.getKey());
			keyLabel.setToolTipText(tooltipText);
			JLabel descriptionLabel = new JLabel(entry.getValue().getValue());
			descriptionLabel.setToolTipText(tooltipText);

			layout.insertRow(++row, PREFERRED);
			nameTemplatePanel.add(keyLabel, "0, " + row);
			nameTemplatePanel.add(descriptionLabel, "1, " + row);
		}
		nameTemplate.setMetadataSource(originalMetadataSource);
		nameTemplate.setCurrentAbcPart(originalAbcPart);

		return nameTemplatePanel;
	}

	private void updateNameTemplateExample()
	{
		AbcMetadataSource originalMetadataSource = nameTemplate.getMetadataSource();
		MockMetadataSource mockMetadata = new MockMetadataSource(originalMetadataSource);
		nameTemplate.setMetadataSource(mockMetadata);

		String exampleText = nameTemplate.formatName(nameTemplateSettings.getPartNamePattern(), mockMetadata);
		String exampleTextEllipsis = Util.ellipsis(exampleText, nameTemplateExampleLabel.getWidth(),
				nameTemplateExampleLabel.getFont());

		nameTemplateExampleLabel.setText(exampleTextEllipsis);
		if (!exampleText.equals(exampleTextEllipsis))
			nameTemplateExampleLabel.setToolTipText(exampleText);

		nameTemplate.setMetadataSource(originalMetadataSource);
	}

	private JPanel createSaveAndExportSettingsPanel()
	{
		JLabel titleLabel = new JLabel("<html><u><b>Save &amp; Export</b></u></html>");

		final JCheckBox promptSaveCheckBox = new JCheckBox("Prompt to save new " + AbcSong.MSX_FILE_DESCRIPTION_PLURAL);
		promptSaveCheckBox.setToolTipText("<html>Select to be prompted to save new "
				+ AbcSong.MSX_FILE_DESCRIPTION_PLURAL + "<br>"
				+ "when opening a new file or closing the application.</html>");
		promptSaveCheckBox.setSelected(saveSettings.promptSaveNewSong);
		promptSaveCheckBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				saveSettings.promptSaveNewSong = promptSaveCheckBox.isSelected();
			}
		});

		final JCheckBox showExportFileChooserCheckBox = new JCheckBox(
				"Always prompt for the ABC file name when exporting");
		showExportFileChooserCheckBox.setToolTipText("<html>Select to have the <b>Export ABC</b> button always<br>"
				+ "prompt for the name of the file.</html>");
		showExportFileChooserCheckBox.setSelected(saveSettings.showExportFileChooser);
		showExportFileChooserCheckBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				saveSettings.showExportFileChooser = showExportFileChooserCheckBox.isSelected();
			}
		});

		final JCheckBox skipSilenceAtStartCheckBox = new JCheckBox("Remove silence from start of exported ABC");
		skipSilenceAtStartCheckBox.setToolTipText("<html>" //
				+ "Exported ABC files will not include silent measures from the<br>" //
				+ "beginning of the song.<br>" //
				+ "<br>" //
				+ "Uncheck if you want to export multiple ABC files from the same<br>" //
				+ "MIDI file that will be played together and need to line up." //
				+ "</html>");
		skipSilenceAtStartCheckBox.setSelected(saveSettings.skipSilenceAtStart);
		skipSilenceAtStartCheckBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				saveSettings.skipSilenceAtStart = skipSilenceAtStartCheckBox.isSelected();
			}
		});

		TableLayout layout = new TableLayout();
		layout.insertColumn(0, FILL);
		layout.setVGap(PAD);

		JPanel panel = new JPanel(layout);
		panel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

		int row = -1;

		layout.insertRow(++row, PREFERRED);
		panel.add(titleLabel, "0, " + row);

		layout.insertRow(++row, PREFERRED);
		panel.add(promptSaveCheckBox, "0, " + row);

		layout.insertRow(++row, PREFERRED);
		panel.add(showExportFileChooserCheckBox, "0, " + row);

		layout.insertRow(++row, PREFERRED);
		panel.add(skipSilenceAtStartCheckBox, "0, " + row);

		return panel;
	}

	public void setActiveTab(int tab)
	{
		if (tab >= 0 && tab < tabPanel.getComponentCount())
			tabPanel.setSelectedIndex(tab);
	}

	public int getActiveTab()
	{
		return tabPanel.getSelectedIndex();
	}

	public boolean isSuccess()
	{
		return success;
	}

	public boolean isNumbererSettingsChanged()
	{
		return numbererSettingsChanged;
	}

	public PartAutoNumberer.Settings getNumbererSettings()
	{
		return numSettings;
	}

	public PartNameTemplate.Settings getNameTemplateSettings()
	{
		return nameTemplateSettings;
	}

	public SaveAndExportSettings getSaveAndExportSettings()
	{
		return saveSettings;
	}

	public static class MockMetadataSource implements AbcMetadataSource, AbcPartMetadataSource
	{
		private AbcMetadataSource originalSource;

		public MockMetadataSource(AbcMetadataSource originalSource)
		{
			this.originalSource = originalSource;
		}

		@Override public String getTitle()
		{
			return "First Flute";
		}

		@Override public LotroInstrument getInstrument()
		{
			return LotroInstrument.BASIC_FLUTE;
		}

		@Override public int getPartNumber()
		{
			return 4;
		}

		@Override public String getSongTitle()
		{
			if (originalSource != null && originalSource.getSongTitle().length() > 0)
				return originalSource.getSongTitle();

			return "Example Title";
		}

		@Override public String getComposer()
		{
			if (originalSource != null && originalSource.getComposer().length() > 0)
				return originalSource.getComposer();

			return "Example Composer";
		}

		@Override public String getTranscriber()
		{
			if (originalSource != null && originalSource.getTranscriber().length() > 0)
				return originalSource.getTranscriber();

			return "Your Name Here";
		}

		@Override public long getSongLengthMicros()
		{
			long length = 0;
			if (originalSource != null)
				length = originalSource.getSongLengthMicros();

			return (length != 0) ? length : 227000000/* 3:47 */;
		}

		@Override public File getExportFile()
		{
			if (originalSource != null)
			{
				File saveFile = originalSource.getExportFile();
				if (saveFile != null)
					return saveFile;
			}

			return new File(Util.getLotroMusicPath(false), "band/examplesong.abc");
		}

		@Override public String getPartName(AbcPartMetadataSource abcPart)
		{
			return null;
		}
	}
}

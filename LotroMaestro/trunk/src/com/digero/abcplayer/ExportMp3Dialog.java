package com.digero.abcplayer;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;

import com.digero.common.util.Util;

public class ExportMp3Dialog extends JDialog implements TableLayoutConstants
{
	private static final int TEXT_FIELD_COLS = 28;

	private enum Quality
	{
		Medium, Standard, Extreme
	}

	private File lameExe;

	private JTextField saveAsField;
	private JTextField titleField;
	private JCheckBox addLotroCheckbox;
	private JTextField artistField;
	private JTextField albumField;
	private ButtonGroup qualityButtonGroup;
	private JRadioButton[] qualityButtons;

	private Preferences prefs;
	private TableLayout layout;
	private JPanel content;

	private List<ActionListener> actionListeners;

	public ExportMp3Dialog(JFrame parent, File lameExe, Preferences prefs, File abcFile, String songTitle,
			String songArtist)
	{
		super(parent, AbcPlayer.APP_NAME + " - Export to MP3", false);

		this.prefs = prefs;
		this.lameExe = lameExe;
		this.actionListeners = new ArrayList<ActionListener>();

		Border outerBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8);

		layout = new TableLayout(//
				new double[] { PREFERRED, FILL, PREFERRED },//
				new double[] { });
		content = new JPanel(layout);

		String saveDir = prefs.get("saveDirectory", abcFile.getParentFile().getAbsolutePath());
		String saveName = abcFile.getName();
		if (saveName.toLowerCase().endsWith(".abc"))
		{
			saveName = saveName.substring(0, saveName.length() - 4) + ".mp3";
		}
		File saveFile = new File(saveDir, saveName);

		titleField = new JTextField(songTitle, TEXT_FIELD_COLS);
		addLotroCheckbox = new JCheckBox("Add \"(LOTRO)\"", prefs.getBoolean("addLotro", true));
		artistField = new JTextField(songArtist, TEXT_FIELD_COLS);
		albumField = new JTextField(prefs.get("album", ""), TEXT_FIELD_COLS);
		saveAsField = new JTextField(saveFile.getAbsolutePath(), TEXT_FIELD_COLS);

		JButton browseButton = new JButton("Browse...");
		browseButton.setMnemonic(KeyEvent.VK_B);
		browseButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				JFileChooser fc = new JFileChooser();
				fc.setSelectedFile(new File(saveAsField.getText()));
				int result = fc.showSaveDialog(ExportMp3Dialog.this);
				if (result == JFileChooser.APPROVE_OPTION)
				{
					File f = fc.getSelectedFile();
					if (f.getName().indexOf('.') < 0)
						f = new File(f.getParentFile(), f + ".mp3");
					saveAsField.setText(fc.getSelectedFile().getAbsolutePath());
				}
			}
		});

		JPanel qualityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		qualityButtons = new JRadioButton[Quality.values().length];
		qualityButtonGroup = new ButtonGroup();
		int iQuality = Util.clamp(prefs.getInt("quality", Quality.Standard.ordinal()), 0, qualityButtons.length - 1);
		for (Quality q : Quality.values())
		{
			int i = q.ordinal();
			qualityButtons[i] = new JRadioButton(q.toString(), i == iQuality);
			qualityButtonGroup.add(qualityButtons[i]);
			qualityPanel.add(qualityButtons[i]);
		}

		addRow("Title", titleField, addLotroCheckbox);
		addRow("Artist", artistField, null);
		addRow("Album", albumField, null);
		addRow("Quality", qualityPanel, qualityPanel);
		addRow("Save As", saveAsField, browseButton);
		for (int r = 0; r < layout.getNumRow(); r++)
		{
			layout.setRow(r, 1.0 / layout.getNumRow());
		}
		layout.insertRow(layout.getNumRow(), 16);

		JPanel okCancelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		JButton okButton = new JButton("Convert");
		okButton.setMnemonic(KeyEvent.VK_O);
		okButton.setFont(okButton.getFont().deriveFont(Font.BOLD));
		okButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (validateFile())
				{
					saveSettings();
					setVisible(false);
					fireActionPerformed();
				}
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic(KeyEvent.VK_C);
		cancelButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
			}
		});
		JPanel spacer = new JPanel();
		spacer.setPreferredSize(new Dimension(5, 5));
		okCancelPanel.add(okButton);
		okCancelPanel.add(spacer);
		okCancelPanel.add(cancelButton);

		JPanel outerContent = new JPanel(new BorderLayout());
		outerContent.setBorder(outerBorder);
		setContentPane(outerContent);
		outerContent.add(content, BorderLayout.CENTER);
		outerContent.add(okCancelPanel, BorderLayout.SOUTH);

		pack();
		setLocation(getOwner().getX() + (getOwner().getWidth() - getWidth()) / 2, getOwner().getY()
				+ (getOwner().getHeight() - getHeight()) / 2);
		setResizable(false);
	}

	private void addRow(String labelText, Component element, Component element2)
	{
		int row = layout.getNumRow();
		layout.insertRow(row, PREFERRED);

		JLabel label = new JLabel(labelText + "  ");
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		content.add(label, "0, " + row);

		if (element == element2)
		{
			content.add(element, "1, " + row + ", 2, " + row);
		}
		else
		{
			if (element != null)
				content.add(element, "1, " + row + ", L, C");
			if (element2 != null)
				content.add(element2, "2, " + row + ", L, C");
		}
	}

	public Preferences getPreferencesNode()
	{
		return prefs;
	}

	private void saveSettings()
	{
		prefs.putBoolean("addLotro", addLotroCheckbox.isSelected());
		prefs.put("album", albumField.getText());
		prefs.putInt("quality", getQualityIndex());
		prefs.put("saveDirectory", getSaveFile().getParentFile().getAbsolutePath());
	}

	private int getQualityIndex()
	{
		for (int i = 0; i < qualityButtons.length; i++)
		{
			if (qualityButtons[i].isSelected())
				return i;
		}
		return Quality.Medium.ordinal();
	}

	public String getSongTitle()
	{
		String title = titleField.getText().trim();
		if (title.length() > 0 && addLotroCheckbox.isSelected())
		{
			title += " (LOTRO)";
		}
		return title;
	}

	public String getArtist()
	{
		return artistField.getText().trim();
	}

	public String getAlbum()
	{
		return albumField.getText().trim();
	}

	public String getQuality()
	{
		return qualityButtons[getQualityIndex()].getText().toLowerCase();
	}

	public File getSaveFile()
	{
		return new File(saveAsField.getText());
	}

	public String getCommandLine(File wav)
	{
		String args = " --silent";
		args += " --preset " + getQuality();
		if (getSongTitle().length() > 0)
			args += " --tt " + Util.quote(getSongTitle());
		if (getArtist().length() > 0)
			args += " --ta " + Util.quote(getArtist());
		if (getAlbum().length() > 0)
			args += " --tl " + Util.quote(getAlbum());
		args += " " + Util.quote(wav.getAbsolutePath());
		args += " " + Util.quote(getSaveFile().getAbsolutePath());
		return Util.quote(lameExe.getAbsolutePath()) + args;
	}

	private boolean validateFile()
	{
		File f = new File(saveAsField.getText());

		if (f.isDirectory())
		{
			JOptionPane.showMessageDialog(this, "Specified path is a folder:\n" + f.getAbsolutePath(), "Invalid file",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if (f.exists())
		{
			int result = JOptionPane.showConfirmDialog(this, "File " + f.getName() + " already exists. Overwrite?",
					"Confirm overwrite", JOptionPane.YES_NO_CANCEL_OPTION);
			if (result == JOptionPane.CANCEL_OPTION)
			{
				setVisible(false);
				return false;
			}
			return result == JOptionPane.OK_OPTION;
		}
		else if (!f.getParentFile().exists())
		{
			int result = JOptionPane.showConfirmDialog(this, "Folder \"" + f.getParentFile().getName()
					+ "\" doesn't exist. Create?", "Create directory", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION)
			{
				if (!f.getParentFile().mkdirs())
				{
					JOptionPane.showMessageDialog(this, "Failed to create parent folder", "Failed to create folder",
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return true;
		}
	}

	public void addActionListener(ActionListener listener)
	{
		actionListeners.add(listener);
	}

	public void removeActionListener(ActionListener listener)
	{
		actionListeners.remove(listener);
	}

	private void fireActionPerformed()
	{
		ActionEvent e = new ActionEvent(this, 0, null);
		for (ActionListener listener : actionListeners)
		{
			listener.actionPerformed(e);
		}
	}
}

package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.icons.IconLoader;
import com.digero.common.midi.NoteFilterSequencerWrapper;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.IDiscardable;
import com.digero.common.view.ColorTable;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartEvent.AbcPartProperty;
import com.digero.maestro.abc.PartAutoNumberer;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.Listener;

@SuppressWarnings("serial")
public class PartPanel extends JPanel implements ICompileConstants, TableLayoutConstants
{
	private static final int HGAP = 4, VGAP = 4;

	private AbcPart abcPart;
	private PartAutoNumberer partAutoNumberer;
	private NoteFilterSequencerWrapper sequencer;
	private SequencerWrapper abcSequencer;
	private boolean isAbcPreviewMode = false;

	private JSpinner numberSpinner;
	private SpinnerNumberModel numberSpinnerModel;
	private JButton numberSettingsButton;
	private JTextField nameTextField;
	private JComboBox<LotroInstrument> instrumentComboBox;
	private JLabel messageLabel;

	private JScrollPane trackScrollPane;

	private JPanel trackListPanel;
	private GroupLayout trackListLayout;
	private GroupLayout.Group trackListVGroup;
	private GroupLayout.Group trackListHGroup;

	private LotroInstrument lastSelectedInstrument = null;

	private boolean initialized = false;

	public PartPanel(NoteFilterSequencerWrapper sequencer, PartAutoNumberer partAutoNumberer,
			SequencerWrapper abcSequencer)
	{
		super(new TableLayout(//
				new double[] { FILL },//
				new double[] { PREFERRED, FILL }));

		TableLayout layout = (TableLayout) getLayout();
		layout.setHGap(HGAP);
		layout.setVGap(VGAP);

		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorTable.PANEL_BORDER.get()));

		this.sequencer = sequencer;
		this.abcSequencer = abcSequencer;
		this.partAutoNumberer = partAutoNumberer;

		numberSpinnerModel = new SpinnerNumberModel(0, 0, 999, partAutoNumberer.getIncrement());
		numberSpinner = new JSpinner(numberSpinnerModel);
		numberSpinner.addChangeListener(new ChangeListener()
		{
			@Override public void stateChanged(ChangeEvent e)
			{
				if (abcPart != null)
					PartPanel.this.partAutoNumberer.setPartNumber(abcPart, (Integer) numberSpinner.getValue());
			}
		});

		numberSettingsButton = new JButton(IconLoader.getImageIcon("gear_16.png"));
		numberSettingsButton.setMargin(new Insets(0, 0, 0, 0));
		numberSettingsButton.setToolTipText("Automatic part numbering options");
		numberSettingsButton.setVisible(false);

		nameTextField = new JTextField(32);
		nameTextField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void removeUpdate(DocumentEvent e)
			{
				if (abcPart != null)
					abcPart.setTitle(nameTextField.getText());
			}

			@Override public void insertUpdate(DocumentEvent e)
			{
				if (abcPart != null)
					abcPart.setTitle(nameTextField.getText());
			}

			@Override public void changedUpdate(DocumentEvent e)
			{
				if (abcPart != null)
					abcPart.setTitle(nameTextField.getText());
			}
		});

		instrumentComboBox = new JComboBox<LotroInstrument>();
		instrumentComboBox.setMaximumRowCount(12);
		instrumentComboBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (abcPart != null)
				{
					LotroInstrument newInstrument = (LotroInstrument) instrumentComboBox.getSelectedItem();
					PartPanel.this.partAutoNumberer.setInstrument(abcPart, newInstrument);
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
		dataPanel2.add(numberSettingsButton);
		dataPanel2.add(new JLabel(" I:"));
		dataPanel2.add(instrumentComboBox);
		dataPanel2.add(new JLabel(" Part name:"));
		dataPanel.add(dataPanel2, BorderLayout.WEST);
		dataPanel.add(nameTextField, BorderLayout.CENTER);

		trackListPanel = new JPanel();
		trackListLayout = new GroupLayout(trackListPanel);
		trackListLayout.setVerticalGroup(trackListVGroup = trackListLayout.createSequentialGroup());
		trackListLayout.setHorizontalGroup(trackListHGroup = trackListLayout.createParallelGroup());
		trackListLayout.setHonorsVisibility(true);
		trackListPanel.setLayout(trackListLayout);
		trackListPanel.setBackground(ColorTable.PANEL_BACKGROUND_DISABLED.get());

		trackScrollPane = new JScrollPane(trackListPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		messageLabel = new JLabel();
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		messageLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 20));
		messageLabel.setForeground(ColorTable.PANEL_TEXT_DISABLED.get());
		messageLabel.setVisible(false);

		add(dataPanel, "0, 0");
		add(messageLabel, "0, 1, C, C");
		add(trackScrollPane, "0, 1");

		setAbcPart(null);
		initialized = true;
	}

	public void addSettingsActionListener(ActionListener listener)
	{
		numberSettingsButton.addActionListener(listener);
		numberSettingsButton.setVisible(true);
	}

	private Listener<AbcPartEvent> abcPartListener = new Listener<AbcPartEvent>()
	{
		@Override public void onEvent(AbcPartEvent e)
		{
			if (e.getProperty() == AbcPartProperty.PART_NUMBER)
			{
				numberSpinner.setValue(abcPart.getPartNumber());
			}
		}
	};

	public void settingsChanged()
	{
		numberSpinnerModel.setStepSize(partAutoNumberer.getIncrement());
	}

	public void setAbcPart(AbcPart abcPart)
	{
		messageLabel.setVisible(false);

		if (this.abcPart == abcPart && initialized)
			return;

		if (this.abcPart != null)
		{
			try
			{
				numberSpinner.commitEdit();
			}
			catch (ParseException e)
			{
			}
			this.abcPart.removeAbcListener(abcPartListener);
			this.abcPart = null;
		}

		if (abcPart == null)
		{
			numberSpinner.setEnabled(false);
			nameTextField.setEnabled(false);
			instrumentComboBox.setEnabled(false);
			instrumentComboBox.setModel(new DefaultComboBoxModel<LotroInstrument>(LotroInstrument.values()));

			numberSpinner.setValue(0);
			nameTextField.setText("");
			instrumentComboBox.setSelectedIndex(0);
			lastSelectedInstrument = null;

			clearTrackListPanel();
		}
		else
		{
			numberSpinner.setEnabled(true);
			nameTextField.setEnabled(true);
			instrumentComboBox.setEnabled(true);

			numberSpinner.setValue(abcPart.getPartNumber());
			nameTextField.setText(abcPart.getTitle());
			instrumentComboBox.setModel(new DefaultComboBoxModel<LotroInstrument>(abcPart.getSupportedInstruments()));
			instrumentComboBox.setSelectedItem(abcPart.getInstrument());
			lastSelectedInstrument = abcPart.getInstrument();

			clearTrackListPanel();

			for (TrackInfo track : abcPart.getSequenceInfo().getTrackList())
			{
				int trackNumber = track.getTrackNumber();
				if (track.hasEvents())
				{
					TrackPanel trackPanel = new TrackPanel(track, sequencer, abcPart, abcSequencer);
					trackPanel.setAbcPreviewMode(isAbcPreviewMode);
					trackScrollPane.getVerticalScrollBar().setUnitIncrement(trackPanel.getPreferredSize().height);
					trackListVGroup.addComponent(trackPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
							GroupLayout.PREFERRED_SIZE);
					trackListHGroup.addComponent(trackPanel);

					if (MUTE_DISABLED_TRACKS)
						sequencer.setTrackMute(trackNumber, !abcPart.isTrackEnabled(trackNumber));
				}

				if (!MUTE_DISABLED_TRACKS)
					sequencer.setTrackMute(trackNumber, false);

				sequencer.setTrackSolo(trackNumber, false);
			}
		}

		this.abcPart = abcPart;
		if (this.abcPart != null)
		{
			this.abcPart.addAbcListener(abcPartListener);
		}

		updateTracksVisible();
		validate();
		repaint();
	}

	public AbcPart getAbcPart()
	{
		return abcPart;
	}

	public void setAbcPreviewMode(boolean isAbcPreviewMode)
	{
		if (this.isAbcPreviewMode != isAbcPreviewMode)
		{
			this.isAbcPreviewMode = isAbcPreviewMode;
			for (Component child : trackListPanel.getComponents())
			{
				if (child instanceof TrackPanel)
				{
					((TrackPanel) child).setAbcPreviewMode(isAbcPreviewMode);
				}
				else if (child instanceof DrumPanel)
				{
					((DrumPanel) child).setAbcPreviewMode(isAbcPreviewMode);
				}
			}
		}
	}

	public boolean isAbcPreviewMode()
	{
		return isAbcPreviewMode;
	}

	public void showInfoMessage(String message)
	{
		setAbcPart(null);

		messageLabel.setText(message);
		messageLabel.setVisible(true);
	}

	private void updateTracksVisible()
	{
		if (abcPart == null)
			return;

		boolean percussion = abcPart.getInstrument().isPercussion;
		boolean setHeight = false;

		for (Component child : trackListPanel.getComponents())
		{
			if (child instanceof TrackPanel)
			{
				TrackPanel trackPanel = (TrackPanel) child;
				child.setEnabled(percussion || trackPanel.getTrackInfo().hasEvents());
				if (!setHeight && !percussion)
				{
					trackScrollPane.getVerticalScrollBar().setUnitIncrement(child.getPreferredSize().height);
					setHeight = true;
				}
			}
			else if (child instanceof DrumPanel)
			{
				child.setVisible(percussion);
				if (!setHeight && percussion)
				{
					trackScrollPane.getVerticalScrollBar().setUnitIncrement(child.getPreferredSize().height);
					setHeight = true;
				}
			}
		}
	}

	private void clearTrackListPanel()
	{
		for (Component child : trackListPanel.getComponents())
		{
			if (child instanceof IDiscardable)
			{
				((IDiscardable) child).discard();
			}
		}
		trackListPanel.removeAll();
		trackListLayout.setVerticalGroup(trackListVGroup = trackListLayout.createSequentialGroup());
		trackListLayout.setHorizontalGroup(trackListHGroup = trackListLayout.createParallelGroup());
	}

	public void setSequencer(NoteFilterSequencerWrapper sequencer)
	{
		AbcPart abcPartTmp = this.abcPart;
		setAbcPart(null);
		this.sequencer = sequencer;
		setAbcPart(abcPartTmp);
	}

	public void commitAllFields()
	{
		try
		{
			numberSpinner.commitEdit();
		}
		catch (java.text.ParseException e)
		{
			// Ignore
		}
	}
}

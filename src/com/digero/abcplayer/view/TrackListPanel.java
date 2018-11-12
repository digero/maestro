package com.digero.abcplayer.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.abctomidi.AbcInfo;
import com.digero.common.abctomidi.AbcRegion;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerEvent.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.Listener;
import com.digero.common.view.InstrumentComboBox;

public class TrackListPanel extends JPanel implements Listener<SequencerEvent>, TableLayoutConstants
{
	public static final int TRACKLIST_ROWHEIGHT = 18;
	private static final Object TRACK_INDEX_KEY = new Object();

	private final SequencerWrapper sequencer;
	private final TrackListPanelCallback abcPlayer;
	private AbcInfo abcInfo;

	private TableLayout layout;
	private TrackControls[] trackControls = null;

	private static class TrackControls
	{
		public TrackControls(JCheckBox checkBox, JLabel lineNumberLabel, JToggleButton soloButton,
				JComboBox<LotroInstrument> instrumentComboBox)
		{
			this.checkBox = checkBox;
			this.lineNumberLabel = lineNumberLabel;
			this.soloButton = soloButton;
			this.instrumentComboBox = instrumentComboBox;
		}

		public final JCheckBox checkBox;
		public final JLabel lineNumberLabel;
		public final JToggleButton soloButton;
		public final JComboBox<LotroInstrument> instrumentComboBox;
		public NavigableMap<Long, Integer> tickToLineNumber;
	}

	private boolean showFullPartName = false;
	private boolean showLineNumbers = true;
	private boolean showSoloButtons = true;
	private boolean showInstrumentComboBoxes = true;
	private boolean updatePending = false;

	private static final int HGAP = 4;
	private static final int CHECKBOX_COLUMN = 1;
	private static final int LINE_NUMBER_COLUMN = 3;
	private static final int SOLO_BUTTON_COLUMN = 5;
	private static final int INSTRUMENT_COLUMN = 7;

	public TrackListPanel(SequencerWrapper sequencer, TrackListPanelCallback abcPlayer)
	{
		super(new TableLayout(new double[] { HGAP, FILL, HGAP, PREFERRED, HGAP, PREFERRED, HGAP, PREFERRED, HGAP },
				new double[] { 0, 0 }));

		this.sequencer = sequencer;
		this.abcPlayer = abcPlayer;

		sequencer.addChangeListener(this);

		layout = (TableLayout) getLayout();
		layout.setVGap(4);
		layout.setHGap(0);

		setBackground(Color.WHITE);
	}

	@SuppressWarnings("rawtypes")//
	public void clear()
	{
		for (Component c : getComponents())
		{
			if (c instanceof JCheckBox)
			{
				((JCheckBox) c).removeActionListener(trackMuteListener);
				((JCheckBox) c).removeActionListener(trackSoloListener);
			}
			if (c instanceof JComboBox)
			{
				((JComboBox) c).removeActionListener(instrumentChangeListener);
			}
		}
		trackControls = null;
		removeAll();
		for (int i = layout.getNumRow() - 2; i >= 1; i--)
		{
			layout.deleteRow(i);
		}
		revalidate();
		repaint();
	}

	public void songChanged(AbcInfo abcInfo)
	{
		clear();
		if (sequencer.getSequence() == null)
		{
			this.abcInfo = null;
			return;
		}

		this.abcInfo = abcInfo;

		Track[] tracks = sequencer.getSequence().getTracks();
		trackControls = new TrackControls[tracks.length];

		for (int i = 0; i < tracks.length; i++)
		{
			Track track = tracks[i];

			// Only show tracks with at least one note
			boolean hasNotes = false;
			LotroInstrument instrument = LotroInstrument.DEFAULT_INSTRUMENT;
			for (int j = 0; j < track.size(); j++)
			{
				MidiEvent evt = track.get(j);
				if (evt.getMessage() instanceof ShortMessage)
				{
					ShortMessage m = (ShortMessage) evt.getMessage();
					if (m.getCommand() == ShortMessage.NOTE_ON)
					{
						hasNotes = true;
					}
					else if (m.getCommand() == ShortMessage.PROGRAM_CHANGE)
					{
						for (LotroInstrument inst : LotroInstrument.values())
						{
							if (m.getData1() == inst.midi.id())
							{
								instrument = inst;
								break;
							}
						}
					}
				}
			}

			if (hasNotes)
			{
				JCheckBox checkBox = new JCheckBox(getCheckBoxText(i));
				checkBox.setToolTipText(abcInfo.getPartNumber(i) + ". " + abcInfo.getPartFullName(i));
				checkBox.putClientProperty(TRACK_INDEX_KEY, i);
				checkBox.setBackground(getBackground());
				checkBox.setSelected(!sequencer.getTrackMute(i));
				checkBox.addActionListener(trackMuteListener);

				JLabel lineNumberLabel = new JLabel();
				lineNumberLabel.setVisible(showLineNumbers);
				lineNumberLabel.putClientProperty(TRACK_INDEX_KEY, i);
				lineNumberLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				lineNumberLabel.addMouseListener(viewAbcForTrackListener);
				lineNumberLabel.setToolTipText("<html>Line number<br>Click to view ABC text at this line</html>");
				lineNumberLabel.setBackground(getBackground());

				JToggleButton soloButton = new JToggleButton("S");
				soloButton.setVisible(showSoloButtons);
				soloButton.setMargin(new Insets(3, 4, 3, 3));
				soloButton.setToolTipText("Play only this part (Solo)");
				soloButton.putClientProperty(TRACK_INDEX_KEY, i);
				soloButton.setBackground(getBackground());
				soloButton.setSelected(sequencer.getTrackSolo(i));
				soloButton.addActionListener(trackSoloListener);

				JComboBox<LotroInstrument> comboBox = new InstrumentComboBox();
				comboBox.setVisible(showInstrumentComboBoxes);
				comboBox.putClientProperty(TRACK_INDEX_KEY, i);
				comboBox.setBackground(getBackground());
				comboBox.setSelectedItem(instrument);
				comboBox.addActionListener(instrumentChangeListener);

				trackControls[i] = new TrackControls(checkBox, lineNumberLabel, soloButton, comboBox);

				int r = layout.getNumRow() - 1;
				layout.insertRow(r, TRACKLIST_ROWHEIGHT);
				add(checkBox, CHECKBOX_COLUMN + ", " + r);
				add(lineNumberLabel, LINE_NUMBER_COLUMN + ", " + r + ", r, c");
				add(soloButton, SOLO_BUTTON_COLUMN + ", " + r);
				add(comboBox, INSTRUMENT_COLUMN + ", " + r);
			}
		}

		revalidate();
		repaint();
	}

	public void setShowFullPartName(boolean showFullPartName)
	{
		if (this.showFullPartName != showFullPartName)
		{
			this.showFullPartName = showFullPartName;

			if (trackControls != null)
			{
				for (int i = 0; i < trackControls.length; i++)
				{
					if (trackControls[i] != null)
						trackControls[i].checkBox.setText(getCheckBoxText(i));
				}
			}
		}
	}

	public void setShowLineNumbers(boolean showLineNumbers)
	{
		if (this.showLineNumbers != showLineNumbers)
		{
			this.showLineNumbers = showLineNumbers;

			layout.setColumn(LINE_NUMBER_COLUMN, showLineNumbers ? PREFERRED : 0);
			layout.setColumn(LINE_NUMBER_COLUMN + 1, showLineNumbers ? HGAP : 0);

			if (trackControls != null)
			{
				for (int i = 0; i < trackControls.length; i++)
				{
					if (trackControls[i] != null)
						trackControls[i].lineNumberLabel.setVisible(showLineNumbers);
				}
			}
		}
	}

	public void setShowSoloButtons(boolean showSoloButtons)
	{
		if (this.showSoloButtons != showSoloButtons)
		{
			this.showSoloButtons = showSoloButtons;

			layout.setColumn(SOLO_BUTTON_COLUMN, showSoloButtons ? PREFERRED : 0);
			layout.setColumn(SOLO_BUTTON_COLUMN + 1, showSoloButtons ? HGAP : 0);

			if (trackControls != null)
			{
				for (int i = 0; i < trackControls.length; i++)
				{
					if (trackControls[i] != null)
						trackControls[i].soloButton.setVisible(showSoloButtons);
				}
			}
		}
	}

	public void setShowInstrumentComboBoxes(boolean showInstrumentComboBoxes)
	{
		if (this.showInstrumentComboBoxes != showInstrumentComboBoxes)
		{
			this.showInstrumentComboBoxes = showInstrumentComboBoxes;

			layout.setColumn(INSTRUMENT_COLUMN, showInstrumentComboBoxes ? PREFERRED : 0);
			layout.setColumn(INSTRUMENT_COLUMN + 1, showInstrumentComboBoxes ? HGAP : 0);

			if (trackControls != null)
			{
				for (int i = 0; i < trackControls.length; i++)
				{
					if (trackControls[i] != null)
						trackControls[i].instrumentComboBox.setVisible(showInstrumentComboBoxes);
				}
			}
		}
	}

	private String getCheckBoxText(int i)
	{
		return abcInfo.getPartNumber(i) + ". "
				+ (showFullPartName ? abcInfo.getPartFullName(i) : abcInfo.getPartName(i));
	}

	private ActionListener trackMuteListener = new ActionListener()
	{
		@Override public void actionPerformed(ActionEvent e)
		{
			JCheckBox checkBox = (JCheckBox) e.getSource();
			int i = (Integer) checkBox.getClientProperty(TRACK_INDEX_KEY);
			sequencer.setTrackMute(i, !checkBox.isSelected());
		}
	};

	private ActionListener trackSoloListener = new ActionListener()
	{
		@Override public void actionPerformed(ActionEvent e)
		{
			JToggleButton checkBox = (JToggleButton) e.getSource();
			int i = (Integer) checkBox.getClientProperty(TRACK_INDEX_KEY);
			sequencer.setTrackSolo(i, checkBox.isSelected());
		}
	};

	private MouseListener viewAbcForTrackListener = new MouseAdapter()
	{
		@Override public void mouseClicked(MouseEvent e)
		{
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				JComponent control = (JComponent) e.getSource();
				int i = (Integer) control.getClientProperty(TRACK_INDEX_KEY);
				abcPlayer.showHighlightPanelForTrack(i);
			}
		}
	};

	private ActionListener instrumentChangeListener = new ActionListener()
	{
		@Override public void actionPerformed(ActionEvent e)
		{
			JComboBox<?> comboBox = (JComboBox<?>) e.getSource();
			int i = (Integer) comboBox.getClientProperty(TRACK_INDEX_KEY);
			abcPlayer.setTrackInstrumentOverride(i, (LotroInstrument) comboBox.getSelectedItem());
		}
	};

	@Override public void onEvent(SequencerEvent evt)
	{
		SequencerProperty p = evt.getProperty();
		if (p.isInMask(SequencerProperty.THUMB_POSITION_MASK | SequencerProperty.LENGTH.mask
				| SequencerProperty.TEMPO.mask))
		{
			update();
		}
	}

	private void update()
	{
		if (updatePending || !isVisible())
			return;

		updatePending = true;
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override public void run()
			{
				updatePending = false;
				updateCore();
			}
		});
	}

	private void updateCore()
	{
		if (trackControls == null)
			return;

		for (int i = 0; i < trackControls.length; i++)
		{
			TrackControls info = trackControls[i];
			if (info == null)
				continue;

			if (info.tickToLineNumber == null)
			{
				info.tickToLineNumber = new TreeMap<Long, Integer>();
				int prevLine = -1;
				for (AbcRegion region : abcInfo.getRegions())
				{
					if (region.getTrackNumber() == i)
					{
						int line = region.getLine();
						if (line != prevLine)
						{
							info.tickToLineNumber.put(region.getStartTick(), region.getLine());
							prevLine = line;
						}
					}
				}
			}

			Entry<Long, Integer> entry = info.tickToLineNumber.floorEntry(sequencer.getThumbTick());
			int lineNumber = (entry != null) ? (entry.getValue() + 1) : 0;
			info.lineNumberLabel.setText("<html><a href='.'>" + lineNumber + "</a></html>");
		}
	}
}
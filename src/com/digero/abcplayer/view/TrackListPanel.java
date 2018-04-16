package com.digero.abcplayer.view;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.digero.abcplayer.AbcPlayer;
import com.digero.common.abc.LotroInstrument;
import com.digero.common.abctomidi.AbcInfo;
import com.digero.common.abctomidi.AbcToMidi;
import com.digero.common.midi.SequencerWrapper;

public class TrackListPanel extends JPanel
{
	public static final int TRACKLIST_ROWHEIGHT = 18;
	private static final Object TRACK_INDEX_KEY = new Object();

	private final SequencerWrapper sequencer;
	private final Map<Integer, LotroInstrument> instrumentOverrideMap;
	private AbcInfo abcInfo;

	private TableLayout layout;
	private LotroInstrument[] sortedInstruments = LotroInstrument.values();
	private JCheckBox[] trackCheckBoxes = null;

	private boolean showFullPartName = false;

	public TrackListPanel(SequencerWrapper sequencer, Map<Integer, LotroInstrument> instrumentOverrideMap)
	{
		super(new TableLayout(new double[] { 0, AbcPlayer.FILL, AbcPlayer.PREFERRED, AbcPlayer.PREFERRED, 0 },
				new double[] { 0, 0 }));

		this.sequencer = sequencer;
		this.instrumentOverrideMap = instrumentOverrideMap;

		Arrays.sort(sortedInstruments, new Comparator<LotroInstrument>()
		{
			@Override public int compare(LotroInstrument a, LotroInstrument b)
			{
				return a.toString().compareTo(b.toString());
			}
		});

		layout = (TableLayout) getLayout();
		layout.setVGap(4);
		layout.setHGap(4);

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
		trackCheckBoxes = null;
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
		trackCheckBoxes = new JCheckBox[tracks.length];

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
							if (m.getData1() == inst.midiProgramId)
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
				trackCheckBoxes[i] = checkBox;
				checkBox.setToolTipText(abcInfo.getPartNumber(i) + ". " + abcInfo.getPartFullName(i));
				checkBox.putClientProperty(TRACK_INDEX_KEY, i);
				checkBox.setBackground(getBackground());
				checkBox.setSelected(!sequencer.getTrackMute(i));
				checkBox.addActionListener(trackMuteListener);

				JToggleButton soloButton = new JToggleButton("S");
				soloButton.setMargin(new Insets(3, 4, 3, 3));
				soloButton.setToolTipText("Play only this part (Solo)");
				soloButton.putClientProperty(TRACK_INDEX_KEY, i);
				soloButton.setBackground(getBackground());
				soloButton.setSelected(sequencer.getTrackSolo(i));
				soloButton.addActionListener(trackSoloListener);

				JComboBox<LotroInstrument> comboBox = new JComboBox<LotroInstrument>(sortedInstruments);
				comboBox.setMaximumRowCount(sortedInstruments.length);
				comboBox.putClientProperty(TRACK_INDEX_KEY, i);
				comboBox.setBackground(getBackground());
				comboBox.setSelectedItem(instrument);
				comboBox.addActionListener(instrumentChangeListener);

				int r = layout.getNumRow() - 1;
				layout.insertRow(r, TRACKLIST_ROWHEIGHT);
				add(checkBox, "1, " + r);
				add(soloButton, "2, " + r);
				add(comboBox, "3, " + r);
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

			if (trackCheckBoxes != null)
			{
				for (int i = 0; i < trackCheckBoxes.length; i++)
				{
					if (trackCheckBoxes[i] != null)
						trackCheckBoxes[i].setText(getCheckBoxText(i));
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

	private ActionListener instrumentChangeListener = new ActionListener()
	{
		@Override public void actionPerformed(ActionEvent e)
		{
			JComboBox<?> comboBox = (JComboBox<?>) e.getSource();
			int i = (Integer) comboBox.getClientProperty(TRACK_INDEX_KEY);
			LotroInstrument instrument = (LotroInstrument) comboBox.getSelectedItem();
			instrumentOverrideMap.put(i, instrument);
			AbcToMidi.updateInstrumentRealtime(sequencer, i, instrument);
		}
	};
}
package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.common.midi.Note;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.ExtensionFileFilter;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.ParseException;
import com.digero.common.util.Util;
import com.digero.common.view.ColorTable;
import com.digero.common.view.LinkButton;
import com.digero.common.view.NoteGraph;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.abc.DrumNoteMap;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.NoteFilterSequencerWrapper;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.IDisposable;

@SuppressWarnings("serial")
public class TrackPanel extends JPanel implements IDisposable, TableLayoutConstants, ICompileConstants {
	private static final String DRUM_NOTE_MAP_DIR_PREF_KEY = "DrumNoteMap.directory";

	//              0              1               2
	//   +--------------------+----------+--------------------+
	//   |      TRACK NAME    | octave   |  +--------------+  |
	// 0 | [X]                | +----^-+ |  | (note graph) |  |
	//   |      Instrument(s) | +----v-+ |  +--------------+  |
	//   +--------------------+----------+                    |
	// 1 | Drum save controls (optional) |                    |
	//   +-------------------------------+--------------------+
	static final int TITLE_COLUMN = 0;
	static final int CONTROL_COLUMN = 1;
	static final int NOTE_COLUMN = 2;

	static final int TITLE_WIDTH = 164;
	static final int CONTROL_WIDTH = 48;
	private static final double[] LAYOUT_COLS = new double[] {
			TITLE_WIDTH, CONTROL_WIDTH, FILL
	};
	private static final double[] LAYOUT_ROWS = new double[] {
			48, PREFERRED
	};

	private final TrackInfo trackInfo;
	private final NoteFilterSequencerWrapper seq;
	private final AbcPart abcPart;

	private JCheckBox checkBox;
	private JSpinner transposeSpinner;
	private JPanel drumSavePanel;
	private TrackNoteGraph noteGraph;

	private AbcPartListener abcListener;
	private SequencerListener seqListener;

	private boolean showDrumPanels;
	private boolean wasDrumPart;

	public TrackPanel(TrackInfo info, NoteFilterSequencerWrapper sequencer, AbcPart part) {
		super(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));

		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorTable.PANEL_BORDER.get()));

		this.trackInfo = info;
		this.seq = sequencer;
		this.abcPart = part;

		TableLayout tableLayout = (TableLayout) getLayout();
		tableLayout.setHGap(4);

		checkBox = new JCheckBox();
		checkBox.setOpaque(false);
		checkBox.setSelected(abcPart.isTrackEnabled(trackInfo.getTrackNumber()));

		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int track = trackInfo.getTrackNumber();
				boolean enabled = checkBox.isSelected();
				abcPart.setTrackEnabled(track, enabled);
				if (MUTE_DISABLED_TRACKS)
					seq.setTrackMute(track, !enabled);
			}
		});

		noteGraph = new TrackNoteGraph(seq, trackInfo);
		noteGraph.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3)
					seq.setTrackSolo(trackInfo.getTrackNumber(), true);
			}

			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3)
					seq.setTrackSolo(trackInfo.getTrackNumber(), false);
			}
		});

		if (!trackInfo.isDrumTrack()) {
			int currentTranspose = abcPart.getTrackTranspose(trackInfo.getTrackNumber());
			transposeSpinner = new JSpinner(new TrackTransposeModel(currentTranspose, -48, 48, 12));
			transposeSpinner.setToolTipText("Transpose this track by octaves (12 semitones)");

			transposeSpinner.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int track = trackInfo.getTrackNumber();
					int value = (Integer) transposeSpinner.getValue();
					if (value % 12 != 0) {
						value = (abcPart.getTrackTranspose(track) / 12) * 12;
						transposeSpinner.setValue(value);
					}
					else {
						abcPart.setTrackTranspose(trackInfo.getTrackNumber(), value);
					}
				}
			});
		}
		
		add(checkBox, TITLE_COLUMN + ", 0");
		if (transposeSpinner != null)
			add(transposeSpinner, CONTROL_COLUMN + ", 0, f, c");
		add(noteGraph, NOTE_COLUMN + ", 0, " + NOTE_COLUMN + ", 1");

		updateTitleText();

		abcPart.addAbcListener(abcListener = new AbcPartListener() {
			public void abcPartChanged(AbcPartEvent e) {
				if (e.isPreviewRelated()) {
					updateState();
					noteGraph.repaint();
				}
			}
		});

		seq.addChangeListener(seqListener = new SequencerListener() {
			public void propertyChanged(SequencerEvent evt) {
				if (evt.getProperty() == SequencerProperty.TRACK_ACTIVE)
					updateColors();
			}
		});

		addPropertyChangeListener("enabled", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				updateState();
			}
		});

		updateState(true);
	}
	
	private void initDrumSavePanel() {
		JLabel intro = new JLabel("Drum Map: ");
		intro.setForeground(ColorTable.PANEL_TEXT_DISABLED.get());
		
		LinkButton saveButton = new LinkButton("<html><u>Save</u></html>");
		saveButton.setForeground(ColorTable.PANEL_LINK.get());
		saveButton.setOpaque(false);
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveDrumMapping();
			}
		});
		
		JLabel divider = new JLabel(" | ");
		divider.setForeground(ColorTable.PANEL_TEXT_DISABLED.get());
		
		LinkButton loadButton = new LinkButton("<html><u>Load</u></html>");
		loadButton.setForeground(ColorTable.PANEL_LINK.get());
		loadButton.setOpaque(false);
		loadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadDrumMapping();
			}
		});
		
		drumSavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		drumSavePanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 0));
		drumSavePanel.setOpaque(false);
		drumSavePanel.add(intro);
		drumSavePanel.add(loadButton);
		drumSavePanel.add(divider);
		drumSavePanel.add(saveButton);
	}

	public TrackInfo getTrackInfo() {
		return trackInfo;
	}

	private void updateState() {
		updateState(false);
	}

	private void updateTitleText() {
		final int ELLIPSIS_OFFSET = 28;

		String title = trackInfo.getTrackNumber() + ". " + trackInfo.getName();
		String instr = trackInfo.getInstrumentNames();
		checkBox.setToolTipText("<html><b>" + title + "</b><br>" + instr + "</html>");

		title = Util.ellipsis(title, TITLE_WIDTH - ELLIPSIS_OFFSET, checkBox.getFont().deriveFont(Font.BOLD));
		instr = Util.ellipsis(instr, TITLE_WIDTH - ELLIPSIS_OFFSET, checkBox.getFont());
		checkBox.setText("<html><b>" + title + "</b><br>" + instr + "</html>");
	}

	private void updateColors() {
		int trackNumber = trackInfo.getTrackNumber();
		boolean trackEnabled = abcPart.isTrackEnabled(trackNumber);

		if (!seq.isTrackActive(trackNumber)) {
			noteGraph.setNoteColor(ColorTable.NOTE_OFF);
			noteGraph.setBadNoteColor(ColorTable.NOTE_BAD_OFF);
			setBackground(ColorTable.GRAPH_BACKGROUND_OFF.get());
		}
		else if (trackEnabled || seq.getTrackSolo(trackNumber)) {
			noteGraph.setNoteColor(ColorTable.NOTE_ENABLED);
			noteGraph.setBadNoteColor(ColorTable.NOTE_BAD_ENABLED);
			setBackground(ColorTable.GRAPH_BACKGROUND_ENABLED.get());
		}
		else {
			boolean pseudoOff = (abcPart.isDrumPart() != trackInfo.isDrumTrack());
			noteGraph.setNoteColor(pseudoOff ? ColorTable.NOTE_OFF : ColorTable.NOTE_DISABLED);
			noteGraph.setBadNoteColor(pseudoOff ? ColorTable.NOTE_BAD_OFF : ColorTable.NOTE_BAD_DISABLED);
			setBackground(ColorTable.GRAPH_BACKGROUND_DISABLED.get());
		}

		if (trackEnabled) {
			checkBox.setForeground(ColorTable.PANEL_TEXT_ENABLED.get());
		}
		else {
			boolean inputEnabled = abcPart.isDrumPart() == trackInfo.isDrumTrack();
			checkBox.setForeground(inputEnabled ? ColorTable.PANEL_TEXT_DISABLED.get() : ColorTable.PANEL_TEXT_OFF
					.get());
		}
	}

	private void updateState(boolean initDrumPanels) {
		updateColors();

		boolean trackEnabled = abcPart.isTrackEnabled(trackInfo.getTrackNumber());
		checkBox.setSelected(trackEnabled);

		boolean showDrumPanelsNew = abcPart.isDrumPart() && trackEnabled;
		if (initDrumPanels || showDrumPanels != showDrumPanelsNew || wasDrumPart != abcPart.isDrumPart()) {
			if (showDrumPanels != showDrumPanelsNew) {
				noteGraph.repaint();
				showDrumPanels = showDrumPanelsNew;
			}
			wasDrumPart = abcPart.isDrumPart();

			for (int i = getComponentCount() - 1; i >= 0; --i) {
				Component child = getComponent(i);
				if (child instanceof DrumPanel) {
					((DrumPanel) child).dispose();
					remove(i);
				}
			}
			if (drumSavePanel != null)
				remove(drumSavePanel);

			if (transposeSpinner != null)
				transposeSpinner.setVisible(!abcPart.isDrumPart());

			TableLayout layout = (TableLayout) getLayout();
			if (showDrumPanels) {
				if (drumSavePanel == null)
					initDrumSavePanel();
				
				add(drumSavePanel, TITLE_COLUMN + ", 1, l, c");
				int row = LAYOUT_ROWS.length;
				for (int noteId : trackInfo.getNotesInUse()) {
					DrumPanel panel = new DrumPanel(trackInfo, seq, abcPart, noteId);
					if (row <= layout.getNumRow())
						layout.insertRow(row, PREFERRED);
					add(panel, "0, " + row + ", 2, " + row);
				}
			}

			updateTitleText();

			revalidate();
		}
	}

	private boolean saveDrumMapping() {
		Preferences prefs = Preferences.userNodeForPackage(TrackPanel.class);

		String dirPath = prefs.get(DRUM_NOTE_MAP_DIR_PREF_KEY, null);
		File dir;
		if (dirPath == null || !(dir = new File(dirPath)).isDirectory())
			dir = Util.getLotroMusicPath(false /* create */);

		JFileChooser fileChooser = new JFileChooser(dir);
		fileChooser.setFileFilter(new ExtensionFileFilter("Drum Map", DrumNoteMap.FILE_SUFFIX));

		File saveFile;
		do {
			if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
				return false;

			saveFile = fileChooser.getSelectedFile();

			if (!Util.stringEndsWithIgnoreCase(saveFile.getName(), "." + DrumNoteMap.FILE_SUFFIX)) {
				saveFile = new File(saveFile.getParentFile(), saveFile.getName() + "." + DrumNoteMap.FILE_SUFFIX);
			}

			if (saveFile.exists()) {
				int result = JOptionPane.showConfirmDialog(this, "File " + saveFile.getName()
						+ " already exists. Overwrite?", "Confirm overwrite", JOptionPane.OK_CANCEL_OPTION);
				if (result != JOptionPane.OK_OPTION)
					continue;
			}
		} while (false);

		try {
			abcPart.getDrumMap(trackInfo.getTrackNumber()).save(saveFile);
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Failed to save drum map:\n\n" + e.getMessage(),
					"Failed to save drum map", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		prefs.put(DRUM_NOTE_MAP_DIR_PREF_KEY, fileChooser.getCurrentDirectory().getAbsolutePath());
		return true;
	}

	private boolean loadDrumMapping() {
		Preferences prefs = Preferences.userNodeForPackage(TrackPanel.class);

		String dirPath = prefs.get(DRUM_NOTE_MAP_DIR_PREF_KEY, null);
		File dir;
		if (dirPath == null || !(dir = new File(dirPath)).isDirectory())
			dir = Util.getLotroMusicPath(false /* create */);

		JFileChooser fileChooser = new JFileChooser(dir);
		fileChooser.setFileFilter(new ExtensionFileFilter("Drum Map", DrumNoteMap.FILE_SUFFIX));

		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return false;

		File loadFile = fileChooser.getSelectedFile();

		try {
			abcPart.getDrumMap(trackInfo.getTrackNumber()).load(loadFile);
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Failed to load drum map:\n\n" + e.getMessage(),
					"Failed to load drum map", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		catch (ParseException e) {
			JOptionPane.showMessageDialog(this, "Failed to load drum map:\n\n" + e.getMessage(),
					"Failed to load drum map", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		prefs.put(DRUM_NOTE_MAP_DIR_PREF_KEY, fileChooser.getCurrentDirectory().getAbsolutePath());
		return true;
	}

	public void dispose() {
		abcPart.removeAbcListener(abcListener);
		seq.removeChangeListener(seqListener);
		noteGraph.dispose();
	}

	private class TrackTransposeModel extends SpinnerNumberModel {
		public TrackTransposeModel(int value, int minimum, int maximum, int stepSize) {
			super(value, minimum, maximum, stepSize);
		}

		@Override
		public void setValue(Object value) {
			if (!(value instanceof Integer))
				throw new IllegalArgumentException();

			if ((Integer) value % 12 != 0)
				throw new IllegalArgumentException();

			super.setValue(value);
		}
	}

	private class TrackNoteGraph extends NoteGraph {
		public TrackNoteGraph(SequencerWrapper sequencer, TrackInfo trackInfo) {
			super(sequencer, trackInfo, Note.C2.id - 12, Note.C5.id + 12);
		}

		@Override
		protected int transposeNote(int noteId) {
			if (!trackInfo.isDrumTrack()) {
				noteId += abcPart.getTranspose(trackInfo.getTrackNumber());
			}
			return noteId;
		}

		@Override
		protected boolean isNotePlayable(int noteId) {
			if (abcPart.isDrumPart()) {
				return abcPart.isDrumPlayable(trackInfo.getTrackNumber(), noteId);
			}
			else if (trackInfo.isDrumTrack() && !abcPart.isTrackEnabled(trackInfo.getTrackNumber())) {
				return true;
			}
			else {
				int minPlayable = abcPart.getInstrument().lowestPlayable.id;
				int maxPlayable = abcPart.getInstrument().highestPlayable.id;
				return (noteId >= minPlayable) && (noteId <= maxPlayable);
			}
		}

		@Override
		protected List<NoteEvent> getEvents() {
			if (showDrumPanels)
				return Collections.emptyList();

			return super.getEvents();
		}
	}
}

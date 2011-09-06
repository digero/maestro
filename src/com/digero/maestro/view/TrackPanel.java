package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
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
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.Util;
import com.digero.common.view.NoteGraph;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.midi.NoteFilterSequencerWrapper;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.IDisposable;

@SuppressWarnings("serial")
public class TrackPanel extends JPanel implements IDisposable, TableLayoutConstants, ICompileConstants {
	//              0              1               2
	//   +--------------------+----------+--------------------+
	//   |      TRACK NAME    | octave   |  +--------------+  |
	// 0 | [X]                | +----^-+ |  | (note graph) |  |
	//   |      Instrument(s) | +----v-+ |  +--------------+  |
	//   +--------------------+----------+--------------------+
	static final int TITLE_WIDTH = 140;
	static final int TITLE_WIDTH_DRUMS = 180;
	static final int SPINNER_WIDTH = 48;
	private static final double DRUM_ROW0 = 32;
	private static final double NOTE_ROW0 = 48;
	private static final double[] LAYOUT_COLS = new double[] {
			TITLE_WIDTH, SPINNER_WIDTH, FILL
	};
	private static final double[] LAYOUT_ROWS = new double[] {
		NOTE_ROW0
	};

	private final TrackInfo trackInfo;
	private final NoteFilterSequencerWrapper seq;
	private final AbcPart abcPart;

	private JCheckBox checkBox;
	private JSpinner transposeSpinner;
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

		add(noteGraph, "2, 0");

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

			add(checkBox, "0, 0");
			add(transposeSpinner, "1, 0, f, c");
		}
		else {
			((TableLayout) getLayout()).setRow(0, DRUM_ROW0);
			checkBox.setFont(checkBox.getFont().deriveFont(Font.ITALIC));

			add(checkBox, "0, 0, 1, 0");
		}

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

	public TrackInfo getTrackInfo() {
		return trackInfo;
	}

	private void updateState() {
		updateState(false);
	}

	private int curTitleWidth() {
		return abcPart.isDrumPart() ? TITLE_WIDTH_DRUMS : TITLE_WIDTH;
	}

	private void updateTitleText() {
		final int ELLIPSIS_OFFSET = 28;

		String title = trackInfo.getTrackNumber() + ". " + trackInfo.getName();
		String instr = trackInfo.getInstrumentNames();
		checkBox.setToolTipText("<html><b>" + title + "</b><br>" + instr + "</html>");

		if (!trackInfo.isDrumTrack()) {
			title = Util.ellipsis(title, curTitleWidth() - ELLIPSIS_OFFSET, checkBox.getFont().deriveFont(Font.BOLD));
			instr = Util.ellipsis(instr, curTitleWidth() - ELLIPSIS_OFFSET, checkBox.getFont());
			checkBox.setText("<html><b>" + title + "</b><br>" + instr + "</html>");
		}
		else {
			title = Util.ellipsis(title, curTitleWidth() - ELLIPSIS_OFFSET, checkBox.getFont());
			checkBox.setText("<html><b>" + title + "</b> (" + instr + ")</html>");
		}
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
			showDrumPanels = showDrumPanelsNew;
			wasDrumPart = abcPart.isDrumPart();

			for (int i = getComponentCount() - 1; i >= 0; --i) {
				Component child = getComponent(i);
				if (child instanceof DrumPanel) {
					((DrumPanel) child).dispose();
					remove(i);
				}
			}
			if (transposeSpinner != null)
				transposeSpinner.setVisible(!abcPart.isDrumPart());

			TableLayout layout = (TableLayout) getLayout();
			layout.setColumn(0, curTitleWidth());
			if (showDrumPanels) {
				int row = 1;
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
	}
}

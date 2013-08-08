package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.Note;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.IDiscardable;
import com.digero.common.view.ColorTable;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.abc.PartAutoNumberer;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.ISelectable;
import com.digero.maestro.util.SingleSelectionModel;

public class AbcPartPanel extends JPanel implements IDiscardable, ISelectable, TableLayoutConstants {
	//     0      1               2                     3
	//   +---+----------+---------------------+--------------------+
	//   |   | X: +--^+ | I: +---------+      |  +--------------+  |
	// 0 |[M]|    +--v+ |    +_Lute___v+      |  |              |  |
	//   |   +----------+---------------------+  | (note graph) |  |
	//   |[S]| T: +-------------------------+ |  |              |  |
	// 1 |   |    +-------------------------+ |  +--------------+  |
	//   +---+--------------------------------+--------------------+

	private static final double[] LAYOUT_COLUMNS = {
			PREFERRED, PREFERRED, FILL
	};
	private static final int MUTE_SOLO_COLUMN = 0;
	private static final int PART_NUMBER_COLUMN = 0;
	private static final int INSTRUMENT_COLUMN = 1;
	private static final int TITLE_COLUMN_START = 0;
	private static final int TITLE_COLUMN_END = 1;
	private static final int NOTE_GRAPH_COLUMN = 2;

	private static final double[] LAYOUT_ROWS = {
			PREFERRED, PREFERRED
	};
	private static final int MUTE_SOLO_ROW_START = 0;
	private static final int MUTE_SOLO_ROW_END = 1;
	private static final int META_INFO_ROW = 0;
	private static final int TITLE_ROW = 1;
	private static final int NOTE_GRAPH_ROW_START = 0;
	private static final int NOTE_GRAPH_ROW_END = 1;

	private AbcPart abcPart;
	private PartAutoNumberer partAutoNumberer;
	private SequencerWrapper abcSequencer;
	private SingleSelectionModel<AbcPartPanel> selectionModel;
	private TrackInfo abcTrackInfo;
	private NoteGraph noteGraph;

	private AbcPartListener abcPartListener;
	private SequencerListener abcSequencerListener;
	private MouseListener selectOnClickListener;
	private FocusListener selectOnFocusListener;

	private JSpinner partNumberSpinner;
	private JComboBox<LotroInstrument> instrumentComboBox;
	private JTextField nameTextField;
//	private JPanel selectionIndicator;

	private boolean selected = false;
	private Color borderColor = null;

	public AbcPartPanel(SequencerWrapper _abcSequencer, PartAutoNumberer _partAutoNumberer,
			SingleSelectionModel<AbcPartPanel> _selectionModel) {
		this(_abcSequencer, _partAutoNumberer, _selectionModel, null, null);
	}

	public AbcPartPanel(SequencerWrapper _abcSequencer, PartAutoNumberer _partAutoNumberer,
			SingleSelectionModel<AbcPartPanel> _selectionModel, AbcPart _abcPart, TrackInfo _trackInfo) {
		super(new TableLayout(LAYOUT_COLUMNS, LAYOUT_ROWS));

		this.abcSequencer = _abcSequencer;
		this.partAutoNumberer = _partAutoNumberer;
		this.selectionModel = _selectionModel;

		selectOnFocusListener = new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (selectionModel != null)
					selectionModel.setSelectedItem(AbcPartPanel.this);
			}
		};

		selectOnClickListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (selectionModel != null)
					selectionModel.setSelectedItem(AbcPartPanel.this);
			}
		};

		addMouseListener(selectOnClickListener);

//		selectionIndicator = new JPanel();

		SpinnerNumberModel partNumberSpinnerModel = new SpinnerNumberModel(0, 0, 999, partAutoNumberer.getIncrement());
		partNumberSpinner = new JSpinner(partNumberSpinnerModel);
		partNumberSpinner.addFocusListener(selectOnFocusListener);
		partNumberSpinnerModel.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (abcPart != null) {
					int newPartNumber = (Integer) partNumberSpinner.getValue();
					if (newPartNumber != abcPart.getPartNumber())
						partAutoNumberer.setPartNumber(abcPart, newPartNumber);
				}
			}
		});

		instrumentComboBox = new JComboBox<LotroInstrument>(partAutoNumberer.getSortedInstrumentList());
		instrumentComboBox.setMaximumRowCount(12);
		instrumentComboBox.addFocusListener(selectOnFocusListener);
		instrumentComboBox.addActionListener(new ActionListener() {
			private LotroInstrument lastSelectedInstrument = (LotroInstrument) instrumentComboBox.getSelectedItem();

			public void actionPerformed(ActionEvent e) {
				if (abcPart != null) {
					LotroInstrument newInstrument = (LotroInstrument) instrumentComboBox.getSelectedItem();
					if (newInstrument != abcPart.getInstrument()) {
						partAutoNumberer.setInstrument(abcPart, newInstrument);
						String title = abcPart.getTitle();
						title = title.replace(lastSelectedInstrument.toString(), newInstrument.toString());
						nameTextField.setText(title);
						lastSelectedInstrument = newInstrument;
					}
				}
			}
		});

		nameTextField = new JTextField();
		nameTextField.addFocusListener(selectOnFocusListener);
		nameTextField.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				if (abcPart != null)
					abcPart.setTitle(nameTextField.getText());
			}

			public void insertUpdate(DocumentEvent e) {
				if (abcPart != null)
					abcPart.setTitle(nameTextField.getText());
			}

			public void changedUpdate(DocumentEvent e) {
				if (abcPart != null)
					abcPart.setTitle(nameTextField.getText());
			}
		});

		noteGraph = new PartNoteGraph(abcSequencer);
		noteGraph.addMouseListener(selectOnClickListener);

		abcPartListener = new AbcPartListener() {
			@Override
			public void abcPartChanged(AbcPartEvent e) {
				switch (e.getProperty()) {
				case TITLE:
					if (!nameTextField.getText().equals(abcPart.getTitle()))
						nameTextField.setText(abcPart.getTitle());
					break;
				case PART_NUMBER:
					if (!partNumberSpinner.getValue().equals(abcPart.getPartNumber()))
						partNumberSpinner.setValue(abcPart.getPartNumber());
					break;
				case INSTRUMENT:
					if (!instrumentComboBox.getSelectedItem().equals(abcPart.getInstrument()))
						instrumentComboBox.setSelectedItem(abcPart.getInstrument());
					break;
				}
			}
		};

		abcSequencer.addChangeListener(abcSequencerListener = new SequencerListener() {
			@Override
			public void propertyChanged(SequencerEvent evt) {
				switch (evt.getProperty()) {
				case TRACK_ACTIVE:
					updateColors();
					break;
				}
			}
		});

		JPanel partNumberPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		partNumberPanel.add(new JLabel("X: "));
		partNumberPanel.add(partNumberSpinner);

		JPanel instrumentPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		instrumentPanel.add(new JLabel("I: "));
		instrumentPanel.add(instrumentComboBox);

		JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		titlePanel.add(new JLabel("T: "));
		titlePanel.add(nameTextField);

//		add(selectionIndicator, MUTE_SOLO_COLUMN + ", " + MUTE_SOLO_ROW_START + ", " + MUTE_SOLO_COLUMN + ", "
//				+ MUTE_SOLO_ROW_END);
		add(partNumberPanel, PART_NUMBER_COLUMN + ", " + META_INFO_ROW);
		add(instrumentPanel, INSTRUMENT_COLUMN + ", " + META_INFO_ROW);
		add(titlePanel, TITLE_COLUMN_START + ", " + TITLE_ROW + ", " + TITLE_COLUMN_END + ", " + TITLE_ROW);
		add(noteGraph, NOTE_GRAPH_COLUMN + ", " + NOTE_GRAPH_ROW_START + ", " + NOTE_GRAPH_COLUMN + ", "
				+ NOTE_GRAPH_ROW_END);

		setAbcPart(_abcPart, _trackInfo, true);
		updateColors();
	}

	@Override
	public void discard() {
		if (abcPart != null)
			abcPart.removeAbcListener(abcPartListener);

		if (abcSequencer != null)
			abcSequencer.removeChangeListener(abcSequencerListener);

		if (noteGraph != null)
			noteGraph.removeMouseListener(selectOnClickListener);

		if (selectionModel != null && selectionModel.getSelectedItem() == this)
			selectionModel.setSelectedItem(null);
		selectionModel = null;
	}

	@Override
	public void setSelected(boolean selected) {
		if (this.selected != selected) {
			this.selected = selected;

			if (selectionModel != null) {
				if (selected && selectionModel.getSelectedItem() != this)
					selectionModel.setSelectedItem(this);
				else if (!selected && selectionModel.getSelectedItem() == this)
					selectionModel.setSelectedItem(null);
			}

			updateColors();
		}
	}

	public AbcPart getAbcPart() {
		return abcPart;
	}

	public void setAbcPart(AbcPart abcPart, TrackInfo abcTrackInfo) {
		setAbcPart(abcPart, abcTrackInfo, false);
	}

	private void setAbcPart(AbcPart abcPart, TrackInfo abcTrackInfo, boolean forceRefresh) {
		if (this.abcPart == abcPart && this.abcTrackInfo == abcTrackInfo && !forceRefresh)
			return;

		if (this.abcPart != null) {
			this.abcPart.removeAbcListener(abcPartListener);
			this.abcPart = null;
		}

		partNumberSpinner.setEnabled(abcPart != null);
		instrumentComboBox.setEnabled(abcPart != null);
		nameTextField.setEnabled(abcPart != null);
		if (abcPart == null) {
			partNumberSpinner.setValue(0);
			instrumentComboBox.setSelectedIndex(0);
			nameTextField.setText("");
		}
		else {
			partNumberSpinner.setValue(abcPart.getPartNumber());
			instrumentComboBox.setSelectedItem(abcPart.getInstrument());
			nameTextField.setText(abcPart.getTitle());
			abcPart.addAbcListener(abcPartListener);
		}

		this.abcPart = abcPart;
		this.abcTrackInfo = abcTrackInfo;
		noteGraph.setTrackInfo(abcTrackInfo);
		updateColors();
	}

	public TrackInfo getAbcTrackInfo() {
		return abcTrackInfo;
	}

	private void updateColors() {
		ColorTable newBorderColor;

		if (abcTrackInfo == null || !abcSequencer.isTrackActive(abcTrackInfo.getTrackNumber())) {
			noteGraph.setNoteColor(ColorTable.NOTE_ABC_OFF);
			noteGraph.setBadNoteColor(ColorTable.NOTE_BAD_OFF);
			setBackground(ColorTable.GRAPH_BACKGROUND_OFF.get());
			newBorderColor = selected ? ColorTable.ABC_BORDER_SELECTED_OFF : ColorTable.ABC_BORDER_UNSELECTED_OFF;
		}
		else {
			noteGraph.setNoteColor(ColorTable.NOTE_ABC_ENABLED);
			noteGraph.setBadNoteColor(ColorTable.NOTE_BAD_ENABLED);
			setBackground(ColorTable.GRAPH_BACKGROUND_ENABLED.get());
			newBorderColor = selected ? ColorTable.ABC_BORDER_SELECTED_ENABLED
					: ColorTable.ABC_BORDER_UNSELECTED_ENABLED;
		}

		if (!newBorderColor.get().equals(borderColor)) {
			borderColor = newBorderColor.get();
			Border outerBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, ColorTable.PANEL_BORDER.get());
//			Border innerBorder = BorderFactory.createMatteBorder(0, 32, 0, 0, borderColor);
//			setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
			setBorder(outerBorder);
		}
	}

	private class PartNoteGraph extends NoteGraph {
		public PartNoteGraph(SequencerWrapper sequencer) {
			super(sequencer, null, Note.MIN_PLAYABLE.id - 12, Note.MAX_PLAYABLE.id + 12);
		}

		@Override
		protected boolean isNotePlayable(int noteId) {
			return abcPart.getInstrument().isPlayable(noteId);
		}
	}
}

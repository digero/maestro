package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.IDiscardable;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.abc.PartAutoNumberer;
import com.digero.maestro.midi.NoteFilterSequencerWrapper;

@SuppressWarnings("unused") // TODO remove this
public class PartPanelNew extends JPanel implements IDiscardable, TableLayoutConstants {
	//     0      1               2                     3
	//   +---+----------+---------------------+--------------------+
	//   |   | X: +--^+ | I: +---------+      |  +--------------+  |
	// 0 |   |    +--v+ |    +_Lute___v+      |  |              |  |
	//   |[X]+----------+---------------------+  | (note graph) |  |
	//   |   | T: +-------------------------+ |  |              |  |
	// 1 |   |    +-------------------------+ |  +--------------+  |
	//   +---+--------------------------------+--------------------+
	//   |   |   ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~   |
	// 2 |   |  ~ ~ ~  Track Panels   ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~  |
	//   |   |   ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~   |
	//   +---+-----------------------------------------------------+

	static final double[] LAYOUT_COLUMNS = {
			PREFERRED, PREFERRED, PREFERRED, FILL
	};
	static final int CHECKBOX_COLUMN = 0;
	static final int PART_NUMBER_COLUMN = 1;
	static final int INSTRUMENT_COLUMN = 2;
	static final int NOTE_GRAPH_COLUMN = 3;
	static final int TITLE_COLUMN_START = 0;
	static final int TITLE_COLUMN_END = 1;
	static final int TRACK_PANEL_COLUMN_START = 0;
	static final int TRACK_PANEL_COLUMN_END = 3;

	static final double[] LAYOUT_ROWS = {
			PREFERRED, PREFERRED, PREFERRED
	};
	static final int META_INFO_ROW = 0;
	static final int TITLE_ROW = 1;
	static final int TRACK_PANEL_ROW = 2;

	private AbcPart abcPart;
	private PartAutoNumberer partAutoNumberer;
	private NoteFilterSequencerWrapper midiSequencer;
	private SequencerWrapper abcSequencer;

	private JCheckBox enabledCheckBox;
	private JSpinner partNumberSpinner;
	private SpinnerNumberModel partNumberSpinnerModel;
	private JTextField nameTextField;
	private JComboBox<LotroInstrument> instrumentComboBox;
	private JPanel trackListPanel;

	private SequencerListener abcSequencerListener;
	private AbcPartListener abcPartListener;

	private LotroInstrument lastSelectedInstrument = null;

	private enum TracksVisibleState {
		NONE, SELECTED_ONLY, ALL
	}

	private TracksVisibleState tracksVisibleState = TracksVisibleState.NONE;

	public PartPanelNew(AbcPart _abcPart, SequencerWrapper _abcSequencer, NoteFilterSequencerWrapper _midiSequencer,
			PartAutoNumberer _partAutoNumberer) {
		super(new TableLayout(LAYOUT_COLUMNS, LAYOUT_ROWS));

		this.abcPart = _abcPart;
		this.abcSequencer = _abcSequencer;
		this.midiSequencer = _midiSequencer;
		this.partAutoNumberer = _partAutoNumberer;

		enabledCheckBox = new JCheckBox();
		enabledCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// TODO Mute on ABC Sequencer, and disable ABC part?
			}
		});

		partNumberSpinnerModel = new SpinnerNumberModel(0, 0, 999, partAutoNumberer.getIncrement());
		partNumberSpinner = new JSpinner(partNumberSpinnerModel);
		partNumberSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				partAutoNumberer.setPartNumber(abcPart, (Integer) partNumberSpinner.getValue());
			}
		});

		nameTextField = new JTextField(32);
		nameTextField.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				abcPart.setTitle(nameTextField.getText());
			}

			public void insertUpdate(DocumentEvent e) {
				abcPart.setTitle(nameTextField.getText());
			}

			public void changedUpdate(DocumentEvent e) {
				abcPart.setTitle(nameTextField.getText());
			}
		});

		instrumentComboBox = new JComboBox<LotroInstrument>();
		instrumentComboBox.setMaximumRowCount(12);
		instrumentComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LotroInstrument newInstrument = (LotroInstrument) instrumentComboBox.getSelectedItem();
				partAutoNumberer.setInstrument(abcPart, newInstrument);
				String title = abcPart.getTitle();
				title = title.replace(lastSelectedInstrument.toString(), newInstrument.toString());
				nameTextField.setText(title);
				lastSelectedInstrument = newInstrument;
				// TODO ? updateTracksVisible();
			}
		});

		abcPart.addAbcListener(abcPartListener = new AbcPartListener() {
			public void abcPartChanged(AbcPartEvent e) {
				// TODO Auto-generated method stub
			}
		});
		// TODO init from abcPart

		// TODO init trackListPanel
		trackListPanel = new JPanel(new TableLayout(new double[] {
			TableLayout.FILL
		}, new double[] {

		}));
		
		// TODO parentSelectionModel
	}

	private void updateTracksVisible() {
		TracksVisibleState newState = null;
		
//		if () {
//			
//		}
	}

//	private static JPanel createLabelPanel(String label, JComponent component) {
//		JPanel panel = new JPanel(new BorderLayout());
//		panel.add(new JLabel(label), BorderLayout.WEST);
//		panel.add(component, BorderLayout.CENTER);
//		return panel;
//	}

	@Override
	public void discard() {
		abcPart.removeAbcListener(abcPartListener);

		for (Component child : trackListPanel.getComponents()) {
			if (child instanceof IDiscardable) {
				((IDiscardable) child).discard();
			}
		}
		// TODO Auto-generated method stub
	}
}

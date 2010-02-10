package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import sun.awt.shell.ShellFolder;

import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.midi.KeySignature;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.SequencerWrapper;
import com.digero.maestro.midi.TimeSignature;

@SuppressWarnings("serial")
public class ProjectFrame extends JFrame implements TableLayoutConstants {
	private static final int HGAP = 4, VGAP = 4;
	private static final double[] LAYOUT_COLS = new double[] {
			192, FILL
	};
	private static final double[] LAYOUT_ROWS = new double[] {
			FILL, PREFERRED, PREFERRED
	};

	private File saveFile;
	private SequenceInfo sequenceInfo;
	private SequencerWrapper sequencer;
	private DefaultListModel parts = new DefaultListModel();

	private JPanel content;
	private JSpinner transposeSpinner;
	private JSpinner tempoSpinner;
	private JFormattedTextField keySignatureField;
	private JFormattedTextField timeSignatureField;

	private JList partsList;
	private JButton newPartButton;
	private JButton deletePartButton;

	private PartPanel partPanel;

	private ImageIcon playIcon, pauseIcon, stopIcon;
	private JButton playButton;
	private JButton stopButton;
	private SongPositionBar songPositionBar;

	public ProjectFrame(SequenceInfo sequenceInfo) {
		this(sequenceInfo, 0, sequenceInfo.getTempo(), sequenceInfo.getTimeSignature(), sequenceInfo.getKeySignature());
	}

	public ProjectFrame(SequenceInfo sequenceInfo, int transpose, int tempoBPM, TimeSignature timeSignature,
			KeySignature keySignature) {
		super("LotRO Maestro");
		setBounds(200, 200, 800, 600);

		this.sequenceInfo = sequenceInfo;
		this.sequencer = new SequencerWrapper();
		try {
			this.sequencer.setSequence(this.sequenceInfo.getSequence());
		}
		catch (InvalidMidiDataException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		partPanel = new PartPanel(this, sequencer);
		partPanel.setBorder(BorderFactory.createTitledBorder("Part Settings"));

		TableLayout tableLayout = new TableLayout(LAYOUT_COLS, LAYOUT_ROWS);
		tableLayout.setHGap(HGAP);
		tableLayout.setVGap(VGAP);

		content = new JPanel(tableLayout, false);
		setContentPane(content);

		keySignatureField = new MyFormattedTextField(keySignature, 5);

		timeSignatureField = new MyFormattedTextField(timeSignature, 5);

		transposeSpinner = new JSpinner(new SpinnerNumberModel(transpose, -48, 48, 1));
		transposeSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				for (int i = 0; i < parts.getSize(); i++) {
					AbcPart part = (AbcPart) parts.getElementAt(i);
					part.setBaseTranspose(getTranspose());
				}
			}
		});

		tempoSpinner = new JSpinner(new SpinnerNumberModel(tempoBPM, 1, 600, 2));

		partsList = new JList(parts);
		partsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		partsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				int idx = partsList.getSelectedIndex();
				if (idx != -1)
					partPanel.setAbcPart((AbcPart) parts.getElementAt(idx));
				else
					partPanel.setAbcPart(null);
			}
		});

		JScrollPane partsListScrollPane = new JScrollPane(partsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		newPartButton = new JButton("New Part");
		newPartButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbcPart newPart = new AbcPart(ProjectFrame.this.sequenceInfo, getTranspose(), calcNextPartNumber());
				newPart.addChangeListener(partChangeListener);
				parts.addElement(newPart);
				partsList.setSelectedIndex(parts.indexOf(newPart));
			}
		});

		deletePartButton = new JButton("Delete");
		deletePartButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String title = "Delete Part";
				String msg = "Are you sure you want to delete the selected part?\nThis cannot be undone.";
				int r = JOptionPane.showConfirmDialog(ProjectFrame.this, msg, title, JOptionPane.YES_NO_OPTION);
				if (r == JOptionPane.YES_OPTION) {
					AbcPart oldPart = (AbcPart) parts.remove(partsList.getSelectedIndex());
					oldPart.removeChangeListener(partChangeListener);
				}
			}
		});

		JPanel partsButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
		partsButtonPanel.add(newPartButton);
		partsButtonPanel.add(deletePartButton);

		JPanel partsListPanel = new JPanel(new BorderLayout(HGAP, VGAP));
		partsListPanel.setBorder(BorderFactory.createTitledBorder("Song Parts"));
		partsListPanel.add(partsListScrollPane, BorderLayout.CENTER);
		partsListPanel.add(partsButtonPanel, BorderLayout.SOUTH);

		TableLayout settingsLayout = new TableLayout(new double[] {
				/* Cols */PREFERRED, PREFERRED, FILL
		}, new double[] {
				/* Rows */PREFERRED, PREFERRED, PREFERRED, PREFERRED
		});
		settingsLayout.setVGap(VGAP);
		settingsLayout.setHGap(HGAP);

		JPanel settingsPanel = new JPanel(settingsLayout);
		settingsPanel.setBorder(BorderFactory.createTitledBorder("Export Settings"));
		settingsPanel.add(new JLabel("Transpose:"), "0, 0");
		settingsPanel.add(new JLabel("Tempo:"), "0, 1");
		settingsPanel.add(new JLabel("Meter:"), "0, 2");
		settingsPanel.add(new JLabel("Key:"), "0, 3");
		settingsPanel.add(transposeSpinner, "1, 0");
		settingsPanel.add(tempoSpinner, "1, 1");
		settingsPanel.add(timeSignatureField, "1, 2, 2, 2, L, F");
		settingsPanel.add(keySignatureField, "1, 3, 2, 3, L, F");

		playIcon = new ImageIcon(ProjectFrame.class.getResource("icons/play.png"));
		pauseIcon = new ImageIcon(ProjectFrame.class.getResource("icons/pause.png"));
		stopIcon = new ImageIcon(ProjectFrame.class.getResource("icons/stop.png"));

		playButton = new JButton(playIcon);
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playPause();
			}
		});

		stopButton = new JButton(stopIcon);
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});

		songPositionBar = new SongPositionBar(sequencer);

		JPanel playControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		playControlPanel.add(playButton);
		playControlPanel.add(stopButton);

		JPanel playPanel = new JPanel(new BorderLayout(4, 4));
		playPanel.add(songPositionBar, BorderLayout.CENTER);
		playPanel.add(playControlPanel, BorderLayout.SOUTH);

		add(partsListPanel, "0, 0");
		add(settingsPanel, "0, 1, 0, 2");
		add(partPanel, "1, 0, 1, 1");
		add(playPanel, "1, 2");

		new DropTarget(this, new MyDropListener());
		newPartButton.doClick();
	}

	private ChangeListener partChangeListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
			int idx = parts.indexOf(e.getSource());
			if (idx >= 0) {
				// Make the list model fire the contentsChanged event, 
				// which will cause a repaint
				parts.set(idx, e.getSource());
			}
		}
	};

	public int getTranspose() {
		return (Integer) transposeSpinner.getValue();
	}

	public int getTempo() {
		return (Integer) tempoSpinner.getValue();
	}

	public KeySignature getKeySignature() {
		return (KeySignature) keySignatureField.getValue();
	}

	public TimeSignature getTimeSignature() {
		return (TimeSignature) timeSignatureField.getValue();
	}

	public int calcNextPartNumber() {
		int size = parts.getSize();
		outerLoop: for (int n = 1; n <= size; n++) {
			for (int i = 0; i < size; i++) {
				if (n == ((AbcPart) parts.getElementAt(i)).getPartNumber())
					continue outerLoop;
			}
			return n;
		}
		return size + 1;
	}

	private void playPause() {
		if (!sequencer.isRunning()) {
			if (sequencer.getPosition() >= sequencer.getLength()) {
				sequencer.setPosition(0, this);
			}

			sequencer.start(this);
			playButton.setIcon(pauseIcon);
			stopButton.setEnabled(true);
		}
		else {
			sequencer.stop(this);
			playButton.setIcon(playIcon);
		}
	}

	private void stop() {
		sequencer.stop(this);
		sequencer.setPosition(0, this);
		stopButton.setEnabled(false);
		playButton.setIcon(playIcon);
	}

	private void close() {
		for (int i = 0; i < parts.size(); i++) {
			AbcPart part = (AbcPart) parts.get(i);
			part.removeChangeListener(partChangeListener);
		}
		parts.clear();

		try {
			sequencer.setSequence(null);
		}
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	private void openSong(File midiFile) {
		stop();

		try {
			for (int i = 0; i < parts.size(); i++) {
				AbcPart part = (AbcPart) parts.get(i);
				part.removeChangeListener(partChangeListener);
			}
			parts.clear();

			this.sequenceInfo = new SequenceInfo(midiFile);
			sequencer.setSequence(sequenceInfo.getSequence());
			newPartButton.doClick();
		}
		catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Slight modification to JFormattedTextField to select the contents when it
	 * receives focus.
	 */
	private class MyFormattedTextField extends JFormattedTextField {
		public MyFormattedTextField(Object value, int columns) {
			super(value);
			setColumns(columns);
		}

		@Override
		protected void processFocusEvent(FocusEvent e) {
			super.processFocusEvent(e);
			if (e.getID() == FocusEvent.FOCUS_GAINED)
				selectAll();
		}
	}

	private class MyDropListener implements DropTargetListener {
		private File draggingFile = null;

		public void dragEnter(DropTargetDragEvent dtde) {
			draggingFile = getMidiFile(dtde.getTransferable());
			if (draggingFile != null) {
				dtde.acceptDrag(DnDConstants.ACTION_COPY);
			}
			else {
				dtde.rejectDrag();
			}
		}

		public void dragExit(DropTargetEvent dte) {
		}

		public void dragOver(DropTargetDragEvent dtde) {
		}

		public void drop(DropTargetDropEvent dtde) {
			if (draggingFile != null) {
				dtde.acceptDrop(DnDConstants.ACTION_COPY);
				openSong(draggingFile);
				draggingFile = null;
			}
			else {
				dtde.rejectDrop();
			}
		}

		public void dropActionChanged(DropTargetDragEvent dtde) {
			draggingFile = getMidiFile(dtde.getTransferable());
			if (draggingFile != null) {
				dtde.acceptDrag(DnDConstants.ACTION_COPY);
			}
			else {
				dtde.rejectDrag();
			}
		}

		@SuppressWarnings("unchecked")
		private File getMidiFile(Transferable t) {
			if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				List<File> files;
				try {
					files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				}
				catch (Exception e) {
					return null;
				}
				if (files.size() >= 1) {
					File file = files.get(0);
					String name = file.getName().toLowerCase();

					if (name.endsWith(".lnk")) {
						try {
							file = ShellFolder.getShellFolder(file).getLinkLocation();
							name = file.getName();
						}
						catch (Throwable e) {
							return null;
						}
					}

					if (name.endsWith(".mid") || name.endsWith(".midi")) {
						return file;
					}
				}
			}
			return null;
		}
	}

}

package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.Sequence;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.common.midi.Note;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.Util;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.IDisposable;

@SuppressWarnings("serial")
public class TrackPanel extends JPanel implements IDisposable, TableLayoutConstants, TrackPanelConstants,
		ICompileConstants {
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

	private TrackInfo trackInfo;
	private SequencerWrapper seq;
	private AbcPart abcPart;

	private JCheckBox checkBox;
	private JSpinner transposeSpinner;
	private NoteGraph noteGraph;

	private AbcPartListener abcListener;

	private boolean showDrumPanels;
	private boolean wasDrumPart;

	public TrackPanel(TrackInfo info, SequencerWrapper sequencer, AbcPart part) {
		super(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));

		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, PANEL_BORDER));

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

		noteGraph = new NoteGraph();
		noteGraph.setOpaque(false);
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
				if (e.isPreviewRelated())
					updateState();
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

	private void updateState(boolean initDrumPanels) {
		boolean trackEnabled = abcPart.isTrackEnabled(trackInfo.getTrackNumber());
		boolean inputEnabled = abcPart.isDrumPart() == trackInfo.isDrumTrack();
		setBackground(trackEnabled ? PANEL_BACKGROUND_ENABLED : PANEL_BACKGROUND_DISABLED);
		checkBox.setForeground(trackEnabled ? (abcPart.isDrumPart() ? PANEL_DRUM_TEXT_ENABLED : PANEL_TEXT_ENABLED)
				: (inputEnabled ? PANEL_TEXT_DISABLED : PANEL_TEXT_OFF));

//		checkBox.setEnabled(inputEnabled);
		checkBox.setSelected(trackEnabled);

		boolean showDrumPanelsNew = abcPart.isDrumPart() && trackEnabled;
		if (initDrumPanels || showDrumPanels != showDrumPanelsNew || wasDrumPart != abcPart.isDrumPart()) {
			showDrumPanels = showDrumPanelsNew;
			wasDrumPart = abcPart.isDrumPart();

			for (int i = getComponentCount() - 1; i >= 0; --i) {
				if (getComponent(i) instanceof DrumPanel) {
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

	private static final int MIN_RENDERED = Note.C2.id - 12;
	private static final int MAX_RENDERED = Note.C5.id + 12;

	private class NoteGraph extends JPanel implements IDisposable, TrackPanelConstants {
		public NoteGraph() {
			abcPart.addAbcListener(myChangeListener);
			seq.addChangeListener(myChangeListener);

			MyMouseListener mouseListener = new MyMouseListener();
			addMouseListener(mouseListener);
			addMouseMotionListener(mouseListener);

			setPreferredSize(new Dimension(200, 24));
		}

		public void dispose() {
			abcPart.removeAbcListener(myChangeListener);
			seq.removeChangeListener(myChangeListener);
		}

		private MyChangeListener myChangeListener = new MyChangeListener();

		private class MyChangeListener implements AbcPartListener, SequencerListener {
			@Override
			public void abcPartChanged(AbcPartEvent e) {
				if (e.isPreviewRelated())
					repaint();
			}

			@Override
			public void propertyChanged(SequencerEvent evt) {
				repaint();
			}
		}

		private Rectangle2D.Double rectTmp = new Rectangle2D.Double();
		private Line2D.Double lineTmp = new Line2D.Double();

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			Sequence song = trackInfo.getSequenceInfo().getSequence();
			if (song == null)
				return;

			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			AffineTransform xform = getTransform();
			double minLength = NOTE_WIDTH_PX / xform.getScaleX();
			double height = Math.abs(NOTE_HEIGHT_PX / xform.getScaleY());

			int trackNumber = trackInfo.getTrackNumber();
			Color noteColor;
			Color xnoteColor;
			Color drumColor;
			Color borderColor;
			Color bkgdColor;

			List<Rectangle2D> notesUnplayable = new ArrayList<Rectangle2D>();
			List<Rectangle2D> notesPlaying = null;

			if (seq.isTrackActive(trackNumber)) {
				if (abcPart.isTrackEnabled(trackNumber)) {
					noteColor = abcPart.isDrumPart() ? NOTE_OFF : NOTE_ENABLED;
					xnoteColor = abcPart.isDrumPart() ? NOTE_BAD_OFF : NOTE_BAD_ENABLED;
					drumColor = !abcPart.isDrumPart() ? NOTE_OFF : NOTE_DRUM_DISABLED;//NOTE_DRUM_ENABLED;
					borderColor = GRAPH_BORDER_ENABLED;
					bkgdColor = GRAPH_BACKGROUND_ENABLED;
				}
				else {
					noteColor = abcPart.isDrumPart() ? NOTE_OFF : NOTE_DISABLED;
					xnoteColor = abcPart.isDrumPart() ? NOTE_BAD_OFF : NOTE_BAD_DISABLED;
					drumColor = !abcPart.isDrumPart() ? NOTE_OFF : NOTE_DRUM_DISABLED;
					borderColor = GRAPH_BORDER_DISABLED;
					bkgdColor = GRAPH_BACKGROUND_DISABLED;
				}

				if (seq.isRunning())
					notesPlaying = new ArrayList<Rectangle2D>();
			}
			else {
				noteColor = NOTE_OFF;
				xnoteColor = NOTE_BAD_OFF;
				drumColor = !abcPart.isDrumPart() ? NOTE_OFF : NOTE_DRUM_OFF;
				borderColor = GRAPH_BORDER_OFF;
				bkgdColor = GRAPH_BACKGROUND_OFF;
			}

			long songPos = seq.getPosition();
			int transpose = abcPart.getTranspose(trackNumber);
			int minPlayable = abcPart.getInstrument().lowestPlayable.id;
			int maxPlayable = abcPart.getInstrument().highestPlayable.id;

			if (GRAPH_HAS_BORDER) {
				g2.setColor(borderColor);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), GRAPH_BORDER_ROUNDED, GRAPH_BORDER_ROUNDED);
				g2.setColor(bkgdColor);
				g2.fillRoundRect(GRAPH_BORDER_SIZE, GRAPH_BORDER_SIZE, getWidth() - 2 * GRAPH_BORDER_SIZE, getHeight()
						- 2 * GRAPH_BORDER_SIZE, GRAPH_BORDER_ROUNDED, GRAPH_BORDER_ROUNDED);
			}
			else {
				g2.setColor(bkgdColor);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), GRAPH_BORDER_ROUNDED, GRAPH_BORDER_ROUNDED);
			}

			g2.transform(xform);

			// Paint the playable notes and keep track of the currently sounding and unplayable notes
			g2.setColor(noteColor);

			for (NoteEvent evt : trackInfo.getEvents()) {
				int id = evt.note.id;
				if (!trackInfo.isDrumTrack())
					id += transpose;
				double width = Math.max(minLength, evt.getLength());
				double y;
				boolean playable;

				if (id < minPlayable) {
					y = Math.max(id, MIN_RENDERED);
					playable = trackInfo.isDrumTrack();
				}
				else if (id > maxPlayable) {
					y = Math.min(id, MAX_RENDERED);
					playable = trackInfo.isDrumTrack();
				}
				else {
					y = id;
					playable = true;
				}

				if (notesPlaying != null && songPos >= evt.startMicros && songPos <= evt.endMicros) {
					notesPlaying.add(new Rectangle2D.Double(evt.startMicros, y, width, height));
				}
				else if (!playable) {
					notesUnplayable.add(new Rectangle2D.Double(evt.startMicros, y, width, height));
				}
				else {
					if (trackInfo.isDrumTrack())
						g2.setColor(drumColor);
					else
						g2.setColor(noteColor);

					rectTmp.setRect(evt.startMicros, y, width, height);
					g2.fill(rectTmp);
				}
			}

			// Paint the unplayable notes above the playable ones
			g2.setColor(xnoteColor);
			for (Rectangle2D rect : notesUnplayable) {
				g2.fill(rect);
			}

			// Paint the currently playing notes last
			if (notesPlaying != null) {
				g2.setColor(NOTE_ON);
				for (Rectangle2D rect : notesPlaying) {
					g2.fill(rect);
				}
			}

			// Draw the indicator line
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setColor(INDICATOR_COLOR);
			long thumbPos = seq.getThumbPosition();
			lineTmp.setLine(thumbPos, MIN_RENDERED, thumbPos, MAX_RENDERED + height);
			g2.draw(lineTmp);
		}

		/**
		 * Gets a transform that converts song coordinates into screen
		 * coordinates.
		 */
		private AffineTransform getTransform() {
			Sequence song = trackInfo.getSequenceInfo().getSequence();
			if (song == null)
				return new AffineTransform();

			double scrnX = GRAPH_BORDER_SIZE;
			double scrnY = GRAPH_BORDER_SIZE + NOTE_HEIGHT_PX;
			double scrnW = getWidth() - 2 * GRAPH_BORDER_SIZE;
			double scrnH = getHeight() - 2 * GRAPH_BORDER_SIZE - NOTE_HEIGHT_PX;

			double noteX = 0;
			double noteY = MAX_RENDERED;
			double noteW = song.getMicrosecondLength();
			double noteH = MIN_RENDERED - MAX_RENDERED;

			if (noteW <= 0 || scrnW <= 0 || scrnH <= 0)
				return new AffineTransform();

			AffineTransform scrnXForm = new AffineTransform(scrnW, 0, 0, scrnH, scrnX, scrnY);
			AffineTransform noteXForm = new AffineTransform(noteW, 0, 0, noteH, noteX, noteY);
			try {
				noteXForm.invert();
			}
			catch (NoninvertibleTransformException e) {
				e.printStackTrace();
				return new AffineTransform();
			}
			scrnXForm.concatenate(noteXForm);

			return scrnXForm;
		}

		private class MyMouseListener extends MouseAdapter {
			private long positionFromEvent(MouseEvent e) {
				AffineTransform xform = getTransform();
				Point2D.Double pt = new Point2D.Double(e.getX(), e.getY());
				try {
					xform.inverseTransform(pt, pt);
					long ret = (long) pt.x;
					if (ret < 0)
						ret = 0;
					if (ret >= seq.getLength())
						ret = seq.getLength() - 1;
					return ret;
				}
				catch (NoninvertibleTransformException e1) {
					e1.printStackTrace();
					return 0;
				}
			}

			private boolean isDragCanceled(MouseEvent e) {
				int x = e.getX();
				if (x < -32 || x > getWidth() + 32)
					return true;

				Component ancestor = SwingUtilities.getAncestorOfClass(JScrollPane.class, NoteGraph.this);
				if (ancestor == null)
					ancestor = SwingUtilities.getRoot(NoteGraph.this);

				int y = SwingUtilities.convertPoint(NoteGraph.this, e.getPoint(), ancestor).y;
				int h = ancestor.getHeight();
				return y < -32 || y > h + 32;
			}

			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					seq.setDragging(true);
					seq.setDragPosition(positionFromEvent(e));
				}
			}

			public void mouseDragged(MouseEvent e) {
				if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
					if (!isDragCanceled(e)) {
						seq.setDragging(true);
						seq.setDragPosition(positionFromEvent(e));
					}
					else {
						seq.setDragging(false);
					}
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					seq.setDragging(false);
					if (!isDragCanceled(e)) {
						seq.setPosition(positionFromEvent(e));
					}
				}
			}
		}
	}
}

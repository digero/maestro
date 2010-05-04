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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.Sequence;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.midi.Note;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequencerEvent;
import com.digero.maestro.midi.SequencerListener;
import com.digero.maestro.midi.SequencerWrapper;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.IDisposable;
import com.digero.maestro.util.Util;

@SuppressWarnings("serial")
public class TrackPanel extends JPanel implements IDisposable, TableLayoutConstants {
	//              0              1               2
	//   +--------------------+----------+--------------------+
	//   |      TRACK NAME    | octave   |  +--------------+  |
	// 0 | [X]                | +----^-+ |  | (note graph) |  |
	//   |      Instrument(s) | +----v-+ |  +--------------+  |
	//   +--------------------+----------+--------------------+
	static final int TITLE_WIDTH = 160;
	static final int SPINNER_WIDTH = 48;
	private static final double[] LAYOUT_COLS = new double[] {
			TITLE_WIDTH, SPINNER_WIDTH, FILL, 1
	};
	private static final double[] LAYOUT_ROWS = new double[] {
			4, 48, 4
	};

	private TrackInfo trackInfo;
	private SequencerWrapper seq;
	private AbcPart abcPart;

	private JCheckBox checkBox;
	private JSpinner transposeSpinner;
	private NoteGraph noteGraph;

	public TrackPanel(TrackInfo info, SequencerWrapper sequencer, AbcPart part) {
		super(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
//		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.LIGHT_GRAY, Color.GRAY));

		this.trackInfo = info;
		this.seq = sequencer;
		this.abcPart = part;

		TableLayout tableLayout = (TableLayout) getLayout();
		tableLayout.setHGap(4);

		checkBox = new JCheckBox();
		checkBox.setOpaque(false);
		checkBox.setSelected(abcPart.isTrackEnabled(trackInfo.getTrackNumber()));

		String title = trackInfo.getTrackNumber() + ". " + trackInfo.getName();
		String instr = trackInfo.getInstrumentNames();
		checkBox.setToolTipText("<html><b>" + title + "</b><br>" + instr + "</html>");

		title = Util.ellipsis(title, TITLE_WIDTH - 32, checkBox.getFont().deriveFont(Font.BOLD));
		instr = Util.ellipsis(instr, TITLE_WIDTH - 32, checkBox.getFont());
		checkBox.setText("<html><b>" + title + "</b><br>" + instr + "</html>");

		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int track = trackInfo.getTrackNumber();
				boolean enabled = checkBox.isSelected();
				abcPart.setTrackEnabled(track, enabled);
				seq.setTrackMute(track, !enabled, TrackPanel.this);
			}
		});

		JLabel octaveLabel = new JLabel("octave", SwingConstants.RIGHT);
		octaveLabel.setOpaque(false);
		octaveLabel.setFont(octaveLabel.getFont().deriveFont(Font.ITALIC));

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

		noteGraph = new NoteGraph();
		noteGraph.setOpaque(false);
		noteGraph.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3)
					seq.setTrackSolo(trackInfo.getTrackNumber(), true, TrackPanel.this);
			}

			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3)
					seq.setTrackSolo(trackInfo.getTrackNumber(), false, TrackPanel.this);
			}
		});

		add(checkBox, "0, 1");
//		add(octaveLabel, "1, 1, f, b");
		add(transposeSpinner, "1, 1, f, c");
		add(noteGraph, "2, 1");
	}

	public void dispose() {
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

	private class NoteGraph extends JPanel implements IDisposable, NoteGraphConstants {
		public NoteGraph() {
			abcPart.addChangeListener(myChangeListener);
			seq.addChangeListener(myChangeListener);

			MyMouseListener mouseListener = new MyMouseListener();
			addMouseListener(mouseListener);
			addMouseMotionListener(mouseListener);
			
			setPreferredSize(new Dimension(200, 24));
		}

		public void dispose() {
			abcPart.removeChangeListener(myChangeListener);
			seq.removeChangeListener(myChangeListener);
		}

		private MyChangeListener myChangeListener = new MyChangeListener();

		private class MyChangeListener implements ChangeListener, SequencerListener {
			public void stateChanged(ChangeEvent e) {
				repaint();
			}

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

			boolean trackEnabled = seq.isTrackActive(trackInfo.getTrackNumber());
			long songPos = seq.getPosition();
			int transpose = abcPart.getTrackTranspose(trackInfo.getTrackNumber()) + abcPart.getBaseTranspose();
			int minPlayable = abcPart.getInstrument().lowestPlayable.id;
			int maxPlayable = abcPart.getInstrument().highestPlayable.id;

			g2.setColor(trackEnabled && trackInfo.hasNotes() ? BORDER_COLOR : BORDER_DISABLED);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
			g2.setColor(trackEnabled && trackInfo.hasNotes() ? BKGD_COLOR : BKGD_DISABLED);
			g2.fillRoundRect(BORDER_SIZE, BORDER_SIZE, getWidth() - 2 * BORDER_SIZE, getHeight() - 2 * BORDER_SIZE, 5,
					5);

			g2.transform(xform);

			List<Rectangle2D> notesUnplayable = new ArrayList<Rectangle2D>();
			List<Rectangle2D> notesPlaying = null;
			if (trackEnabled && seq.isRunning())
				notesPlaying = new ArrayList<Rectangle2D>();

			// Paint the playable notes and keep track of the currently sounding and unplayable notes
			g2.setColor(trackEnabled ? NOTE : NOTE_DISABLED);

			Iterator<NoteEvent> noteEventIter = trackInfo.getNoteEvents().iterator();
			Iterator<NoteEvent> drumEventIter = trackInfo.getDrumEvents().iterator();

			while (noteEventIter.hasNext() || drumEventIter.hasNext()) {
				boolean drums;
				NoteEvent evt = (drums = drumEventIter.hasNext()) ? drumEventIter.next() : noteEventIter.next();

				int id = evt.note.id;
				if (!drums)
					id += transpose;
				double width = Math.max(minLength, evt.getLength());
				double y;
				boolean playable;

				if (id < minPlayable) {
					y = Math.max(id, MIN_RENDERED);
					playable = drums;
				}
				else if (id > maxPlayable) {
					y = Math.min(id, MAX_RENDERED);
					playable = drums;
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
					if (drums)
						g2.setColor(trackEnabled ? NOTE_DRUM : NOTE_DRUM_DISABLED);
					else
						g2.setColor(trackEnabled ? NOTE : NOTE_DISABLED);

					rectTmp.setRect(evt.startMicros, y, width, height);
					g2.fill(rectTmp);
				}
			}

			// Paint the unplayable notes above the playable ones
			g2.setColor(trackEnabled ? XNOTE : XNOTE_DISABLED);
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

			double scrnX = BORDER_SIZE;
			double scrnY = BORDER_SIZE + NOTE_HEIGHT_PX;
			double scrnW = getWidth() - 2 * BORDER_SIZE;
			double scrnH = getHeight() - 2 * BORDER_SIZE - NOTE_HEIGHT_PX;

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
					seq.setDragging(true, this);
					seq.setDragPosition(positionFromEvent(e), this);
				}
			}

			public void mouseDragged(MouseEvent e) {
				if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
					if (!isDragCanceled(e)) {
						seq.setDragging(true, this);
						seq.setDragPosition(positionFromEvent(e), this);
					}
					else {
						seq.setDragging(false, this);
					}
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					seq.setDragging(false, this);
					if (!isDragCanceled(e)) {
						seq.setPosition(positionFromEvent(e), this);
					}
				}
			}
		}
	}
}

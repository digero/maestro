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
import java.util.List;

import javax.sound.midi.Sequence;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.abc.LotroDrumInfo;
import com.digero.maestro.midi.MidiConstants;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequencerEvent;
import com.digero.maestro.midi.SequencerListener;
import com.digero.maestro.midi.SequencerWrapper;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.IDisposable;
import com.digero.maestro.util.Util;

@SuppressWarnings("serial")
public class DrumPanel extends JPanel implements IDisposable, TableLayoutConstants {
	//              0              1               2
	//   +--------------------+----------+--------------------+
	//   | TRACK NAME         | Drum     |  +--------------+  |
	// 0 |                    | +------+ |  | (note graph) |  |
	//   | Instrument(s)      | +-----v+ |  +--------------+  |
	//   +--------------------+----------+--------------------+
	private static final int HGAP = 4;
	private static final int TITLE_WIDTH = TrackPanel.TITLE_WIDTH - 52;
	private static final int COMBO_WIDTH = TrackPanel.SPINNER_WIDTH + 52;
	private static final double[] LAYOUT_COLS = new double[] {
			TITLE_WIDTH, COMBO_WIDTH, FILL, 1
	};
	private static final double[] LAYOUT_ROWS = new double[] {
			2, PREFERRED, 2
	};

	private static final Color BACKGROUND = new Color(224, 224, 224);

	private TrackInfo trackInfo;
	private SequencerWrapper seq;
	private AbcPart abcPart;
	private int drumId;

	private JCheckBox checkBox;
	private JComboBox drumComboBox;
	private NoteGraph noteGraph;

	public DrumPanel(TrackInfo info, SequencerWrapper sequencer, AbcPart part, int drumNoteId) {
		super(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));
		setBackground(BACKGROUND);
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BACKGROUND));

		this.trackInfo = info;
		this.seq = sequencer;
		this.abcPart = part;
		this.drumId = drumNoteId;

		TableLayout tableLayout = (TableLayout) getLayout();
		tableLayout.setHGap(HGAP);

		checkBox = new JCheckBox();
		checkBox.setSelected(abcPart.isDrumEnabled(drumId));
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abcPart.setDrumEnabled(drumId, checkBox.isSelected());
			}
		});

		checkBox.setOpaque(false);
		checkBox.setFont(checkBox.getFont().deriveFont(Font.ITALIC));

		String title = trackInfo.getTrackNumber() + ". " + trackInfo.getName();
		String instr = MidiConstants.getDrumName(drumId);
		checkBox.setToolTipText("<html><b>" + title + "</b><br>" + instr + "</html>");

//		title = Util.ellipsis(title, TITLE_WIDTH, titleLabel.getFont().deriveFont(Font.BOLD));
		instr = Util.ellipsis(instr, TITLE_WIDTH, checkBox.getFont());
		checkBox.setText(instr);

		drumComboBox = new JComboBox(LotroDrumInfo.ALL_DRUMS.toArray());
		drumComboBox.setSelectedItem(getSelectedDrum());
		drumComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LotroDrumInfo selected = (LotroDrumInfo) drumComboBox.getSelectedItem();
				abcPart.setDrumMapping(drumId, selected.note.id);
			}
		});

		abcPart.addAbcListener(abcPartListener);

		noteGraph = new NoteGraph();
		noteGraph.setOpaque(false);
		noteGraph.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					seq.setTrackSolo(trackInfo.getTrackNumber(), true);
					seq.setDrumSolo(drumId, true);
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					seq.setTrackSolo(trackInfo.getTrackNumber(), false);
					seq.setDrumSolo(drumId, false);
				}
			}
		});

		add(checkBox, "0, 1");
		add(drumComboBox, "1, 1, f, c");
		add(noteGraph, "2, 1");
	}

	public void dispose() {
		noteGraph.dispose();
		abcPart.removeAbcListener(abcPartListener);
	}

	private AbcPartListener abcPartListener = new AbcPartListener() {
		public void abcPartChanged(AbcPartEvent e) {
			checkBox.setSelected(abcPart.isDrumEnabled(drumId));
			drumComboBox.setSelectedItem(getSelectedDrum());
		}
	};

	private LotroDrumInfo getSelectedDrum() {
		return LotroDrumInfo.getById(abcPart.getDrumMapping(drumId));
	}

	private class NoteGraph extends JPanel implements IDisposable, NoteGraphConstants {
		private MyChangeListener myChangeListener = new MyChangeListener();

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

		private class MyChangeListener implements AbcPartListener, SequencerListener {
			@Override
			public void propertyChanged(SequencerEvent evt) {
				repaint();
			}

			@Override
			public void abcPartChanged(AbcPartEvent e) {
				if (e.isPreviewRelated())
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
			double minLength = NOTE_WIDTH_PX * 0.5 / xform.getScaleX();
			double height = Math.abs(NOTE_HEIGHT_PX * 2.0 / xform.getScaleY());

			boolean trackEnabled = seq.isDrumActive(trackInfo.getTrackNumber(), drumId);
			long songPos = seq.getPosition();

			g2.setColor(trackEnabled ? BORDER_COLOR : BORDER_DISABLED);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
			g2.setColor(trackEnabled ? BKGD_COLOR : BKGD_DISABLED);
			g2.fillRoundRect(BORDER_SIZE, BORDER_SIZE, getWidth() - 2 * BORDER_SIZE, getHeight() - 2 * BORDER_SIZE, 5,
					5);

			g2.transform(xform);

			List<Rectangle2D> notesPlaying = null;
			if (trackEnabled && seq.isRunning())
				notesPlaying = new ArrayList<Rectangle2D>();

			// Paint the playable notes and keep track of the currently sounding and unplayable notes
			g2.setColor(trackEnabled ? NOTE_DRUM : NOTE_DRUM_DISABLED);

			for (NoteEvent evt : trackInfo.getDrumEvents()) {
				int id = evt.note.id;
				if (id != drumId)
					continue;

				double width = Math.max(minLength, evt.getLength());
				double y = 1;

				if (notesPlaying != null && songPos >= evt.startMicros && songPos <= evt.endMicros) {
					notesPlaying.add(new Rectangle2D.Double(evt.startMicros, y, width, height));
				}
				else {
					rectTmp.setRect(evt.startMicros, y, width, height);
					g2.fill(rectTmp);
				}
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
			lineTmp.setLine(thumbPos, 0, thumbPos, 2 + height);
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
			double noteY = 2; //MAX_RENDERED;
			double noteW = song.getMicrosecondLength();
			double noteH = 0 - 2; //MIN_RENDERED - MAX_RENDERED;

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

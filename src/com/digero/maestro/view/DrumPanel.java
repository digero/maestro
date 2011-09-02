package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.Note;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.Util;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartListener;
import com.digero.maestro.abc.LotroDrumInfo;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.NoteFilterSequencerWrapper;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.IDisposable;

@SuppressWarnings("serial")
public class DrumPanel extends JPanel implements IDisposable, TableLayoutConstants, TrackPanelConstants,
		ICompileConstants {
	//              0              1               2
	//   +--------------------+----------+--------------------+
	//   | TRACK NAME         | Drum     |  +--------------+  |
	// 0 |                    | +------+ |  | (note graph) |  |
	//   | Instrument(s)      | +-----v+ |  +--------------+  |
	//   +--------------------+----------+--------------------+
	private static final int HGAP = 4;
	private static final int TITLE_WIDTH = TrackPanel.TITLE_WIDTH_DRUMS - 78;
	private static final int COMBO_WIDTH = TrackPanel.SPINNER_WIDTH + 78;
	private static final double[] LAYOUT_COLS = new double[] {
			TITLE_WIDTH, COMBO_WIDTH, FILL
	};
	private static final double[] LAYOUT_ROWS = new double[] {
			0, PREFERRED, 0
	};

	private TrackInfo trackInfo;
	private NoteFilterSequencerWrapper seq;
	private AbcPart abcPart;
	private int drumId;

	private JCheckBox checkBox;
	private JComboBox drumComboBox;
	private NoteGraph noteGraph;

	public DrumPanel(TrackInfo info, NoteFilterSequencerWrapper sequencer, AbcPart part, int drumNoteId) {
		super(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));

		this.trackInfo = info;
		this.seq = sequencer;
		this.abcPart = part;
		this.drumId = drumNoteId;

		TableLayout tableLayout = (TableLayout) getLayout();
		tableLayout.setHGap(HGAP);

		checkBox = new JCheckBox();
		checkBox.setSelected(abcPart.isDrumEnabled(trackInfo.getTrackNumber(), drumId));
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abcPart.setDrumEnabled(trackInfo.getTrackNumber(), drumId, checkBox.isSelected());
			}
		});

		checkBox.setOpaque(false);
//		checkBox.setFont(checkBox.getFont().deriveFont(Font.ITALIC));

		String title = trackInfo.getTrackNumber() + ". " + trackInfo.getName();
		String instr;
		if (info.isDrumTrack())
			instr = MidiConstants.getDrumName(drumId);
		else {
			Note note = Note.fromId(drumNoteId);
			instr = note.getDisplayName() + " (" + note.abc + ")";
		}

		checkBox.setToolTipText("<html><b>" + title + "</b><br>" + instr + "</html>");

		instr = Util.ellipsis(instr, TITLE_WIDTH, checkBox.getFont());
		checkBox.setText(instr);

		drumComboBox = new JComboBox(LotroDrumInfo.ALL_DRUMS.toArray());
		drumComboBox.setSelectedItem(getSelectedDrum());
		drumComboBox.setMaximumRowCount(20);
		drumComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LotroDrumInfo selected = (LotroDrumInfo) drumComboBox.getSelectedItem();
				abcPart.setDrumMapping(trackInfo.getTrackNumber(), drumId, selected.note.id);
			}
		});

		abcPart.addAbcListener(abcPartListener);

		noteGraph = new NoteGraph();
		noteGraph.setOpaque(false);
		noteGraph.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					seq.setNoteSolo(trackInfo.getTrackNumber(), drumId, true);
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					seq.setNoteSolo(trackInfo.getTrackNumber(), drumId, false);
				}
			}
		});

		addPropertyChangeListener("enabled", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				updateState();
			}
		});

		add(checkBox, "0, 1");
		add(drumComboBox, "1, 1, f, c");
		add(noteGraph, "2, 1");

		updateState();
	}

	public void dispose() {
		noteGraph.dispose();
		abcPart.removeAbcListener(abcPartListener);
	}

	private AbcPartListener abcPartListener = new AbcPartListener() {
		public void abcPartChanged(AbcPartEvent e) {
			checkBox.setEnabled(abcPart.isTrackEnabled(trackInfo.getTrackNumber()));
			checkBox.setSelected(abcPart.isDrumEnabled(trackInfo.getTrackNumber(), drumId));
			drumComboBox.setSelectedItem(getSelectedDrum());
			updateState();
		}
	};

	private void updateState() {
		boolean trackEnabled = abcPart.isTrackEnabled(trackInfo.getTrackNumber());
		boolean drumEnabled = abcPart.isDrumEnabled(trackInfo.getTrackNumber(), drumId);
		boolean enabled = trackEnabled && drumEnabled;
		setBackground(enabled ? ColorTable.PANEL_BACKGROUND_ENABLED.get() : ColorTable.PANEL_BACKGROUND_DISABLED.get());
		checkBox.setForeground(enabled ? ColorTable.PANEL_TEXT_ENABLED.get()
				: (trackEnabled ? ColorTable.PANEL_TEXT_DISABLED.get() : ColorTable.PANEL_TEXT_OFF.get()));
		checkBox.setEnabled(trackEnabled);
		drumComboBox.setEnabled(trackEnabled);
		drumComboBox.setVisible(abcPart.getInstrument() == LotroInstrument.DRUMS);
	}

	private LotroDrumInfo getSelectedDrum() {
		return LotroDrumInfo.getById(abcPart.getDrumMapping(trackInfo.getTrackNumber(), drumId));
	}

	private class NoteGraph extends JPanel implements IDisposable, TrackPanelConstants {
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

			Color drumColor;
			Color borderColor;
			Color bkgdColor;

			List<Rectangle2D> notesPlaying = null;

			int trackNumber = trackInfo.getTrackNumber();
			if (seq.isNoteActive(trackNumber, drumId)) {
				boolean playable = abcPart.isDrumPlayable(trackNumber, drumId);
				if (abcPart.isTrackEnabled(trackNumber) && abcPart.isDrumEnabled(trackNumber, drumId)) {
					drumColor = playable ? ColorTable.NOTE_DRUM_ENABLED.get() : ColorTable.NOTE_BAD_ENABLED.get();
					borderColor = ColorTable.GRAPH_BORDER_ENABLED.get();
					bkgdColor = ColorTable.GRAPH_BACKGROUND_ENABLED.get();
				}
				else {
					drumColor = playable ? ColorTable.NOTE_DRUM_DISABLED.get() : ColorTable.NOTE_BAD_DISABLED.get();
					borderColor = ColorTable.GRAPH_BORDER_DISABLED.get();
					bkgdColor = ColorTable.GRAPH_BACKGROUND_DISABLED.get();
				}

				// Drum parts don't really need to highlight the notes playing
				if (!trackInfo.isDrumTrack() && seq.isRunning())
					notesPlaying = new ArrayList<Rectangle2D>();
			}
			else {
				drumColor = ColorTable.NOTE_DRUM_OFF.get();
				borderColor = ColorTable.GRAPH_BORDER_OFF.get();
				bkgdColor = ColorTable.GRAPH_BACKGROUND_OFF.get();
			}
			long songPos = seq.getPosition();

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
			g2.setColor(drumColor);

			for (NoteEvent evt : trackInfo.getEvents()) {
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
				g2.setColor(ColorTable.NOTE_ON.get());
				for (Rectangle2D rect : notesPlaying) {
					g2.fill(rect);
				}
			}

			// Draw the indicator line
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setColor(ColorTable.INDICATOR_COLOR.get());
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

			double scrnX = GRAPH_BORDER_SIZE;
			double scrnY = GRAPH_BORDER_SIZE + NOTE_HEIGHT_PX;
			double scrnW = getWidth() - 2 * GRAPH_BORDER_SIZE;
			double scrnH = getHeight() - 2 * GRAPH_BORDER_SIZE - NOTE_HEIGHT_PX;

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

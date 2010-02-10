package com.digero.maestro.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.sound.midi.Sequence;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.maestro.midi.Note;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequencerEvent;
import com.digero.maestro.midi.SequencerListener;
import com.digero.maestro.midi.SequencerWrapper;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.project.AbcPart;

@SuppressWarnings("serial")
public class NoteGraphXXX extends JPanel {
	private static final int BORDER_SIZE = 1;
	private static final double NOTE_WIDTH_PX = 4;
	private static final double NOTE_HEIGHT_PX = 2;

	private static final Color NOTE = new Color(29, 95, 255);
	private static final Color XNOTE = Color.RED;
	private static final Color NOTE_ON = Color.WHITE;
	private static final Color NOTE_DISABLED = Color.GRAY;
	private static final Color XNOTE_DISABLED = Color.LIGHT_GRAY;
	private static final Color INDICATOR_COLOR = new Color(0x66FFFFFF, true);

	private static final int MIN_RENDERED = Note.C2.id - 12;
	private static final int MAX_RENDERED = Note.C5.id + 12;

	private TrackInfo trackInfo;
	private SequencerWrapper seq;
	private AbcPart abcPart;

	public NoteGraphXXX(TrackInfo info, SequencerWrapper sequencer, AbcPart part) {
		this.trackInfo = info;
		this.seq = sequencer;
		this.abcPart = part;

		part.addChangeListener(myChangeListener);
		part.getProject().addChangeListener(myChangeListener);
		seq.addChangeListener(myChangeListener);

		setPreferredSize(new Dimension(300, 24));
	}

	private class MyChangeListener implements ChangeListener, SequencerListener {
		public void stateChanged(ChangeEvent e) {
			repaint();
		}

		public void propertyChanged(SequencerEvent evt) {
			repaint();
		}
	}

	private MyChangeListener myChangeListener = new MyChangeListener();

	private Rectangle2D.Double rectTmp = new Rectangle2D.Double();
	private Line2D.Double lineTmp = new Line2D.Double();

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Sequence song = trackInfo.getSequenceInfo().getSequence();
		if (song == null)
			return;

		Graphics2D g2 = (Graphics2D) g;
		AffineTransform xform = getTransform();
		double minLength = NOTE_WIDTH_PX / xform.getScaleX();
		double height = Math.abs(NOTE_HEIGHT_PX / xform.getScaleY());

		boolean trackEnabled = seq.isTrackActive(trackInfo.getTrackNumber());
		long songPos = seq.getPosition();
		int transpose = abcPart.getTranspose(trackInfo.getTrackNumber());
		int minPlayable = abcPart.getInstrument().lowestPlayable.id;
		int maxPlayable = abcPart.getInstrument().highestPlayable.id;

		if (!trackEnabled) {
			g2.setColor(Color.DARK_GRAY);
			g2.fillRect(BORDER_SIZE, BORDER_SIZE, getWidth() - 2 * BORDER_SIZE, getHeight() - 2 * BORDER_SIZE);
		}

		g2.transform(xform);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		ArrayList<Rectangle2D> notesPlaying = new ArrayList<Rectangle2D>();

		// Paint the playable notes and keep track of the currently sounding and unplayable notes
		for (NoteEvent evt : trackInfo.getNoteEvents()) {
			int id = evt.note.id + transpose;
			double width = Math.max(minLength, evt.getLength());
			double y;

			if (id < minPlayable) {
				y = Math.max(id - height, MIN_RENDERED);
				g2.setColor(trackEnabled ? XNOTE : XNOTE_DISABLED);
			}
			else if (id > maxPlayable) {
				y = Math.min(id + height, MAX_RENDERED);
				g2.setColor(trackEnabled ? XNOTE : XNOTE_DISABLED);
			}
			else {
				y = id;
				g2.setColor(trackEnabled ? NOTE : NOTE_DISABLED);
			}

			if (trackEnabled && seq.isRunning() && songPos >= evt.startMicros && songPos <= evt.endMicros) {
				notesPlaying.add(new Rectangle2D.Double(evt.startMicros, y, width, height));
			}
			else {
				rectTmp.setRect(evt.startMicros, y, width, height);
				g2.fill(rectTmp);
			}
		}

		// Paint the currently playing notes last
		g2.setColor(NOTE_ON);
		for (Rectangle2D rect : notesPlaying) {
			g2.fill(rect);
		}

		// Draw the indicator line
		if (seq.isRunning()) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setColor(INDICATOR_COLOR);
			long thumbPos = seq.getThumbPosition();
			lineTmp.setLine(thumbPos, MIN_RENDERED, thumbPos, MAX_RENDERED);
			g2.draw(lineTmp);
		}
	}

	/**
	 * Gets a transform that converts song coordinates into screen coordinates.
	 */
	private AffineTransform getTransform() {
		Sequence song = trackInfo.getSequenceInfo().getSequence();
		if (song == null)
			return new AffineTransform();

		double scrnX = BORDER_SIZE;
		double scrnY = BORDER_SIZE;
		double scrnW = getWidth() - 2 * BORDER_SIZE;
		double scrnH = getHeight() - 2 * BORDER_SIZE;

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
}

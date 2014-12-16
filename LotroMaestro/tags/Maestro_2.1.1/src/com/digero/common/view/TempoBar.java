package com.digero.common.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerEvent.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.Listener;
import com.digero.common.util.Util;

public class TempoBar extends JPanel implements Listener<SequencerEvent>
{
	private static final int PTR_WIDTH = 12;
	private static final int PTR_HEIGHT = 12;
	private static final int BAR_HEIGHT = 6;
	private static final int SIDE_PAD = PTR_WIDTH / 2;
	private static final int ROUND = 6;

	public static final int WIDTH = PTR_WIDTH * 7;

	private static final float MID = 0.5f;
	private static final float MAX_TEMPO = 2.0f; // Must be at least 2.0

	private SequencerWrapper seq;
	private Rectangle ptrRect = new Rectangle(0, 0, PTR_WIDTH, PTR_HEIGHT);
	private boolean useInvertedColors;

	public TempoBar(SequencerWrapper seq)
	{
		this.seq = seq;

		MouseHandler mouseHandler = new MouseHandler();
		addMouseListener(mouseHandler);
		addMouseMotionListener(mouseHandler);

		Dimension sz = new Dimension(WIDTH, PTR_HEIGHT);
		setMinimumSize(sz);
		setPreferredSize(sz);
		updatePointerRect();

		seq.addChangeListener(this);
	}

	private float tempoToPct(float tempo)
	{
		assert tempo >= 0;

		if (tempo <= 1)
			return MID * tempo;

		if (tempo > MAX_TEMPO)
			tempo = MAX_TEMPO;

		return MID + (1 - MID) * (tempo - 1) / (MAX_TEMPO - 1);
	}

	private float pctToTempo(float pct)
	{
		assert pct >= 0 && pct <= 1;

		if (pct <= MID)
			return pct / MID;

		return (pct - MID) / (1 - MID) * (MAX_TEMPO - 1) + 1;
	}

	@Override protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int ptrPos = (int) (SIDE_PAD + (getWidth() - 2 * SIDE_PAD) * tempoToPct(seq.getTempoFactor()));

		final int x = 0;
		final int y = (PTR_HEIGHT - BAR_HEIGHT) / 2;
		int right = getWidth();

		float tempo = seq.getTempoFactor();
		Color fill, fillDark, fillBright;
		if (tempo <= 1.0f)
		{
			float sat = 1 - tempo;
			fill = Color.getHSBColor(0.0f, sat, 0.50f);
			fillDark = Color.getHSBColor(0.0f, sat, 0.25f);
			fillBright = Color.getHSBColor(0.0f, sat, 0.75f);
		}
		else
		{
			float sat = 1 - (MAX_TEMPO - tempo) / (MAX_TEMPO - 1);
			fill = Color.getHSBColor(0.33f, sat, 0.50f + 0.25f * sat);
			fillDark = Color.getHSBColor(0.33f, sat, 0.25f + 0.25f * sat);
			fillBright = Color.getHSBColor(0.33f, sat, 0.75f + 0.25f * sat);
		}

		Color fillA = useInvertedColors ? Color.WHITE : fillDark;
		Color fillB = useInvertedColors ? fillBright : fill;
		Color bkgdA = useInvertedColors ? fillDark : fillBright;
		Color bkgdB = useInvertedColors ? fill : Color.WHITE;

		g2.setClip(new RoundRectangle2D.Float(x, y, right - x, BAR_HEIGHT, ROUND, ROUND));
		g2.setPaint(new GradientPaint(0, y, fillA, 0, y + BAR_HEIGHT, fillB));
		g2.fillRect(x, y, ptrPos - x, BAR_HEIGHT);

		g2.setPaint(new GradientPaint(0, y, bkgdA, 0, y + BAR_HEIGHT, bkgdB));
		g2.fillRect(ptrPos, y, right - ptrPos, BAR_HEIGHT);
		g2.setClip(null);

		g2.setColor(Color.BLACK);
		g2.drawRoundRect(x, y, right - x - 1, BAR_HEIGHT, ROUND, ROUND);

		// Pointer
		int left = ptrPos - PTR_WIDTH / 2;

		final Color PTR_COLOR_1 = Color.WHITE;
		final Color PTR_COLOR_2 = Color.LIGHT_GRAY;

		g2.setPaint(new GradientPaint(left, 0, PTR_COLOR_1, left + PTR_WIDTH, 0, PTR_COLOR_2));
		g2.fillOval(left, 0, PTR_WIDTH - 1, PTR_HEIGHT - 1);
		g2.setColor(Color.BLACK);
		g2.drawOval(left, 0, PTR_WIDTH - 1, PTR_HEIGHT - 1);
	}

	private void updatePointerRect()
	{
		ptrRect.x = (int) (getWidth() * tempoToPct(seq.getTempoFactor()) - PTR_WIDTH / 2);
	}

	private class MouseHandler implements MouseListener, MouseMotionListener
	{
		private boolean draggingButton1 = false;

		private float getTempo(int x)
		{
			float pct = ((float) x) / (getWidth());
			pct = Util.clamp(pct, 0.0f, 1.0f);
			float tempo = pctToTempo(pct);
			if (tempo <= 0.05f)
				tempo = 0.001f;
			else if (tempo > 0.9f && tempo < 1.1f)
				tempo = 1.0f;
			else if (tempo <= 1.0f)
				tempo = Math.round(tempo / 0.10f) * 0.10f;
			else
				tempo = Math.round(tempo / 0.10f) * 0.10f;

			return tempo;
		}

		@Override public void mouseClicked(MouseEvent e)
		{
		}

		@Override public void mousePressed(MouseEvent e)
		{
			if (!TempoBar.this.isEnabled())
				return;
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				draggingButton1 = true;
				seq.setTempoFactor(getTempo(e.getX()));
				repaint();
				requestFocus();
			}
			if (e.getButton() == MouseEvent.BUTTON3)
			{
				if (seq.getTempoFactor() == 1.0f)
					seq.setTempoFactor(0.001f);
				else
					seq.setTempoFactor(1.0f);
			}
		}

		@Override public void mouseReleased(MouseEvent e)
		{
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				draggingButton1 = false;
			}
		}

		@Override public void mouseDragged(MouseEvent e)
		{
			if (!TempoBar.this.isEnabled())
				return;
			if (draggingButton1)
			{
				seq.setTempoFactor(getTempo(e.getX()));
				repaint();
			}
		}

		@Override public void mouseMoved(MouseEvent e)
		{
		}

		@Override public void mouseEntered(MouseEvent e)
		{
		}

		@Override public void mouseExited(MouseEvent e)
		{
		}
	}

	@Override public void onEvent(SequencerEvent evt)
	{
		if (evt.getProperty() == SequencerProperty.TEMPO)
		{
			repaint();
		}
	}

	public void setUseInvertedColors(boolean useInvertedColors)
	{
		this.useInvertedColors = useInvertedColors;
	}

	public boolean isUseInvertedColors()
	{
		return useInvertedColors;
	}
}

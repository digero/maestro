/* Copyright (c) 2008 Ben Howell
 * This software is licensed under the MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package com.digero.common.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
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

@SuppressWarnings("serial")
public class SongPositionBar extends JPanel implements Listener<SequencerEvent>
{
	private static final int PTR_WIDTH = 12;
	private static final int PTR_HEIGHT = 12;
	private static final int BAR_HEIGHT = 8;
	public static final int SIDE_PAD = PTR_WIDTH / 2;
	private static final int ROUND = 8;

	private SequencerWrapper seq;
	private boolean mouseHovering = false;

	private boolean useInvertedColors = false;

	private Rectangle ptrRect = new Rectangle(0, 0, PTR_WIDTH, PTR_HEIGHT);

	public SongPositionBar(SequencerWrapper sequencer)
	{
		setSequence(sequencer);

		MouseHandler mouseHandler = new MouseHandler();
		addMouseListener(mouseHandler);
		addMouseMotionListener(mouseHandler);

		Dimension sz = new Dimension(100, PTR_HEIGHT);
		setMinimumSize(sz);
		setPreferredSize(sz);
		updatePointerRect();
	}

	public void setSequence(SequencerWrapper sequencer)
	{
		if (this.seq != null)
			this.seq.removeChangeListener(this);

		this.seq = sequencer;

		if (this.seq != null)
			this.seq.addChangeListener(this);

		setEnabled(this.seq != null && this.seq.isLoaded());
	}

	@Override protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int ptrPos;
		if (seq == null || seq.getLength() == 0)
		{
			ptrPos = 0;
		}
		else
		{
			ptrPos = (int) (SIDE_PAD + (getWidth() - 2 * SIDE_PAD) * seq.getThumbPosition() / seq.getLength());
		}

		final int x = SIDE_PAD;
		final int y = (PTR_HEIGHT - BAR_HEIGHT) / 2;
		int right = getWidth() - SIDE_PAD;

		Color fillA = useInvertedColors ? Color.WHITE : Color.DARK_GRAY;
		Color fillB = useInvertedColors ? Color.LIGHT_GRAY : Color.GRAY;
		Color bkgdA = useInvertedColors ? Color.DARK_GRAY : Color.LIGHT_GRAY;
		Color bkgdB = useInvertedColors ? Color.GRAY : Color.WHITE;

		g2.setClip(new RoundRectangle2D.Float(x, y, right - x, BAR_HEIGHT, ROUND, ROUND));
		g2.setPaint(new GradientPaint(0, y, fillA, 0, y + BAR_HEIGHT, fillB));
		g2.fillRect(x, y, ptrPos - x, BAR_HEIGHT);

		g2.setPaint(new GradientPaint(0, y, bkgdA, 0, y + BAR_HEIGHT, bkgdB));
		g2.fillRect(ptrPos, y, right - ptrPos, BAR_HEIGHT);

		g2.setColor(useInvertedColors ? new Color(0xAA000000, true) : new Color(0xAAFFFFFF, true));
		g2.drawLine(ptrPos - 1, y, ptrPos - 1, y + BAR_HEIGHT);

		g2.setClip(null);

		g2.setColor(Color.BLACK);
		g2.drawRoundRect(x, y, right - x - 1, BAR_HEIGHT, ROUND, ROUND);

		if ((mouseHovering || seq.isDragging()) && this.isEnabled())
		{
			int left = ptrPos - PTR_WIDTH / 2;

			final Color PTR_COLOR_1 = Color.WHITE;
			final Color PTR_COLOR_2 = Color.LIGHT_GRAY;

			g2.setPaint(new GradientPaint(left, 0, PTR_COLOR_1, left + PTR_WIDTH, 0, PTR_COLOR_2));
			g2.fillOval(left, 0, PTR_WIDTH - 1, PTR_HEIGHT - 1);
			g2.setClip(null);
			g2.setColor(Color.BLACK);
			g2.drawOval(left, 0, PTR_WIDTH - 1, PTR_HEIGHT - 1);
		}
	}

	public void setUseInvertedColors(boolean usingInvertedColors)
	{
		this.useInvertedColors = usingInvertedColors;
	}

	public boolean isUseInvertedColors()
	{
		return useInvertedColors;
	}

	private void updatePointerRect()
	{
		if (seq == null || seq.getLength() == 0)
		{
			ptrRect.x = 0;
		}
		else
		{
			ptrRect.x = (int) (getWidth() * seq.getThumbPosition() / seq.getLength() - PTR_WIDTH / 2);
		}
	}

	private class MouseHandler implements MouseListener, MouseMotionListener
	{
		private static final int MAX_MOUSE_DIST = 100;

		private long getPosition(int x)
		{
			if (seq == null)
				return 0;

			long pos = (x + 1 - SIDE_PAD) * seq.getLength() / (getWidth() - 2 * SIDE_PAD);
			return Util.clamp(pos, 0, seq.getLength() - 1);
		}

		private void setMouseHovering(MouseEvent e)
		{
			Point pt = e.getPoint();

			boolean inside = pt.x >= 0 && pt.x < getWidth() && pt.y >= 0 && pt.y < getHeight();
			boolean newMouseHovering = SongPositionBar.this.isEnabled() && (seq.isDragging() || inside);

			if (newMouseHovering != mouseHovering)
			{
				mouseHovering = newMouseHovering;
				repaint();
			}
		}

		@Override public void mouseClicked(MouseEvent e)
		{
		}

		@Override public void mousePressed(MouseEvent e)
		{
			if (!SongPositionBar.this.isEnabled())
				return;
			seq.setDragging(true);
			seq.setDragPosition(getPosition(e.getX()));
			setMouseHovering(e);
			requestFocus();
		}

		@Override public void mouseReleased(MouseEvent e)
		{
			if (!SongPositionBar.this.isEnabled())
				return;
			seq.setDragging(false);
			if (e.getY() > -MAX_MOUSE_DIST && e.getY() < getHeight() + MAX_MOUSE_DIST)
			{
				seq.setPosition(getPosition(e.getX()));
			}
			setMouseHovering(e);
		}

		@Override public void mouseDragged(MouseEvent e)
		{
			if (!SongPositionBar.this.isEnabled())
				return;
			if (e.getY() > -MAX_MOUSE_DIST && e.getY() < getHeight() + MAX_MOUSE_DIST)
			{
				seq.setDragging(true);
				seq.setDragPosition(getPosition(e.getX()));
			}
			else
			{
				seq.setDragging(false);
			}
			setMouseHovering(e);
		}

		@Override public void mouseMoved(MouseEvent e)
		{
			if (!SongPositionBar.this.isEnabled())
				return;
			setMouseHovering(e);
		}

		@Override public void mouseEntered(MouseEvent e)
		{
		}

		@Override public void mouseExited(MouseEvent e)
		{
			if (mouseHovering)
			{
				mouseHovering = false;
				repaint();
			}
		}
	}

	@Override public void onEvent(SequencerEvent evt)
	{
		if (evt.getProperty() == SequencerProperty.IS_LOADED)
			setEnabled(evt.getSource().isLoaded());

		repaint();
	}
}

package com.digero.common.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

import com.digero.common.midi.VolumeTransceiver;

public class VolumeBar extends JPanel
{
	private static final int PTR_WIDTH = 12;
	private static final int PTR_HEIGHT = 12;
	private static final int BAR_HEIGHT = 6;
	private static final int SIDE_PAD = PTR_WIDTH / 2;
	private static final int ROUND = 6;

	public static final int WIDTH = PTR_WIDTH * 5;

	private VolumeTransceiver volumizer;
	private boolean useInvertedColors;

	public VolumeBar(VolumeTransceiver volumizer)
	{
		this.volumizer = volumizer;

		MouseHandler mouseHandler = new MouseHandler();
		addMouseListener(mouseHandler);
		addMouseMotionListener(mouseHandler);

		Dimension sz = new Dimension(WIDTH, PTR_HEIGHT);
		setMinimumSize(sz);
		setPreferredSize(sz);
	}

	@Override protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int ptrPos = SIDE_PAD + (getWidth() - 2 * SIDE_PAD) * volumizer.getVolume() / VolumeTransceiver.MAX_VOLUME;

		final int x = 0;
		final int y = (PTR_HEIGHT - BAR_HEIGHT) / 2;
		int right = getWidth();

		Color fillA = useInvertedColors ? Color.WHITE : Color.DARK_GRAY;
		Color fillB = useInvertedColors ? Color.LIGHT_GRAY : Color.GRAY;
		Color bkgdA = useInvertedColors ? Color.DARK_GRAY : Color.LIGHT_GRAY;
		Color bkgdB = useInvertedColors ? Color.GRAY : Color.WHITE;

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

	public void setUseInvertedColors(boolean useInvertedColors)
	{
		this.useInvertedColors = useInvertedColors;
	}

	public boolean isUseInvertedColors()
	{
		return useInvertedColors;
	}

	private class MouseHandler implements MouseListener, MouseMotionListener
	{
		private int getPosition(int x)
		{
			int pos = (x + 1 - SIDE_PAD) * VolumeTransceiver.MAX_VOLUME / (getWidth() - 2 * SIDE_PAD);
			if (pos < 0)
			{
				pos = 0;
			}
			if (pos > VolumeTransceiver.MAX_VOLUME)
			{
				pos = VolumeTransceiver.MAX_VOLUME;
			}
			return pos;
		}

		@Override public void mouseClicked(MouseEvent e)
		{
		}

		@Override public void mousePressed(MouseEvent e)
		{
			if (!VolumeBar.this.isEnabled())
				return;
			volumizer.setVolume(getPosition(e.getX()));
			repaint();
			requestFocus();
		}

		@Override public void mouseReleased(MouseEvent e)
		{
		}

		@Override public void mouseDragged(MouseEvent e)
		{
			if (!VolumeBar.this.isEnabled())
				return;
			volumizer.setVolume(getPosition(e.getX()));
			repaint();
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
}

package com.digero.maestro.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.digero.common.abc.Dynamics;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Util;

public class TrackVolumeBar extends JPanel implements IDiscardable
{
	private static final int PTR_WIDTH = 6;
	private static final int PTR_HEIGHT = 16;
	private static final int BAR_HEIGHT = 10;
	private static final int SIDE_PAD = PTR_WIDTH / 2;
	private static final int ROUND = 0;

	private static final int DRAG_MARGIN_X = 512;
	private static final int DRAG_MARGIN_Y = 32;

	public static final int WIDTH = PTR_WIDTH * 5;

	private List<ActionListener> actionListeners;

	private static final int VALUE_GUTTER = 16;
	private static final int STEP_SIZE = 16;
	private static final int DEFAULT_VALUE = 0;
	private final int MIN_VALUE;
	private final int MAX_VALUE;
	private int value;

	// Visual properties
	private boolean mouseWithin = false;
	private boolean mouseDown = false;

	public TrackVolumeBar(int trackMinVelocity, int trackMaxVelocity)
	{
		MIN_VALUE = Dynamics.MINIMUM.midiVol - trackMaxVelocity;
		MAX_VALUE = Dynamics.MAXIMUM.midiVol - trackMinVelocity;

		InputHandler inputHandler = new InputHandler();
		addMouseListener(inputHandler);
		addMouseMotionListener(inputHandler);
		addKeyListener(inputHandler);

		Dimension sz = new Dimension(WIDTH, PTR_HEIGHT);
		setMinimumSize(sz);
		setPreferredSize(sz);
	}

	@Override public void discard()
	{
		if (actionListeners != null)
		{
			actionListeners.clear();
			actionListeners = null;
		}
	}

	public int getDeltaVolume()
	{
		return value;
	}

	public void setDeltaVolume(int deltaVolume)
	{
		if (deltaVolume != value)
		{
			value = Util.clamp(deltaVolume, MIN_VALUE, MAX_VALUE);
			fireActionEvent();
		}
	}

	public boolean isDragging()
	{
		return mouseDown;
	}

	public void addActionListener(ActionListener trackVolumeListener)
	{
		if (actionListeners == null)
			actionListeners = new ArrayList<ActionListener>();

		if (!actionListeners.contains(trackVolumeListener))
			actionListeners.add(trackVolumeListener);
	}

	public void removeActionListener(ActionListener trackVolumeListener)
	{
		if (actionListeners != null)
			actionListeners.remove(trackVolumeListener);
	}

	protected void fireActionEvent()
	{
		if (actionListeners != null)
		{
			ActionEvent e = new ActionEvent(this, 0, null);
			for (ActionListener l : actionListeners)
				l.actionPerformed(e);
		}
	}

	@Override protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int vMin = MIN_VALUE;
		int vMax = MAX_VALUE;
		int vCtr = DEFAULT_VALUE;

		int ptrPos = SIDE_PAD + (getWidth() - 2 * SIDE_PAD) * (value - vMin) / (vMax - vMin);
		int ctrPos = SIDE_PAD + (getWidth() - 2 * SIDE_PAD) * (vCtr - vMin) / (vMax - vMin);
		ctrPos = ctrPos + PTR_WIDTH * (2 * ctrPos - getWidth()) / (2 * getWidth());

		int fillStart = 0;
		int fillWidth = ptrPos + PTR_WIDTH * (2 * ptrPos - getWidth()) / (2 * getWidth());

		final int x = 0;
		final int y = (PTR_HEIGHT - BAR_HEIGHT) / 2;
		int right = getWidth();

		Color fillA = isEnabled() ? Color.WHITE : Color.LIGHT_GRAY;
		Color fillB = isEnabled() ? Color.LIGHT_GRAY : Color.GRAY;
		Color bkgdA = Color.DARK_GRAY;
		Color bkgdB = Color.GRAY;

		g2.setClip(new RoundRectangle2D.Float(x, y, right - x, BAR_HEIGHT, ROUND, ROUND));

		// Background
		g2.setPaint(new GradientPaint(0, y, bkgdA, 0, y + BAR_HEIGHT, bkgdB));
		g2.fillRect(x, y, right - x - 1, BAR_HEIGHT);

		// Fill
		g2.setPaint(new GradientPaint(0, y, fillA, 0, y + BAR_HEIGHT, fillB));
		g2.fillRect(fillStart, y, fillWidth, BAR_HEIGHT);

		g2.setClip(null);

		// Center indicator
		g2.setColor(Color.BLACK);
		g2.fillRect(ctrPos, y, 1, BAR_HEIGHT);

		// Border
		g2.setColor(Color.BLACK);
		g2.drawRoundRect(x, y, right - x - 1, BAR_HEIGHT, ROUND, ROUND);

		// Pointer
		if (mouseDown || mouseWithin)
		{
			int left = ptrPos - PTR_WIDTH / 2;

			final Color PTR_COLOR_1 = Color.WHITE;
			final Color PTR_COLOR_2 = Color.LIGHT_GRAY;

			g2.setPaint(new GradientPaint(left, 0, PTR_COLOR_1, left + PTR_WIDTH, 0, PTR_COLOR_2));
			g2.fillRoundRect(left, 0, PTR_WIDTH - 1, PTR_HEIGHT - 1, ROUND, ROUND);
			g2.setColor(Color.BLACK);
			g2.drawRoundRect(left, 0, PTR_WIDTH - 1, PTR_HEIGHT - 1, ROUND, ROUND);
		}
	}

	private class InputHandler implements MouseListener, MouseMotionListener, KeyListener
	{
		private int deltaAtDragStart = value;

		private void handleDrag(MouseEvent e)
		{
			int x = e.getX();
			int y = e.getY();
			if ((y < -DRAG_MARGIN_Y || y > getHeight() + DRAG_MARGIN_Y)
					|| (x < -DRAG_MARGIN_X || x > getWidth() + DRAG_MARGIN_X))
			{
				// Cancel the drag
				value = deltaAtDragStart;
			}
			else
			{
				float xMin = SIDE_PAD;
				float xMax = getWidth() - 2 * SIDE_PAD;

				float v = (MAX_VALUE - MIN_VALUE) * (x - xMin) / xMax + MIN_VALUE;

				value = Util.clamp(Math.round(v / STEP_SIZE) * STEP_SIZE, MIN_VALUE, MAX_VALUE);

				if (value != MIN_VALUE && value != MAX_VALUE && Math.abs(value - DEFAULT_VALUE) < VALUE_GUTTER)
					value = DEFAULT_VALUE;
			}

			fireActionEvent();
			repaint();
		}

		private void endDrag(boolean success)
		{
			if (mouseDown)
			{
				mouseDown = false;
				if (!success)
					value = deltaAtDragStart;

				fireActionEvent();
				repaint();
			}
		}

		@Override public void mousePressed(MouseEvent e)
		{
			if (isEnabled() && e.getButton() == MouseEvent.BUTTON1)
			{
				mouseDown = true;
				deltaAtDragStart = value;
				handleDrag(e);
				requestFocus();
			}
		}

		@Override public void mouseDragged(MouseEvent e)
		{
			if (isEnabled() && mouseDown && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0)
				handleDrag(e);
		}

		@Override public void mouseReleased(MouseEvent e)
		{
			if (isEnabled() && e.getButton() == MouseEvent.BUTTON1)
				endDrag(true);
		}

		@Override public void mouseClicked(MouseEvent e)
		{
		}

		@Override public void mouseMoved(MouseEvent e)
		{
		}

		@Override public void mouseEntered(MouseEvent e)
		{
			if (isEnabled())
			{
				mouseWithin = true;
				repaint();
			}
		}

		@Override public void mouseExited(MouseEvent e)
		{
			if (mouseWithin)
			{
				mouseWithin = false;
				repaint();
			}
		}

		@Override public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				endDrag(false);
		}

		@Override public void keyReleased(KeyEvent e)
		{
		}

		@Override public void keyTyped(KeyEvent e)
		{
		}
	}
}

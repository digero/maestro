package com.digero.common.util;

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.event.ChangeListener;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

/**
 * A Caret implementation that does nothing. Useful for making a TextArea be readonly.
 */
public class NullCaret implements Caret
{
	@Override public void setVisible(boolean v)
	{
	}

	@Override public void setSelectionVisible(boolean v)
	{
	}

	@Override public void setMagicCaretPosition(Point p)
	{
	}

	@Override public void setDot(int dot)
	{
	}

	@Override public void setBlinkRate(int rate)
	{
	}

	@Override public void paint(Graphics g)
	{
	}

	@Override public void moveDot(int dot)
	{
	}

	@Override public boolean isVisible()
	{
		return false;
	}

	@Override public boolean isSelectionVisible()
	{
		return false;
	}

	@Override public void install(JTextComponent c)
	{
	}

	@Override public int getMark()
	{
		return 0;
	}

	@Override public Point getMagicCaretPosition()
	{
		return new Point(0, 0);
	}

	@Override public int getDot()
	{
		return 0;
	}

	@Override public int getBlinkRate()
	{
		return 0;
	}

	@Override public void deinstall(JTextComponent c)
	{
	}

	@Override public void addChangeListener(ChangeListener l)
	{
	}

	@Override public void removeChangeListener(ChangeListener l)
	{
	}
}

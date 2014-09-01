package com.digero.maestro.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;

/**
 * Returns a preferred size that's no larger than the specified max size, since TableLayout only
 * uses preferred size, ignoring maximum size.
 */
public class MaxSizeContainer extends JPanel
{
	public MaxSizeContainer(Component element, int maxWidth)
	{
		this(element, new Dimension(maxWidth, Integer.MAX_VALUE));
	}

	public MaxSizeContainer(Component element, int maxWidth, int maxHeight)
	{
		this(element, new Dimension(maxWidth, maxHeight));
	}

	public MaxSizeContainer(Component element, Dimension maximumSize)
	{
		super(new BorderLayout());
		add(element, BorderLayout.CENTER);
		setMaximumSize(maximumSize);
	}

	@Override public Dimension getPreferredSize()
	{
		Dimension max = super.getMaximumSize();
		Dimension pref = super.getPreferredSize();
		if (pref.width > max.width)
			pref.width = max.width;
		if (pref.height > max.height)
			pref.height = max.height;
		return pref;
	}
}

package com.digero.abcplayer.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;

import javax.swing.JPanel;

import com.digero.common.abctomidi.AbcRegion;

public class HighlightAbcNotesPanel extends JPanel
{
	private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 13);

	private List<String> lines = null;
	private NavigableSet<AbcRegion> regions = null;
	private NavigableMap<Integer, AbcRegion> indexToRegion = null;
	private Dimension size = null;

	public HighlightAbcNotesPanel()
	{
	}

	private void setLinesTmp(List<String> lines)
	{
		this.lines = lines;
		size = null;

		invalidate();
		repaint();
	}

	@Override public Dimension getPreferredSize()
	{
		if (size == null)
		{
			if (lines == null)
				return size = new Dimension(0, 0);

			FontMetrics metrics = getGraphics().getFontMetrics(FONT);

			int width = 0;
			for (String line : lines)
				width = Math.max(width, metrics.stringWidth(line));

			int height = metrics.getHeight() * lines.size();
			size = new Dimension(width, height);
		}

		return size;
	}

	public void setLinesAndRegions(List<String> lines, NavigableSet<AbcRegion> regions, boolean retainScrollPosition)
	{
		repaint();

		StringBuilder text = new StringBuilder();
		for (int i = 0; i < lines.size(); i++)
		{
			text.append(lines.get(i)).append("\r\n");
		}

		if (!retainScrollPosition)
			scrollRectToVisible(new Rectangle());

		this.regions = regions;
		this.indexToRegion = null;

		update();
	}

	@Override protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if (lines == null)
			return;

		Graphics2D g2 = (Graphics2D) g;
		g2.setFont(FONT);
		g2.setColor(getForeground());

		FontMetrics metrics = g2.getFontMetrics();
		Rectangle clipRect = g2.getClipBounds();
		int lineHeight = metrics.getHeight();
		int leadingAndAscent = metrics.getLeading() + metrics.getAscent();
		int iMin = clipRect.y / lineHeight;
		int iMax = Math.min(lines.size() - 1, (clipRect.y + clipRect.height) / lineHeight);
		for (int i = iMin; i <= iMax; i++)
		{
			GlyphVector glyphs = FONT.createGlyphVector(g2.getFontRenderContext(), lines.get(i));

			g2.drawString(lines.get(i), 0, i * lineHeight + leadingAndAscent);
		}

		System.out.println("Draw " + (iMax - iMin + 1) + " lines");
	}

	private void update()
	{
		// TODO
	}
}

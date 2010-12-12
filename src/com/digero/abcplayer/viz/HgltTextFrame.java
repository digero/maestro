package com.digero.abcplayer.viz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

public class HgltTextFrame extends JFrame {
	private Map<Region, Object> regions = new HashMap<Region, Object>();

	private DefaultHighlighter hglter;
	private DefaultHighlightPainter hgltPainter;

	private JTextArea textArea;
	private String text;

	public HgltTextFrame(String title, String text) {
		super(title);
		this.text = text;

		JPanel content = new JPanel(new BorderLayout());
		setContentPane(content);

		hglter = new DefaultHighlighter();
		hgltPainter = new DefaultHighlightPainter(Color.YELLOW);

		textArea = new JTextArea(text);
		textArea.setEditable(false);
		textArea.setLineWrap(false);
		textArea.setHighlighter(hglter);

		JScrollPane textAreaScrollPane = new JScrollPane(textArea);
		content.add(textAreaScrollPane, BorderLayout.CENTER);
	}

	public void setRegion(int p0, int p1) {
		Region rgn = new Region(p0, p1);
		if (!regions.containsKey(rgn)) {
			try {
				Object tag = hglter.addHighlight(rgn.getStart(), rgn.getEnd(), hgltPainter);
				regions.put(rgn, tag);
			}
			catch (BadLocationException e) {
				throw new InvalidParameterException();
			}
		}
	}

	public void setRegions(Iterable<Region> newRegions) {
		ArrayList x;
		for (Region r : newRegions) {
			
		}
	}

	public void removeRegion(int p0, int p1) {
		Object tag = regions.remove(new Region(p0, p1));
		if (tag != null) {
			hglter.removeHighlight(tag);
		}
	}

	public boolean isHighlighted(int p0, int p1) {
		if (p0 < p1) {
			int tmp = p0;
			p0 = p1;
			p1 = tmp;
		}

		return regions.containsKey(new Region(p0, p1));
	}
}

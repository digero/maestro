package com.digero.abcplayer.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

import com.digero.abcplayer.AbcPlayer;
import com.digero.common.abctomidi.AbcRegion;
import com.digero.common.midi.Note;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerEvent.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.Listener;
import com.digero.common.util.NullCaret;
import com.digero.common.util.Util;
import com.digero.common.view.ColorTable;

public class HighlightAbcNotesFrame extends JFrame
{
	private SequencerWrapper sequencer;
	private boolean scrollLock = true;

	private NavigableSet<AbcRegion> regions = new TreeSet<AbcRegion>();
	private NavigableMap<Integer, AbcRegion> indexToRegion = null;
	private Map<AbcRegion, Object> highlightedRegions = new HashMap<AbcRegion, Object>();
	private Map<AbcRegion, Object> highlightedTiedRegions = new HashMap<AbcRegion, Object>();
	private int[] lineStartIndex;

	private Highlighter highlighter;
	private Highlighter.HighlightPainter chordPainter;
	private Highlighter.HighlightPainter noteOnPainter;
	private Highlighter.HighlightPainter tiedNoteOnPainter;
	private Highlighter.HighlightPainter inactiveNoteOnPainter;
	private Highlighter.HighlightPainter inactiveTiedNoteOnPainter;
	private Highlighter.HighlightPainter restPainter;
	private Highlighter.HighlightPainter inactiveRestPainter;

	private JTextArea textArea;
	private JScrollPane textAreaScrollPane;

	private boolean updatePending = false;
	private boolean resetAllHighlightsNextUpdate = false;

	public HighlightAbcNotesFrame(SequencerWrapper seq)
	{
		super(AbcPlayer.APP_NAME);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override public void windowOpened(WindowEvent e)
			{
				update();
			}
		});

		this.sequencer = seq;
		sequencer.addChangeListener(new Listener<SequencerEvent>()
		{
			@Override public void onEvent(SequencerEvent evt)
			{
				SequencerProperty p = evt.getProperty();
				if (p.isInMask(SequencerProperty.TRACK_ACTIVE.mask | SequencerProperty.IS_RUNNING.mask
						| SequencerProperty.IS_DRAGGING.mask))
				{
					resetAllHighlightsNextUpdate = true;
					update();
				}
				else if (p.isInMask(SequencerProperty.THUMB_POSITION_MASK))
				{
					update();
				}
			}
		});

		JPanel content = new JPanel(new BorderLayout());
		setContentPane(content);

		highlighter = new DefaultHighlighter();

		Color noteOnColor = Color.getHSBColor(0.61f, 0.75f, 1.00f);
		Color inactiveNoteOnColor = Color.getHSBColor(0.62f, 0.00f, 0.45f);

		restPainter = new RestPainter(noteOnColor);
		inactiveRestPainter = new RestPainter(inactiveNoteOnColor);
		noteOnPainter = new DefaultHighlightPainter(noteOnColor);
		tiedNoteOnPainter = restPainter;
		inactiveNoteOnPainter = new DefaultHighlightPainter(inactiveNoteOnColor);
		inactiveTiedNoteOnPainter = inactiveRestPainter;
		chordPainter = new RestPainter(noteOnColor, 2, 2, 2, 2);

		textArea = new JTextArea();
		textArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
		textArea.setEditable(false);
		textArea.setLineWrap(false);
		textArea.setHighlighter(highlighter);
		textArea.setColumns(40);
		textArea.setRows(20);
		textArea.setCaret(new NullCaret());
		textArea.setBackground(ColorTable.GRAPH_BACKGROUND_DISABLED.get());
		textArea.setForeground(ColorTable.NOTE_ON.get());
		textArea.setDisabledTextColor(ColorTable.NOTE_ON.get());
		TextAreaInputListener textAreaListener = new TextAreaInputListener();
		textArea.addMouseListener(textAreaListener);
		textArea.addMouseMotionListener(textAreaListener);
		textArea.addKeyListener(textAreaListener);

		textAreaScrollPane = new JScrollPane(textArea);
		content.add(textAreaScrollPane, BorderLayout.CENTER);

		pack();

		Preferences prefs = Preferences.userNodeForPackage(HighlightAbcNotesFrame.class).node("HighlightAbcNotesFrame");
		Util.initWinBounds(this, prefs.node("window"), getWidth(), getHeight());
	}

	public void addDropListener(DropTargetListener listener)
	{
		new DropTarget(this, listener);
		new DropTarget(textArea, listener);
	}

	private static class RestPainter extends LayeredHighlighter.LayerPainter
	{
		private int left, top, right, bottom;
		private Color color;

		public RestPainter(Color color)
		{
			this(color, 0, 0, 0, 2);
		}

		public RestPainter(Color color, int left, int top, int right, int bottom)
		{
			this.color = color;
			setSize(left, top, right, bottom);
		}

		public void setSize(int left, int top, int right, int bottom)
		{
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}

		@Override public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c)
		{
			try
			{
				TextUI mapper = c.getUI();
				Rectangle p0 = mapper.modelToView(c, offs0);
				Rectangle p1 = mapper.modelToView(c, offs1);

				g.setColor(color);
				drawRect(g, p0.union(p1));
			}
			catch (BadLocationException e)
			{
			}
		}

		@Override public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view)
		{
			g.setColor(color);
			Rectangle r;

			if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset())
			{
				if (bounds instanceof Rectangle)
				{
					r = (Rectangle) bounds;
				}
				else
				{
					r = bounds.getBounds();
				}
			}
			else
			{
				try
				{
					Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
					r = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
				}
				catch (BadLocationException e)
				{
					r = null;
				}
			}

			if (r != null)
				drawRect(g, r);

			return r;
		}

		private void drawRect(Graphics g, Rectangle r)
		{
			int w = Math.max(r.width, 1);
			int h = Math.max(r.height, 1);

			if (left > 0)
				g.fillRect(r.x, r.y, left, h);

			if (top > 0)
				g.fillRect(r.x, r.y, w, top);

			if (right > 0)
				g.fillRect(r.x + w - right, r.y, right, h);

			if (bottom > 0)
				g.fillRect(r.x, r.y + h - bottom, w, bottom);
		}
	}

	private class TextAreaInputListener extends MouseAdapter implements KeyListener
	{
		private static final int DRAG_THRESHOLD = 25;
		private boolean dragActive = false;

		@Override public void mousePressed(MouseEvent e)
		{
			if (SwingUtilities.isLeftMouseButton(e))
			{
				handleMouseEvent(e.getPoint());
				dragActive = true;
			}
			else
			{
				endDrag(false);
			}
		}

		@Override public void mouseReleased(MouseEvent e)
		{
			if (SwingUtilities.isLeftMouseButton(e))
				endDrag(true);
		}

		@Override public void mouseDragged(MouseEvent e)
		{
			if (!dragActive || !SwingUtilities.isLeftMouseButton(e))
				return;

			Point pt = e.getPoint();
			if (pt.x < -DRAG_THRESHOLD || pt.y < -DRAG_THRESHOLD || pt.x > (textArea.getWidth() + DRAG_THRESHOLD)
					|| pt.y > (textArea.getHeight() + DRAG_THRESHOLD))
			{
				sequencer.setDragging(false);
			}
			else
			{
				handleMouseEvent(pt);
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

		private void endDrag(boolean commit)
		{
			if (commit && dragActive && sequencer.isDragging())
				sequencer.setTickPosition(sequencer.getDragTick());

			dragActive = false;
			sequencer.setDragging(false);
		}

		private void handleMouseEvent(Point pt)
		{
			if (indexToRegion == null)
			{
				indexToRegion = new TreeMap<Integer, AbcRegion>();
				for (AbcRegion region : regions)
				{
					indexToRegion.put(lineStartIndex[region.getLine()] + region.getEndIndex(), region);
				}
			}

			int index = textArea.viewToModel(pt);
			Map.Entry<Integer, AbcRegion> e = indexToRegion.ceilingEntry(index);
			if (!isValidEntry(e, pt, index))
			{
				e = indexToRegion.floorEntry(index);
				if (!isValidEntry(e, pt, index))
					e = null;
			}

			if (e != null)
			{
				sequencer.setDragging(true);
				sequencer.setDragTick(e.getValue().getStartTick());
			}
			else
			{
				sequencer.setDragging(false);
			}
		}

		private boolean isValidEntry(Map.Entry<Integer, AbcRegion> e, Point pt, int index)
		{
			try
			{
				if (e != null)
				{
					int line = e.getValue().getLine();
					Rectangle lineEndRect = textArea.modelToView(lineStartIndex[line + 1] - 1);
					int lineEndX = lineEndRect.x + lineEndRect.width;

					// Check if it's the same line
					if (index < lineStartIndex[line] || index >= lineStartIndex[line + 1])
						return false;

					// Check if it's past the end of the line
					if (pt.x > lineEndX + DRAG_THRESHOLD)
						return false;

					return true;
				}
			}
			catch (BadLocationException e1)
			{
				e1.printStackTrace();
			}

			return false;
		}
	}

	private void clearAllHighlights()
	{
		highlighter.removeAllHighlights();
		highlightedRegions.clear();
		highlightedTiedRegions.clear();
	}

	public void setLinesAndRegions(List<String> lines, NavigableSet<AbcRegion> regions, boolean retainScrollPosition)
	{
		clearAllHighlights();

		lineStartIndex = new int[lines.size() + 1];
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < lines.size(); i++)
		{
			lineStartIndex[i] = text.length();
			text.append(lines.get(i)).append("\r\n");
		}
		lineStartIndex[lines.size()] = text.length();
		textArea.setText(text.toString());

		if (!retainScrollPosition)
			textAreaScrollPane.getViewport().setViewPosition(new Point(0, 0));

		this.regions = regions;
		this.indexToRegion = null;

		update();
	}

	public boolean isScrollLock()
	{
		return scrollLock;
	}

	public void setScrollLock(boolean scrollLock)
	{
		this.scrollLock = scrollLock;
	}

	private boolean isRegionOn(AbcRegion region, long tick)
	{
		return (region.getStartTick() <= tick && region.getEndTick() > tick);
	}

	private Highlighter.HighlightPainter getPainter(AbcRegion region)
	{
		boolean active = sequencer.isTrackActive(region.getTrackNumber());
		if (region.getNote() == null)
			return chordPainter;
		if (region.getNote() == Note.REST /* || region.getNote() == null */)
			return active ? restPainter : inactiveRestPainter;
		if (region.getTiesFrom() == null)
			return active ? noteOnPainter : inactiveNoteOnPainter;
		else
			return active ? tiedNoteOnPainter : inactiveTiedNoteOnPainter;
	}

	private void update()
	{
		if (updatePending || !isVisible())
			return;

		updatePending = true;
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override public void run()
			{
				updatePending = false;
				updateCore();
			}
		});
	}

	private void updateCore()
	{
		if (resetAllHighlightsNextUpdate)
		{
			clearAllHighlights();
			resetAllHighlightsNextUpdate = false;
		}

		long tick = sequencer.getThumbTick();

		if (tick == 0 && !sequencer.isRunning() && !sequencer.isDragging())
		{
			clearAllHighlights();
			return;
		}

		// Remove regions that aren't highlighted anymore
		Iterator<Map.Entry<AbcRegion, Object>> highlightIter = highlightedRegions.entrySet().iterator();
		while (highlightIter.hasNext())
		{
			Map.Entry<AbcRegion, Object> e = highlightIter.next();
			AbcRegion region = e.getKey();
			if (!isRegionOn(region, tick))
			{
				highlightIter.remove();
				if (e.getValue() != null)
					highlighter.removeHighlight(e.getValue());

				for (AbcRegion t = region.getTiesFrom(); t != null; t = t.getTiesFrom())
				{
					Object tieTag = highlightedTiedRegions.remove(t);
					if (tieTag != null)
						highlighter.removeHighlight(tieTag);
				}
				for (AbcRegion t = region.getTiesTo(); t != null; t = t.getTiesTo())
				{
					Object tieTag = highlightedTiedRegions.remove(t);
					if (tieTag != null)
						highlighter.removeHighlight(tieTag);
				}
			}
		}

		try
		{
			// Add highlighted regions
			int maxEnd = -1;
			for (AbcRegion region : regions)
			{
				if (region.getStartTick() > tick)
					break;

				if (isRegionOn(region, tick))
				{
					int lineStart = lineStartIndex[region.getLine()];
					int end = lineStart + region.getEndIndex();
					if (end > maxEnd)
						maxEnd = end;

					if (!highlightedRegions.containsKey(region))
					{
						Object tieTag = highlightedTiedRegions.remove(region);
						if (tieTag != null)
							highlighter.removeHighlight(tieTag);

						int start = lineStart + region.getStartIndex();
						Object tag = highlighter.addHighlight(start, end, getPainter(region));
						highlightedRegions.put(region, tag);
					}
				}
			}

			// Add tied highlighted regions
			for (AbcRegion region : highlightedRegions.keySet())
			{
				for (AbcRegion t = region.getTiesFrom(); t != null; t = t.getTiesFrom())
				{
					if (highlightedTiedRegions.containsKey(t))
						continue;

					int lineStart = lineStartIndex[t.getLine()];
					int start = lineStart + t.getStartIndex();
					int end = lineStart + t.getEndIndex();
					Object tag = highlighter.addHighlight(start, end, getPainter(t));
					highlightedTiedRegions.put(t, tag);
				}
				for (AbcRegion t = region.getTiesTo(); t != null; t = t.getTiesTo())
				{
					if (highlightedTiedRegions.containsKey(t))
						continue;

					int lineStart = lineStartIndex[t.getLine()];
					int start = lineStart + t.getStartIndex();
					int end = lineStart + t.getEndIndex();
					Object tag = highlighter.addHighlight(start, end, getPainter(t));
					highlightedTiedRegions.put(t, tag);
				}
			}

			// Scroll to the end
			if (!scrollLock && maxEnd >= 0)
			{
				Rectangle rect = textArea.modelToView(maxEnd);
				if (rect != null)
					textArea.scrollRectToVisible(rect);
			}
		}
		catch (BadLocationException e)
		{
			throw new RuntimeException(e);
		}
	}
}

package com.digero.abcplayer.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.Scrollable;
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
import com.digero.common.abctomidi.AbcInfo;
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

	private NavigableSet<AbcRegion> regions = new TreeSet<AbcRegion>();
	private int lineOffset = 0;
	private boolean showFullPartName = false;
	private Integer scrollToIndexNextUpdate = null;
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
	private JTextArea gutterTextArea;
	private JScrollPane textAreaScrollPane;

	private boolean updatePending = false;
	private boolean resetAllHighlightsNextUpdate = false;
	private boolean focusTextAreaNextUpdate = true;
	private int lastAutoScrollY = -1;
	private boolean dragActive = false;

	private JCheckBox autoScrollCheckBox;
	private JComboBox<PartInfo> followTrackComboBox;

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
		textArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
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

		gutterTextArea = new JTextArea();
		gutterTextArea.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 0, 1, ColorTable.GRAPH_BORDER_OFF.get()), textArea.getBorder()));
		gutterTextArea.setFont(textArea.getFont());
		gutterTextArea.setEditable(false);
		gutterTextArea.setFocusable(false);
		gutterTextArea.setLineWrap(false);
		gutterTextArea.setColumns(4);
		gutterTextArea.setRows(textArea.getRows());
		gutterTextArea.setCaret(new NullCaret());
		gutterTextArea.setBackground(ColorTable.GRAPH_BACKGROUND_ENABLED.get());
		gutterTextArea.setForeground(ColorTable.NOTE_OFF.get());
		gutterTextArea.setDisabledTextColor(ColorTable.NOTE_OFF.get());

		textAreaScrollPane = new JScrollPane(new TextAreaContainer(textArea, gutterTextArea));
		setBackground(textArea.getBackground());
		content.add(textAreaScrollPane, BorderLayout.CENTER);

		JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));

		toolBar.add(new JLabel("Part: "));

		followTrackComboBox = new JComboBox<PartInfo>();
		followTrackComboBox.setEnabled(false);
		followTrackComboBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				PartInfo part = (PartInfo) followTrackComboBox.getSelectedItem();
				if (part != null)
					scrollToLineNumber(part.trackStartLine);

				update();
			}
		});
		toolBar.add(followTrackComboBox);

		autoScrollCheckBox = new JCheckBox("Auto scroll");
		autoScrollCheckBox.setSelected(true);
		autoScrollCheckBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				lastAutoScrollY = -1;
				update();
			}
		});
		toolBar.add(autoScrollCheckBox);

		content.add(toolBar, BorderLayout.NORTH);

		pack();

		Preferences prefs = Preferences.userNodeForPackage(HighlightAbcNotesFrame.class).node("HighlightAbcNotesFrame");
		Util.initWinBounds(this, prefs.node("window"), 800, 600);
	}

	private static class TextAreaContainer extends JPanel implements Scrollable
	{
		private JTextArea textArea;
		private JTextArea gutter;

		public TextAreaContainer(JTextArea textArea, JTextArea gutter)
		{
			super(new BorderLayout());
			this.textArea = textArea;
			this.gutter = gutter;
			add(textArea, BorderLayout.CENTER);
			add(gutter, BorderLayout.WEST);
		}

		@Override public Dimension getPreferredSize()
		{
			Dimension a = textArea.getPreferredSize();
			Dimension b = gutter.getPreferredSize();
			return new Dimension(a.width + b.width, Math.max(a.height, b.height));
		}

		@Override public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return textArea.getScrollableUnitIncrement(visibleRect, orientation, direction);
		}

		@Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return textArea.getScrollableBlockIncrement(visibleRect, orientation, direction);
		}

		@Override public boolean getScrollableTracksViewportWidth()
		{
			Container parent = SwingUtilities.getUnwrappedParent(this);
			return (parent instanceof JViewport) ? (parent.getWidth() > getPreferredSize().width) : false;
		}

		@Override public boolean getScrollableTracksViewportHeight()
		{
			Container parent = SwingUtilities.getUnwrappedParent(this);
			return (parent instanceof JViewport) ? (parent.getHeight() > textArea.getPreferredSize().height) : false;
		}
	}

	/** For use in followTrackComboBox */
	private static class PartInfo
	{
		public final String name;
		public final int trackNumber;
		public final int trackStartLine;

		public PartInfo(String name, int trackNumber, int trackStartLine)
		{
			this.name = name;
			this.trackNumber = trackNumber;
			this.trackStartLine = trackStartLine;
		}

		@Override public String toString()
		{
			return name;
		}
	}

	public void addDropListener(DropTargetListener listener)
	{
		new DropTarget(this, listener);
		new DropTarget(textArea, listener);
		new DropTarget(gutterTextArea, listener);
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

		@Override public void mousePressed(MouseEvent e)
		{
			textArea.requestFocusInWindow();
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
			int key = e.getKeyCode();
			switch (key)
			{
				case KeyEvent.VK_ESCAPE:
					endDrag(false);
					break;

				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_PAGE_UP:
				case KeyEvent.VK_PAGE_DOWN:
					boolean horz = (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT);
					boolean back = (key == KeyEvent.VK_UP || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_PAGE_UP);
					boolean page = (key == KeyEvent.VK_PAGE_UP || key == KeyEvent.VK_PAGE_DOWN);
					scroll(horz, back, page);
					break;
			}
		}

		private void scroll(boolean horz, boolean backwards, boolean page)
		{
			JScrollBar bar = horz ? textAreaScrollPane.getHorizontalScrollBar() : textAreaScrollPane
					.getVerticalScrollBar();

			int dir = backwards ? -1 : 1;
			int incr = page ? bar.getBlockIncrement(dir) : bar.getUnitIncrement(dir);

			Point pt = textAreaScrollPane.getViewport().getViewPosition();
			if (horz)
				pt.x = Util.clamp(pt.x + incr * dir, 0, textArea.getHeight());
			else
				pt.y = Util.clamp(pt.y + incr * dir, 0, textArea.getHeight());

			textAreaScrollPane.getViewport().setViewPosition(pt);
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
			if (regions == null)
				return;

			if (indexToRegion == null)
			{
				indexToRegion = new TreeMap<Integer, AbcRegion>();
				for (AbcRegion region : regions)
				{
					indexToRegion.put(lineStartIndex[getLine(region)] + region.getEndIndex(), region);
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
					int line = getLine(e.getValue());

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

	public void clearLinesAndRegions()
	{
		List<String> emptyLines = Collections.emptyList();
		setLinesAndRegions(emptyLines, 0, null);
	}

	public void setLinesAndRegions(List<String> lines, int lineOffset, AbcInfo abcInfo)
	{
		clearAllHighlights();

		// Init text area
		{
			int maxLine = lines.size() + lineOffset + 1;
			int gutterWidth = (maxLine < 1000) ? 3 : (int) Math.ceil(Math.log10(maxLine));
			gutterTextArea.setColumns(gutterWidth);
			String gutterFormatString = "%" + gutterWidth + "d\r\n";

			lineStartIndex = new int[lines.size() + 1];
			StringBuilder text = new StringBuilder();
			StringBuilder gutterText = new StringBuilder();
			for (int i = 0; i < lines.size(); i++)
			{
				lineStartIndex[i] = text.length();
				text.append(lines.get(i)).append("\r\n");
				gutterText.append(String.format(gutterFormatString, i + lineOffset + 1));
			}

			// Trim trailing newline
			if (text.length() > "\r\n".length())
			{
				text.setLength(text.length() - "\r\n".length());
				gutterText.setLength(gutterText.length() - "\r\n".length());
			}

			lineStartIndex[lines.size()] = text.length();
			textArea.setText(text.toString());
			gutterTextArea.setText(gutterText.toString());
		}

		// Init regions
		{
			NavigableSet<AbcRegion> regions = (abcInfo != null) ? abcInfo.getRegions() : null;
			if (regions == null)
				regions = new TreeSet<AbcRegion>();

			// Filter out regions that are not in the view
			boolean hasRegionsOutOfRange = false;
			for (AbcRegion region : regions)
			{
				int line = region.getLine() - lineOffset;
				if (line < 0 || line >= lines.size())
				{
					hasRegionsOutOfRange = true;
					break;
				}
			}

			if (hasRegionsOutOfRange)
			{
				TreeSet<AbcRegion> regionsInRange = new TreeSet<AbcRegion>();
				for (AbcRegion region : regions)
				{
					int line = region.getLine() - lineOffset;
					if (line >= 0 && line < lines.size())
						regionsInRange.add(region);
				}

				regions = regionsInRange;
			}

			this.regions = regions;
			this.lineOffset = lineOffset;
		}

		// Init track list in the combo box
		{
			followTrackComboBox.removeAllItems();
			if (abcInfo != null)
			{
				SortedSet<Integer> tracksInView = new TreeSet<Integer>();
				for (AbcRegion region : regions)
					tracksInView.add(region.getTrackNumber());

				for (int i : tracksInView)
				{
					String name = abcInfo.getPartNumber(i) + ". "
							+ (showFullPartName ? abcInfo.getPartFullName(i) : abcInfo.getPartName(i));
					followTrackComboBox.addItem(new PartInfo(name, i, abcInfo.getPartStartLine(i)));
				}
			}
			followTrackComboBox.setEnabled(followTrackComboBox.getItemCount() > 0);
		}

		focusTextAreaNextUpdate = true;
		lastAutoScrollY = -1;
		scrollToIndexNextUpdate = null;
		indexToRegion = null;
		update();
	}

	private int getLine(AbcRegion region)
	{
		return region.getLine() - lineOffset;
	}

	public void scrollToLineNumber(int line)
	{
		line -= lineOffset;
		if (line >= 0 && line < lineStartIndex.length)
		{
			scrollToIndexNextUpdate = lineStartIndex[line];
			update();
		}
		else
		{
			scrollToIndexNextUpdate = null;
		}
	}

	public int getFollowedTrackNumber()
	{
		PartInfo part = (PartInfo) followTrackComboBox.getSelectedItem();
		return (part != null) ? part.trackNumber : -1;
	}

	public void setFollowedTrackNumber(int trackNumber)
	{
		for (int i = 0; i < followTrackComboBox.getItemCount(); i++)
		{
			if (trackNumber == followTrackComboBox.getItemAt(i).trackNumber)
			{
				followTrackComboBox.setSelectedIndex(i);
				return;
			}
		}
	}

	public void setShowFullPartName(boolean showFullPartName)
	{
		this.showFullPartName = showFullPartName;
	}

	private boolean isRegionOn(AbcRegion region, long tick)
	{
		return (region.getStartTick() <= tick && region.getEndTick() > tick);
	}

	private Highlighter.HighlightPainter getPainter(AbcRegion region)
	{
		boolean active = sequencer.isTrackActive(region.getTrackNumber());
		if (region.isChord())
			return chordPainter;
		if (region.getNote() == Note.REST)
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

		if (focusTextAreaNextUpdate)
		{
			textArea.requestFocusInWindow();
			focusTextAreaNextUpdate = false;
		}

		if (scrollToIndexNextUpdate != null)
		{
			try
			{
				Rectangle rect = textArea.modelToView(scrollToIndexNextUpdate);
				Point scrollPt = (rect != null) ? new Point(0, rect.y) : new Point(0, 0);
				textAreaScrollPane.getViewport().setViewPosition(scrollPt);
			}
			catch (BadLocationException e)
			{
			}
			scrollToIndexNextUpdate = null;
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
			int endOfFollowedTrack = -1;
			int followedTrackNumber = getFollowedTrackNumber();
			for (AbcRegion region : regions)
			{
				if (region.getStartTick() > tick)
					break;

				if (isRegionOn(region, tick))
				{
					int lineStart = lineStartIndex[getLine(region)];
					int end = lineStart + region.getEndIndex();

					if (end > endOfFollowedTrack && region.getTrackNumber() == followedTrackNumber)
						endOfFollowedTrack = end;

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

					int lineStart = lineStartIndex[getLine(t)];
					int start = lineStart + t.getStartIndex();
					int end = lineStart + t.getEndIndex();
					Object tag = highlighter.addHighlight(start, end, getPainter(t));
					highlightedTiedRegions.put(t, tag);
				}
				for (AbcRegion t = region.getTiesTo(); t != null; t = t.getTiesTo())
				{
					if (highlightedTiedRegions.containsKey(t))
						continue;

					int lineStart = lineStartIndex[getLine(t)];
					int start = lineStart + t.getStartIndex();
					int end = lineStart + t.getEndIndex();
					Object tag = highlighter.addHighlight(start, end, getPainter(t));
					highlightedTiedRegions.put(t, tag);
				}
			}

			// Scroll to the end
			if (endOfFollowedTrack >= 0 && autoScrollCheckBox.isSelected())
			{
				Rectangle rect = textArea.modelToView(endOfFollowedTrack);
				if (rect != null && rect.y != lastAutoScrollY)
				{
					lastAutoScrollY = rect.y;
					if (!dragActive)
					{
						final int marginLines = 4;
						rect.y -= rect.height * marginLines;
						rect.height *= (2 * marginLines + 1);
						textArea.scrollRectToVisible(rect);
					}
				}
			}
		}
		catch (BadLocationException e)
		{
			throw new RuntimeException(e);
		}
	}
}

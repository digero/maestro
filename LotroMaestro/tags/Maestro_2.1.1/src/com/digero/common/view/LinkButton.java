package com.digero.common.view;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

public class LinkButton extends JLabel
{
	private List<ActionListener> listeners = null;

	public LinkButton()
	{
		init();
	}

	public LinkButton(String text)
	{
		super("<html><u>" + text + "</u></html>");
		init();
	}

	private void init()
	{
		setForeground(ColorTable.LINK.get());
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseListener(new MouseAdapter()
		{
			@Override public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					fireActionEvent();
				}
			}
		});
	}

	public void addActionListener(ActionListener listener)
	{
		if (listeners == null)
			listeners = new ArrayList<ActionListener>(1);

		listeners.add(listener);
	}

	public void removeActionListener(ActionListener listener)
	{
		if (listeners != null)
			listeners.remove(listener);
	}

	protected void fireActionEvent()
	{
		if (listeners != null && listeners.isEmpty())
			listeners = null;

		if (listeners != null)
		{
			ActionEvent e = new ActionEvent(this, 0, "click");
			for (ActionListener l : listeners)
			{
				l.actionPerformed(e);
			}
		}
	}
}

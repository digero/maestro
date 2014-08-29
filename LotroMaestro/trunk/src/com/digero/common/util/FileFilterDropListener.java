package com.digero.common.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class FileFilterDropListener implements DropTargetListener
{
	private FileFilter filter;
	private boolean acceptMultiple;
	private List<File> draggingFiles = null;
	private List<ActionListener> listeners = null;
	private DropTargetDropEvent dropEvent = null;

	public FileFilterDropListener(boolean acceptMultiple, String... fileTypes)
	{
		this(acceptMultiple, new ExtensionFileFilter("", fileTypes));
	}

	public FileFilterDropListener(boolean acceptMultiple, FileFilter filter)
	{
		this.acceptMultiple = acceptMultiple;
		this.filter = filter;
	}

	public void addActionListener(ActionListener l)
	{
		if (listeners == null)
			listeners = new ArrayList<ActionListener>(1);

		listeners.add(l);
	}

	public void removeActionListener(ActionListener l)
	{
		if (listeners != null)
			listeners.remove(l);
	}

	public File getDroppedFile()
	{
		if (draggingFiles == null || draggingFiles.isEmpty())
			return null;

		return draggingFiles.get(0);
	}

	public List<File> getDroppedFiles()
	{
		return draggingFiles;
	}

	@Override public void dragEnter(DropTargetDragEvent dtde)
	{
		draggingFiles = getMatchingFiles(dtde.getTransferable());
		if (draggingFiles != null)
		{
			dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		}
		else
		{
			dtde.rejectDrag();
		}
	}

	@Override public void dragExit(DropTargetEvent dte)
	{
	}

	@Override public void dragOver(DropTargetDragEvent dtde)
	{
	}

	@Override public void drop(DropTargetDropEvent dtde)
	{
		if (draggingFiles != null)
		{
			dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
			fireActionPerformed(dtde);
			draggingFiles = null;
		}
		else
		{
			dtde.rejectDrop();
		}
	}

	@Override public void dropActionChanged(DropTargetDragEvent dtde)
	{
		draggingFiles = getMatchingFiles(dtde.getTransferable());
		if (draggingFiles != null)
		{
			dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		}
		else
		{
			dtde.rejectDrag();
		}
	}

	public DropTargetDropEvent getDropEvent()
	{
		return dropEvent;
	}

	private void fireActionPerformed(DropTargetDropEvent dtde)
	{
		this.dropEvent = dtde;
		for (ActionListener l : listeners)
		{
			l.actionPerformed(new ActionEvent(this, 0, null));
		}
		this.dropEvent = null;
	}

	@SuppressWarnings("unchecked")//
	private List<File> getMatchingFiles(Transferable t)
	{
		if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
		{
			List<File> files;
			try
			{
				files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
			}
			catch (Exception e)
			{
				return null;
			}

			if (files.isEmpty() || !acceptMultiple && files.size() > 1)
				return null;

			for (File file : files)
			{
				file = Util.resolveShortcut(file);

				if (file.isDirectory() || !filter.accept(file))
					return null;
			}

			return files;
		}
		else
		{
			return null;
		}
	}
}
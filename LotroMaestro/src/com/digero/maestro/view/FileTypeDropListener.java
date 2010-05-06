package com.digero.maestro.view;

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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import sun.awt.shell.ShellFolder;

public class FileTypeDropListener implements DropTargetListener {
	private Pattern fileNameRegex;
	private File draggingFile = null;
	private List<ActionListener> listeners = null;

	public FileTypeDropListener(String... fileTypes) {
		String regex = ".*\\.(" + fileTypes[0];
		for (int i = 1; i < fileTypes.length; i++)
			regex += "|" + fileTypes[i];
		regex += ")$";
		fileNameRegex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	public void addActionListener(ActionListener l) {
		if (listeners == null)
			listeners = new ArrayList<ActionListener>(1);

		listeners.add(l);
	}

	public void removeActionListener(ActionListener l) {
		if (listeners != null)
			listeners.remove(l);
	}

	public File getDroppedFile() {
		return draggingFile;
	}

	public void dragEnter(DropTargetDragEvent dtde) {
		draggingFile = getMatchingFile(dtde.getTransferable());
		if (draggingFile != null) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY);
		}
		else {
			dtde.rejectDrag();
		}
	}

	public void dragExit(DropTargetEvent dte) {
	}

	public void dragOver(DropTargetDragEvent dtde) {
	}

	public void drop(DropTargetDropEvent dtde) {
		if (draggingFile != null) {
			dtde.acceptDrop(DnDConstants.ACTION_COPY);
			fireActionPerformed();
			draggingFile = null;
		}
		else {
			dtde.rejectDrop();
		}
	}

	public void dropActionChanged(DropTargetDragEvent dtde) {
		draggingFile = getMatchingFile(dtde.getTransferable());
		if (draggingFile != null) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY);
		}
		else {
			dtde.rejectDrag();
		}
	}

	private void fireActionPerformed() {
		for (ActionListener l : listeners) {
			l.actionPerformed(new ActionEvent(this, 0, null));
		}
	}

	@SuppressWarnings("unchecked")
	private File getMatchingFile(Transferable t) {
		if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			List<File> files;
			try {
				files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
			}
			catch (Exception e) {
				return null;
			}
			if (files.size() >= 1) {
				File file = files.get(0);
				String name = file.getName().toLowerCase();

				if (name.endsWith(".lnk")) {
					try {
						file = ShellFolder.getShellFolder(file).getLinkLocation();
						name = file.getName();
					}
					catch (Throwable e) {
						return null;
					}
				}

				if (fileNameRegex.matcher(name).matches()) {
					return file;
				}
			}
		}
		return null;
	}
}
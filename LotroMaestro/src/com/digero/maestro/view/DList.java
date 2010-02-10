package com.digero.maestro.view;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import com.digero.maestro.project.ArrayListModel;

@SuppressWarnings("serial")
public class DList extends JList {
	public static DataFlavor DList_Flavor = new DataFlavor(DListData.class, "DListData");
	private static DataFlavor[] supportedFlavors = {
		DList_Flavor
	};

	public DList() {
		this(new ArrayListModel<Object>());
	}

	public DList(ArrayListModel<?> dataModel) {
		super(dataModel);
		setTransferHandler(new ReorderHandler());
		setDragEnabled(true);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public DList(Object[] listData) {
		this(new ArrayListModel<Object>(listData));
	}

	public DList(Vector<?> listData) {
		this(new ArrayListModel<Object>(listData));
	}

	public void dropComplete() {
	}

	private class ReorderHandler extends TransferHandler {
		@Override
		public boolean importData(TransferSupport support) {

			// this is the index of the element onto which the dragged element, is dropped
			final int dropIndex = DList.this.locationToIndex(getDropLocation().getDropPoint());

			try {
				Object[] draggedData = ((DListData) support.getTransferable().getTransferData(DList_Flavor)).data;
				final DList dragList = ((DListData) support.getTransferable().getTransferData(DList_Flavor)).parent;
				ArrayListModel<Object> dragModel = (ArrayListModel<Object>) dragList.getModel();
				ArrayListModel<Object> dropModel = (ArrayListModel<Object>) DList.this.getModel();

				final Object leadItem = dropIndex >= 0 ? dropModel.getElementAt(dropIndex) : null;
				final int dataLength = draggedData.length;

				// make sure that the lead item, is not in the dragged data
				if (leadItem != null)
					for (int i = 0; i < dataLength; i++)
						if (draggedData[i].equals(leadItem))
							return false;

				int dragLeadIndex = -1;
				final boolean localDrop = dropModel.contains(draggedData[0]);

				if (localDrop)
					dragLeadIndex = dropModel.indexOf(draggedData[0]);

				for (int i = 0; i < dataLength; i++)
					dragModel.remove(draggedData[i]);

				if (localDrop) {
					final int adjustedLeadIndex = dropModel.indexOf(leadItem);
					final int insertionAdjustment = dragLeadIndex <= adjustedLeadIndex ? 1 : 0;

					final int[] indices = new int[dataLength];
					for (int i = 0; i < dataLength; i++) {
						dropModel.add(adjustedLeadIndex + insertionAdjustment + i, draggedData[i]);
						indices[i] = adjustedLeadIndex + insertionAdjustment + i;
					}

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							DList.this.clearSelection();
							DList.this.setSelectedIndices(indices);
							dropComplete();
						}
					});
				}
				else {
					final int[] indices = new int[dataLength];
					for (int i = 0; i < dataLength; i++) {
						dropModel.add(dropIndex + 1, draggedData[i]);
						indices[i] = dropIndex + 1 + i;
					}

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							DList.this.clearSelection();
							DList.this.setSelectedIndices(indices);
							dragList.clearSelection();
							dropComplete();
						}
					});
				}
			}
			catch (Exception x) {
				x.printStackTrace();
			}
			return false;
		}

		public int getSourceActions(JComponent c) {
			return TransferHandler.MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c) {
			return new DListData(DList.this, DList.this.getSelectedValues());
		}

		@Override
		public boolean canImport(TransferSupport support) {
			if (!support.isDrop() || !support.isDataFlavorSupported(DList_Flavor))
				return false;

			return true;
		}

		@Override
		public Icon getVisualRepresentation(Transferable t) {
			// TODO Auto-generated method stub
			return super.getVisualRepresentation(t);
		}
	}

	private class DListData implements Transferable {

		private Object[] data;
		private DList parent;

		protected DListData(DList p, Object[] d) {
			parent = p;
			data = d;
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (flavor.equals(DList_Flavor))
				return DListData.this;
			else
				return null;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return supportedFlavors;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			// TODO Auto-generated method stub
			return true;
		}
	}
}
package com.digero.maestro.view;

import java.util.AbstractList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;

import com.digero.common.midi.SequencerWrapper;
import com.digero.common.util.IDiscardable;
import com.digero.common.view.ColorTable;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.PartAutoNumberer;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.SingleSelectionModel;

public class PartListPanel extends JPanel implements IDiscardable {
	private SingleSelectionModel<AbcPartPanel> selectionModel = new SingleSelectionModel<AbcPartPanel>();
	private PartList parts = new PartList();
	private SequencerWrapper abcSequencer;
	private PartAutoNumberer partAutoNumberer;

	private JPanel partsContainer = this;

	public PartListPanel(SequencerWrapper abcSequencer, PartAutoNumberer partAutoNumberer) {
		this.abcSequencer = abcSequencer;
		this.partAutoNumberer = partAutoNumberer;

		partsContainer.setBackground(ColorTable.GRAPH_BACKGROUND_OFF.get());
	}

	public SingleSelectionModel<AbcPartPanel> getSelectionModel() {
		return selectionModel;
	}

	public List<AbcPart> getParts() {
		return parts;
	}

	private class PartList extends AbstractList<AbcPart> {
		@Override
		public AbcPart get(int index) {
			return ((AbcPartPanel) partsContainer.getComponent(index)).getAbcPart();
		}

		@Override
		public int size() {
			return partsContainer.getComponentCount();
		}

		public void add(int index, AbcPart abcPart, TrackInfo trackInfo) {
			AbcPartPanel panel = new AbcPartPanel(abcSequencer, partAutoNumberer, selectionModel, abcPart, trackInfo);
			partsContainer.add(panel, index);

			partsContainer.validate();
			partsContainer.repaint();
		}

		@Override
		public AbcPart remove(int index) {
			boolean selected = selectionModel.isSelected((AbcPartPanel) partsContainer.getComponent(index));

			AbcPartPanel panel = (AbcPartPanel) partsContainer.getComponent(index);
			AbcPart part = panel.getAbcPart();

			partsContainer.remove(index);
			if (part != null)
				part.discard();
			panel.discard();
			partsContainer.remove(panel);

			if (selected) {
				int selIndex = index;
				if (selIndex > partsContainer.getComponentCount())
					selIndex = partsContainer.getComponentCount() - 1;
				if (selIndex >= 0)
					selectionModel.setSelectedItem((AbcPartPanel) partsContainer.getComponent(selIndex));
			}

			partsContainer.validate();
			partsContainer.repaint();

			return part;
		}
	}

	public void addNewPart(AbcPart part, TrackInfo trackInfo) {
		// TODO
	}

	public void setSelectedPart(int index) {

	}

	@Override
	public void discard() {
		for (int i = 0; i < partsContainer.getComponentCount(); i++) {
			AbcPartPanel panel = (AbcPartPanel) partsContainer.getComponent(i);
			AbcPart part = panel.getAbcPart();
			if (part != null)
				part.discard();
			panel.discard();
		}
		partsContainer.removeAll();

		selectionModel.discard();
	}

	private class PartComparator implements Comparator<AbcPartPanel> {
		@Override
		public int compare(AbcPartPanel o1, AbcPartPanel o2) {
			AbcPart p1 = o1.getAbcPart();
			AbcPart p2 = o2.getAbcPart();

			if (p1 == null)
				return (p2 == null) ? 0 : 1;

			if (p2 == null)
				return -1;

			int base1 = partAutoNumberer.getFirstNumber(p1.getInstrument());
			int base2 = partAutoNumberer.getFirstNumber(p2.getInstrument());

			if (base1 != base2)
				return base1 - base2;

			return p1.getPartNumber() - p2.getPartNumber();
		}
	}
}

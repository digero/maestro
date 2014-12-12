package com.digero.maestro.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.common.util.IDiscardable;

public class SingleSelectionModel<T extends ISelectable> implements IDiscardable {
	private T selectedItem;
	private List<ChangeListener> listeners;

	public void setSelectedItem(T newItem) {
		if (newItem != selectedItem) {
			if (selectedItem != null)
				selectedItem.setSelected(false);
			if (newItem != null)
				newItem.setSelected(true);
			fireChangeEvent();
		}
	}

	public boolean isSelected(ISelectable item) {
		return item != null && selectedItem == item;
	}

	public T getSelectedItem() {
		return selectedItem;
	}

	public void addChangeListener(ChangeListener listener) {
		if (listener == null)
			throw new NullPointerException();

		if (listeners == null)
			listeners = new ArrayList<ChangeListener>();

		listeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		if (listeners != null)
			listeners.remove(listener);
	}

	protected void fireChangeEvent() {
		if (listeners != null) {
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener listener : listeners) {
				listener.stateChanged(e);
			}
		}
	}

	@Override
	public void discard() {
		listeners = null;
	}
}

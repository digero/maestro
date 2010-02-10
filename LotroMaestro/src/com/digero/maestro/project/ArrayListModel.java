package com.digero.maestro.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;

@SuppressWarnings("serial")
public class ArrayListModel<T> extends AbstractListModel {
	private List<T> list;

	public ArrayListModel() {
		this.list = new ArrayList<T>();
	}
	
	public ArrayListModel(Collection<? extends T> list) {
		this.list = new ArrayList<T>(list);
	}
	
	public ArrayListModel(T[] list) {
		this(Arrays.asList(list));
	}

	public List<T> getList() {
		return Collections.unmodifiableList(list);
	}

	@Override
	public T getElementAt(int index) {
		return list.get(index);
	}

	@Override
	public int getSize() {
		return list.size();
	}
	
	public boolean contains(T e) {
		return list.contains(e);
	}
	
	public int indexOf(T e) {
		return list.indexOf(e);
	}

	public boolean remove(T e) {
		int idx = list.indexOf(e);
		if (idx < 0)
			return false;
		remove(idx);
		return true;
	}

	public T remove(int idx) {
		T e = list.remove(idx);
		fireIntervalRemoved(this, idx, idx);
		return e;
	}

	public boolean add(T e) {
		int idx = list.size();
		list.add(e);
		fireIntervalAdded(this, idx, idx);
		return true;
	}

	public boolean add(int idx, T e) {
		list.add(idx, e);
		fireIntervalAdded(this, idx, idx);
		return true;
	}

	public void move(int fromIndex, int toIndex) {
		if (fromIndex != toIndex)
			add(toIndex, remove(fromIndex));
	}
}
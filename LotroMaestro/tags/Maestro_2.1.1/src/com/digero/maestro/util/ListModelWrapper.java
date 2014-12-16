package com.digero.maestro.util;

import java.util.AbstractList;

import javax.swing.DefaultListModel;

public class ListModelWrapper<E> extends AbstractList<E>
{
	private DefaultListModel<E> listModel;

	public ListModelWrapper(DefaultListModel<E> listModel)
	{
		this.listModel = listModel;
	}

	public DefaultListModel<E> getListModel()
	{
		return listModel;
	}

	@Override public void clear()
	{
		listModel.clear();
	}

	@Override public E get(int index)
	{
		return listModel.getElementAt(index);
	}

	@Override public int size()
	{
		return listModel.getSize();
	}

	@Override public E set(int index, E element)
	{
		return listModel.set(index, element);
	}

	@Override public void add(int index, E element)
	{
		listModel.add(index, element);
	}

	@Override public E remove(int index)
	{
		return listModel.remove(index);
	}

	@Override public boolean remove(Object o)
	{
		return listModel.removeElement(o);
	}
}

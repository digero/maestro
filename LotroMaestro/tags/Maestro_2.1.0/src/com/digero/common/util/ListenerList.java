package com.digero.common.util;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class ListenerList<E extends EventObject> implements IDiscardable
{
	private List<Listener<E>> listeners = new ArrayList<Listener<E>>();

	private int firing = 0;
	private int lastModifiedFiring = 0;

	public ListenerList()
	{
	}

	@Override public void discard()
	{
		listeners = null;
	}

	public int size()
	{
		if (listeners == null)
			return 0;

		return listeners.size();
	}

	public void add(Listener<E> l)
	{
		if (listeners == null)
			return;

		modifyListeners();
		listeners.add(l);
	}

	public void remove(Listener<E> l)
	{
		if (listeners == null)
			return;

		modifyListeners();
		listeners.remove(l);
	}

	public void fire(E e)
	{
		if (listeners == null || listeners.size() == 0)
			return;

		try
		{
			firing++;
			List<Listener<E>> listeners = this.listeners;

			/* Important: do not access this.listeners after this point. It may be copied and
			 * modified if a listener is added or removed while firing this event. If that happens,
			 * this loop will finish on the old copy of the list. */
			for (Listener<E> l : listeners)
			{
				l.onEvent(e);
			}
		}
		finally
		{
			firing--;
			if (lastModifiedFiring > firing)
				lastModifiedFiring = firing;
		}
	}

	private void modifyListeners()
	{
		if (listeners == null)
			return;

		// If we're currently firing an event and haven't yet copied the 
		// listener list for this firing iteration, do so now.
		if (firing > lastModifiedFiring)
		{
			listeners = new ArrayList<Listener<E>>(listeners);
			lastModifiedFiring = firing;
		}
	}
}

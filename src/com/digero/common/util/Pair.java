package com.digero.common.util;

public class Pair<T1, T2>
{
	public T1 first;
	public T2 second;

	public Pair()
	{
		first = null;
		second = null;
	}

	public Pair(T1 first, T2 second)
	{
		this.first = first;
		this.second = second;
	}

	@Override public boolean equals(Object obj)
	{
		if (!(obj instanceof Pair<?, ?>))
			return false;

		Pair<?, ?> that = (Pair<?, ?>) obj;
		return ((this.first == null) ? (that.first == null) : this.first.equals(that.first))
				&& ((this.second == null) ? (that.second == null) : this.second.equals(that.second));
	}

	@Override public int hashCode()
	{
		int hash = (first == null) ? 0 : first.hashCode();
		if (second != null)
			hash ^= Integer.rotateLeft(second.hashCode(), Integer.SIZE / 2);
		return hash;
	}
}

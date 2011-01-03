package com.digero.abcplayer.viz;

public class Region implements Comparable<Region> {
	private final int start;
	private final int end;

	public Region(int start, int end) {
		if (start > end) {
			int tmp = start;
			start = end;
			end = tmp;
		}

		this.start = start;
		this.end = end;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Region) {
			Region that = (Region) obj;
			return this.start == that.start && this.end == that.end;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (start << 15) ^ end;
	}

	@Override
	public int compareTo(Region that) {
		if (this.start != that.start)
			return (this.start - that.start);
		else
			return (this.end - that.end);
	}
}
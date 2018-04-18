package com.digero.common.abctomidi;

import com.digero.common.midi.Note;

public class AbcRegion implements Comparable<AbcRegion>
{
	private final int line;
	private final int startIndex;
	private final int endIndex;
	private final long startTick;
	private final long endTick;
	private final Note note;
	private final int trackNumber;
	private AbcRegion tiesFrom;
	private AbcRegion tiesTo;

	public AbcRegion(int line, int startIndex, int endIndex, long startTick, long endTick, Note note, int trackNumber)
	{
		this.line = line;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.startTick = startTick;
		this.endTick = endTick;
		this.note = note;
		this.trackNumber = trackNumber;
	}

	public int getLine()
	{
		return line;
	}

	public int getStartIndex()
	{
		return startIndex;
	}

	public int getEndIndex()
	{
		return endIndex;
	}

	public long getStartTick()
	{
		return startTick;
	}

	public long getEndTick()
	{
		return endTick;
	}

	public Note getNote()
	{
		return note;
	}

	public boolean isChord()
	{
		return note == null;
	}

	public int getTrackNumber()
	{
		return trackNumber;
	}

	public AbcRegion getTiesFrom()
	{
		return tiesFrom;
	}

	public void setTiesFrom(AbcRegion tiesFrom)
	{
		this.tiesFrom = tiesFrom;
	}

	public AbcRegion getTiesTo()
	{
		return tiesTo;
	}

	public void setTiesTo(AbcRegion tiesTo)
	{
		this.tiesTo = tiesTo;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj == null || obj.getClass() != AbcRegion.class)
			return false;

		return this.compareTo((AbcRegion) obj) == 0;
	}

	@Override public int hashCode()
	{
		return (line << 15) ^ (startIndex << 7) ^ endIndex ^ (int) (startTick << 7) ^ (int) endTick;
	}

	@Override public int compareTo(AbcRegion that)
	{
		if (this.startTick != that.startTick)
			return (int) (this.startTick - that.startTick);

		if (this.endTick != that.endTick)
			return (int) (this.endTick - that.endTick);

		if (this.line != that.line)
			return (this.line - that.line);

		if (this.startIndex != that.startIndex)
			return (this.startIndex - that.startIndex);

		return (this.endIndex - that.endIndex);
	}
}
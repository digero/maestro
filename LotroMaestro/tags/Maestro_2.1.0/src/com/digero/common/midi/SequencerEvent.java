package com.digero.common.midi;

import java.util.EventObject;

@SuppressWarnings("serial")
public class SequencerEvent extends EventObject
{

	public enum SequencerProperty
	{
		POSITION, LENGTH, DRAG_POSITION, IS_DRAGGING, IS_RUNNING, IS_LOADED, TRACK_ACTIVE, TEMPO, SEQUENCE;

		public static final int THUMB_POSITION_MASK = POSITION.mask | DRAG_POSITION.mask | IS_DRAGGING.mask;

		public final int mask;

		public static long makeMask(SequencerProperty[] props)
		{
			int mask = 0;
			for (SequencerProperty prop : props)
			{
				mask |= prop.mask;
			}
			return mask;
		}

		public boolean isInMask(int mask)
		{
			return (mask & this.mask) != 0;
		}

		private SequencerProperty()
		{
			mask = MaskMaker.getNextMask();
		}

		private static class MaskMaker
		{
			private static int nextMask = 1;

			public static int getNextMask()
			{
				if (nextMask < 0)
					throw new RuntimeException("Mask overflow; convert int to long");
				int mask = nextMask;
				nextMask <<= 1;
				return mask;
			}
		}
	}

	private SequencerProperty property;

	public SequencerEvent(SequencerWrapper sequencerWrapper, SequencerProperty property)
	{
		super(sequencerWrapper);
		this.property = property;
	}

	@Override public SequencerWrapper getSource()
	{
		return (SequencerWrapper) super.getSource();
	}

	public SequencerProperty getProperty()
	{
		return property;
	}
}

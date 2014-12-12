package com.digero.common.midi;

public interface ITempoCache
{
	long tickToMicros(long tick);

	long microsToTick(long micros);
}

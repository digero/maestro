package com.digero.maestro.midi;

public interface ITempoCache
{
	long tickToMicros(long tick);

	long microsToTick(long micros);
}

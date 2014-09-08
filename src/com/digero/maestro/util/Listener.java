package com.digero.maestro.util;

public interface Listener<EventType>
{
	void onEvent(EventType e);
}

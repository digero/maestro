package com.digero.common.util;

import java.util.EventListener;
import java.util.EventObject;

public interface Listener<EventType extends EventObject> extends EventListener
{
	void onEvent(EventType e);
}

package com.digero.abcplayer.view;

import com.digero.common.abc.LotroInstrument;

public interface TrackListPanelCallback
{
	void setTrackInstrumentOverride(int trackIndex, LotroInstrument instrument);
	void showHighlightPanelForTrack(int trackIndex);
}

package com.digero.maestro.abc;

import java.io.File;

public interface AbcMetadataSource
{
	public String getSongTitle();

	public String getComposer();

	public String getTranscriber();

	public long getSongLengthMicros();

	public File getExportFile();

	public String getPartName(AbcPartMetadataSource abcPart);
}

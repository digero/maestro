package com.digero.common.abctomidi;

import java.io.File;
import java.util.List;

public class FileAndData
{
	public final File file;
	public final List<String> lines;

	public FileAndData(File file, List<String> lines)
	{
		this.file = file;
		this.lines = lines;
	}
}

package com.digero.maestro.util;

import java.io.File;

public interface FileResolver
{
	/**
	 * When there is a failure loading a file (e.g. file not found, or failed to parse), this method
	 * is called to find a replacement file.
	 * 
	 * @param original The file that caused the failure
	 * @param message A description of the failure
	 * @return The new file, or <b>null</b> if no new file is available.
	 */
	File resolveFile(File original, String message);
}

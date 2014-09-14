package com.digero.common.util;

import java.io.File;
import java.util.regex.Pattern;

public class ExtensionFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter
{
	private Pattern fileNameRegex;
	private String description;
	private boolean matchDirectories;

	public ExtensionFileFilter(String description, String... fileTypes)
	{
		this(description, true, fileTypes);
	}

	public ExtensionFileFilter(String description, boolean matchDirectories, String... fileTypes)
	{
		this.description = description;
		this.matchDirectories = matchDirectories;

		String regex = ".*\\.(" + fileTypes[0];
		for (int i = 1; i < fileTypes.length; i++)
			regex += "|" + fileTypes[i];
		regex += ")$";
		fileNameRegex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	@Override public boolean accept(File f)
	{
		if (f.isDirectory())
			return matchDirectories;

		return fileNameRegex.matcher(f.getName()).matches();
	}

	@Override public String getDescription()
	{
		return description;
	}

}

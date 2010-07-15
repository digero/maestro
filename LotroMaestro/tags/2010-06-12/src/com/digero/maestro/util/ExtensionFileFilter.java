package com.digero.maestro.util;

import java.io.File;
import java.util.regex.Pattern;

public class ExtensionFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {
	private Pattern fileNameRegex;
	private String description;

	public ExtensionFileFilter(String description, String... fileTypes) {
		this.description = description;

		String regex = ".*\\.(" + fileTypes[0];
		for (int i = 1; i < fileTypes.length; i++)
			regex += "|" + fileTypes[i];
		regex += ")$";
		fileNameRegex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;

		return fileNameRegex.matcher(f.getName()).matches();
	}

	@Override
	public String getDescription() {
		return description;
	}

}

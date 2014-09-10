package com.digero.common.abc;

import java.util.HashMap;
import java.util.Map;

public enum AbcField
{
	SONG_TITLE, //
	SONG_COMPOSER, //
	SONG_DURATION, //
	SONG_TRANSCRIBER, //
	//
	ABC_VERSION, //
	ABC_CREATOR, //
	//
	PART_NAME, //
	//
	TEMPO("Q:"), //
	;

	private static class MetaData
	{
		private static Map<String, Integer> longestByPrefix = new HashMap<String, Integer>();
	}

	private final String formattedName;

	private AbcField()
	{
		this(null);
	}

	private AbcField(String formattedName)
	{
		this.formattedName = formattedName;
		String name = getFormattedName();
		String prefix = getPrefix(name);
		Integer maxLength = MetaData.longestByPrefix.get(prefix);
		if (maxLength == null || name.length() > maxLength)
			MetaData.longestByPrefix.put(prefix, name.length());
	}

	public static AbcField fromString(String string)
	{
		if (string.startsWith("%%"))
			string = string.substring(2);

		string = string.trim();
		int space = string.indexOf(' ');
		if (space > 0)
			string = string.substring(0, space);

		for (AbcField f : values())
		{
			if (f.getFormattedName().equalsIgnoreCase(string))
				return f;
		}
		return null;
	}

	public String getFormattedName()
	{
		if (formattedName != null)
			return formattedName;

		return name().toLowerCase().replace('_', '-');
	}

	@Override public String toString()
	{
		String name = getFormattedName();
		Integer maxLength = MetaData.longestByPrefix.get(getPrefix(name));
		int paddedLength = 1 + ((maxLength == null) ? name.length() : maxLength);

		StringBuilder output = new StringBuilder(2 + paddedLength);
		output.append("%%");
		output.append(name);
		for (int i = name.length(); i < paddedLength; i++)
		{
			output.append(' ');
		}

		return output.toString();
	}

	private static String getPrefix(String s)
	{
		int dash = s.indexOf('-');
		if (dash > 0)
			s = s.substring(0, dash);
		return s;
	}
}

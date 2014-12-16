package com.digero.common.util;

public class ParseException extends Exception
{
	public ParseException(String message, String fileName, int line, int column)
	{
		super(formatMessage(message, fileName, line, column));
	}

	public ParseException(String message, String fileName, int line)
	{
		super(formatMessage(message, fileName, line, -1));
	}

	public ParseException(String message, String fileName)
	{
		super(formatMessage(message, fileName, -1, -1));
	}

	private static String formatMessage(String message, String fileName, int line, int column)
	{
		String msg = "Error";
		if (fileName != null && fileName.length() > 0)
			msg += " reading " + fileName;

		if (line >= 0)
		{
			msg += " on line " + line;
			if (column >= 0)
				msg += ", column " + (column + 1);
		}

		msg += ":\n" + message;

		return msg;
	}
}
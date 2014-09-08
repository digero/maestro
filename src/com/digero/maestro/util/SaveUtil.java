package com.digero.maestro.util;

import java.io.File;

import org.w3c.dom.Element;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.ParseException;

public class SaveUtil
{
	private SaveUtil()
	{
	}

	public static String parseProperty(Element ele, String propName, String defaultValue)
	{
		if (!ele.hasAttribute(propName))
			return defaultValue;

		return ele.getAttribute(propName);
	}

	public static int parseProperty(Element ele, String propName, int defaultValue) throws ParseException
	{
		if (!ele.hasAttribute(propName))
			return defaultValue;

		String value = ele.getAttribute(propName);
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			throw makeParseException(ele, propName, e.getMessage());
		}
	}

	public static float parseProperty(Element ele, String propName, float defaultValue) throws ParseException
	{
		if (!ele.hasAttribute(propName))
			return defaultValue;

		String value = ele.getAttribute(propName);
		try
		{
			return Float.parseFloat(value);
		}
		catch (NumberFormatException e)
		{
			throw makeParseException(ele, propName, e.getMessage());
		}
	}

	public static boolean parseProperty(Element ele, String propName, boolean defaultValue) throws ParseException
	{
		if (!ele.hasAttribute(propName))
			return defaultValue;

		String value = ele.getAttribute(propName).toLowerCase();
		if (value.equals("true") || value.equals("1"))
			return true;
		if (value.equals("false") || value.equals("0"))
			return false;

		throw makeParseException(ele, propName, "Value must be 'true' or 'false'");
	}

	public static TimeSignature parseProperty(Element ele, String propName, TimeSignature defaultValue)
			throws ParseException
	{
		if (!ele.hasAttribute(propName))
			return defaultValue;

		String value = ele.getAttribute(propName);
		try
		{
			return new TimeSignature(value);
		}
		catch (IllegalArgumentException e)
		{
			throw makeParseException(ele, propName, e.getMessage());
		}
	}

	public static KeySignature parseProperty(Element ele, String propName, KeySignature defaultValue)
			throws ParseException
	{
		if (!ele.hasAttribute(propName))
			return defaultValue;

		String value = ele.getAttribute(propName);
		try
		{
			return new KeySignature(value);
		}
		catch (IllegalArgumentException e)
		{
			throw makeParseException(ele, propName, e.getMessage());
		}
	}

	public static LotroInstrument parseProperty(Element ele, String propName, LotroInstrument defaultValue)
			throws ParseException
	{
		if (!ele.hasAttribute(propName))
			return defaultValue;

		String value = ele.getAttribute(propName);
		try
		{
			return LotroInstrument.parseInstrument(value);
		}
		catch (IllegalArgumentException e)
		{
			throw makeParseException(ele, propName, e.getMessage());
		}
	}

	private static ParseException makeParseException(Element ele, String attribName, String message)
	{
		String msg = "Invalid value " + attribName + "=\"" + ele.getAttribute(attribName) + "\"";
		if (message != null && message.length() > 0)
			msg += ": " + message;

		File f = XmlUtil.getDocumentFile(ele.getOwnerDocument());
		String fileName = (f == null) ? null : f.getName();
		return new ParseException(msg, fileName, XmlUtil.getLineNumber(ele));
	}
}

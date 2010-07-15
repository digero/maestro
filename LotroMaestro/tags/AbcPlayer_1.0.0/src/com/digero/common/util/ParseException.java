package com.digero.common.util;

public class ParseException extends Exception {
	public ParseException(String message, String fileName, int line, int column) {
		super("Error reading " + fileName + " on line " + line + ", column " + (column + 1) + ":\n" + message);
	}

	public ParseException(String message, String fileName, int line) {
		super("Error reading " + fileName + " on line " + line + ":\n" + message);
	}

	public ParseException(String message, String fileName) {
		super("Error reading " + fileName + ":\n" + message);
	}
}
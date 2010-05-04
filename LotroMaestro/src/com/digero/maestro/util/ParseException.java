package com.digero.maestro.util;

public class ParseException extends Exception {
	public ParseException(String message, int line, int column) {
		super("Error reading ABC file on line " + line + ", column " + (column + 1) + ":\n" + message);
	}

	public ParseException(String message, int line) {
		super("Error reading ABC file on line " + line + ":\n" + message);
	}

	public ParseException(String message) {
		super("Error reading ABC file:\n" + message);
	}
}
package com.digero.maestro.util.singleinstance;

public class SingleInstanceException extends Exception {
	public SingleInstanceException() {
	}

	public SingleInstanceException(String message) {
		super(message);
	}

	public SingleInstanceException(Throwable cause) {
		super(cause);
	}

	public SingleInstanceException(String message, Throwable cause) {
		super(message, cause);
	}
}

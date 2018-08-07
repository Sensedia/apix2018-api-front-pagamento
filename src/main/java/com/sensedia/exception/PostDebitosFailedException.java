package com.sensedia.exception;

import org.springframework.web.client.HttpStatusCodeException;

public class PostDebitosFailedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PostDebitosFailedException(HttpStatusCodeException ex) {
		super(ex);
	}

	public PostDebitosFailedException(String message) {
		super(message);
	}

}

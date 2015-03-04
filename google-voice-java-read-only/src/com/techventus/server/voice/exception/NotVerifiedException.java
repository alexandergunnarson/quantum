package com.techventus.server.voice.exception;

public class NotVerifiedException extends AuthenticationException {
	private static final long serialVersionUID = -6945694818861858697L;

	public NotVerifiedException() {
		super(ERROR_CODE.NotVerified);
	}
}

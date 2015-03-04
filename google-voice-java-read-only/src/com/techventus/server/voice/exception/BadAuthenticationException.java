package com.techventus.server.voice.exception;

public class BadAuthenticationException extends AuthenticationException {
	private static final long serialVersionUID = 370186803888464884L;

	public BadAuthenticationException() {
		super(ERROR_CODE.BadAuthentication);
	}
}

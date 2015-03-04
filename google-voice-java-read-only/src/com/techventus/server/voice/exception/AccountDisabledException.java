package com.techventus.server.voice.exception;

public class AccountDisabledException extends AuthenticationException {
	private static final long serialVersionUID = 3975877956108653417L;

	public AccountDisabledException() {
		super(ERROR_CODE.AccountDisabled);
	}
}

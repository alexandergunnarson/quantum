package com.techventus.server.voice.exception;

public class AccountDeletedException extends AuthenticationException {
	private static final long serialVersionUID = -6043357209505991129L;
	
	public AccountDeletedException() {
		super(ERROR_CODE.AccountDeleted);
	}

}

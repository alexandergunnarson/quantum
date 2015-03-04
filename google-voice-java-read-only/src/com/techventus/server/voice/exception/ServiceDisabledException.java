package com.techventus.server.voice.exception;

public class ServiceDisabledException extends AuthenticationException {
	private static final long serialVersionUID = -5095735710095958867L;

	public ServiceDisabledException() {
		super(ERROR_CODE.ServiceDisabled);
	}
}

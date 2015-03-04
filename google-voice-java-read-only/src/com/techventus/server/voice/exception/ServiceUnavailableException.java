package com.techventus.server.voice.exception;

public class ServiceUnavailableException extends AuthenticationException {
	private static final long serialVersionUID = 2876935821178126326L;

	public ServiceUnavailableException() {
		super(ERROR_CODE.ServiceUnavailable);
	}
}

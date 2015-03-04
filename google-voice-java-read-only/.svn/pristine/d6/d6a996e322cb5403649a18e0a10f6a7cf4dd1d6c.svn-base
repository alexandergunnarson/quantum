package com.techventus.server.voice.exception;

public class CaptchaRequiredException extends AuthenticationException {
	private static final long serialVersionUID = -8965049712183837356L;
	
	/**
	 * Token representing the specific Captcha challenge. Google supplies this token and the 
	 * CAPTCHA image URL in a login failed response with the error code "CaptchaRequired".
	 */
	private String captchaToken = null;

	/**
	 * URL of the Captcha image.
	 */
	private String captchaUrl = null;
	
	
	
	public CaptchaRequiredException(String captchaToken, String captchaUrl) {
		super(ERROR_CODE.CaptchaRequired);
		this.captchaToken = captchaToken;
		this.captchaUrl = captchaUrl;
	}
	
	
	/**
	 * @return Token representing the specific Captcha challenge. Google supplies this token 
	 * and the CAPTCHA image URL in a login failed response with the error code 
	 * "CaptchaRequired".
	 */
	public String getCaptchaToken() {
		return captchaToken;
	}

	/**
	 * @return URL of the Captcha image.
	 */
	public String getCaptchaUrl() {
		return captchaUrl;
	}
	
	
}

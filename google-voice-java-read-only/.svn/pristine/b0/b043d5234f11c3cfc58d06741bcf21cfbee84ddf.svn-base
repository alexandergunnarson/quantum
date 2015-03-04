/**
 * 
 */
package com.techventus.server.voice.exception;

public enum ERROR_CODE{
	BadAuthentication(	"Wrong username or password."),
	NotVerified(		"The account email address has not been verified. You need to access your Google account directly to resolve the issue before logging in using google-voice-java."),
	TermsNotAgreed(		"You have not agreed to terms. You need to access your Google account directly to resolve the issue before logging in using google-voice-java."),
	CaptchaRequired(	"A CAPTCHA is required. (A response with this error code will also contain an image URL and a CAPTCHA token.)"),
	Unknown(			"Unknown or unspecified error; the request contained invalid input or was malformed."),
	AccountDeleted(		"The user account has been deleted."),
	AccountDisabled(	"The user account has been disabled."),
	ServiceDisabled(	"Your access to the voice service has been disabled. (Your user account may still be valid.)"),
	ServiceUnavailable(	"The service is not available; try again later.");
	ERROR_CODE(String pLongText) {
		LONG_TEXT = pLongText;
	}
	public final String LONG_TEXT;
}
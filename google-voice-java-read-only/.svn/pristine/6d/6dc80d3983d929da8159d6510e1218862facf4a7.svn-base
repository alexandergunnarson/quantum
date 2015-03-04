/**
 * AuthenticationException.java
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.techventus.server.voice.exception;

import java.io.IOException;


public class AuthenticationException extends IOException{
	
	private static final long serialVersionUID = 5375401570657384394L;
	protected ERROR_CODE error;
	
	public AuthenticationException() {
	}
	
	public AuthenticationException(ERROR_CODE errorCode) {
		super(errorCode.LONG_TEXT);
		error = errorCode;
	}

	public AuthenticationException(Throwable cause, ERROR_CODE errorCode) {
		//super(errorCode.LONG_TEXT, cause); Java5 no cause
		super(errorCode.LONG_TEXT);
		error = errorCode;
	}

	public static void throwProperException(ERROR_CODE errorCode, String captchaToken, String captchaUrl) throws AuthenticationException {
		switch (errorCode) {
			case AccountDeleted:
				throw new AccountDeletedException();
			case AccountDisabled:
				throw new AccountDisabledException();
			case BadAuthentication:
				throw new BadAuthenticationException();
			case CaptchaRequired:
				throw new CaptchaRequiredException(captchaToken, captchaUrl);
			case NotVerified:
				throw new NotVerifiedException();
			case ServiceDisabled:
				throw new ServiceDisabledException();
			case ServiceUnavailable:
				throw new ServiceUnavailableException();
			case TermsNotAgreed:
				throw new TermsNotAgreedException();
			default:
				throw new AuthenticationException(errorCode);
		}
	}
	
	public void setErrorCode(String errorCodeString) {
		error = ERROR_CODE.valueOf(errorCodeString);
	}
	
	public void setErrorCode(ERROR_CODE errorCode) {
		error = errorCode;
	}
	
	public ERROR_CODE getError() {
		return error;
	}
}


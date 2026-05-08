package com.english.auth;

public class AuthException extends RuntimeException {

	private static final String AUTHENTICATION_FAILURE_MESSAGE = "이메일 또는 비밀번호가 올바르지 않습니다";
	private static final String INVALID_TOKEN_MESSAGE = "인증 토큰이 올바르지 않습니다";

	private final AuthErrorCode errorCode;

	public AuthException(AuthErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public static AuthException shortPassword() {
		return new AuthException(AuthErrorCode.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다");
	}

	public static AuthException duplicateEmail() {
		return new AuthException(AuthErrorCode.DUPLICATE, "이미 등록된 이메일입니다");
	}

	public static AuthException authenticationFailed() {
		return new AuthException(AuthErrorCode.UNAUTHORIZED, AUTHENTICATION_FAILURE_MESSAGE);
	}

	public static AuthException invalidToken() {
		return new AuthException(AuthErrorCode.UNAUTHORIZED, INVALID_TOKEN_MESSAGE);
	}

	public AuthErrorCode getErrorCode() {
		return errorCode;
	}
}

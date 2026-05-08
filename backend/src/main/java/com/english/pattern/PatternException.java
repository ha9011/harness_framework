package com.english.pattern;

public class PatternException extends RuntimeException {

	private final PatternErrorCode errorCode;

	private PatternException(PatternErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public static PatternException badRequest(String message) {
		return new PatternException(PatternErrorCode.BAD_REQUEST, message);
	}

	public static PatternException duplicate() {
		return new PatternException(PatternErrorCode.DUPLICATE, "이미 등록된 패턴입니다");
	}

	public static PatternException notFound() {
		return new PatternException(PatternErrorCode.NOT_FOUND, "패턴을 찾을 수 없습니다");
	}

	public static PatternException forbidden() {
		return new PatternException(PatternErrorCode.FORBIDDEN, "패턴에 접근할 권한이 없습니다");
	}

	public PatternErrorCode getErrorCode() {
		return errorCode;
	}
}

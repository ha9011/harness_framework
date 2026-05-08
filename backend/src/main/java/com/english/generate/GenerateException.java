package com.english.generate;

public class GenerateException extends RuntimeException {

	private final GenerateErrorCode errorCode;

	private GenerateException(GenerateErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public static GenerateException badRequest(String message) {
		return new GenerateException(GenerateErrorCode.BAD_REQUEST, message);
	}

	public static GenerateException notFound(String message) {
		return new GenerateException(GenerateErrorCode.NOT_FOUND, message);
	}

	public static GenerateException forbidden(String message) {
		return new GenerateException(GenerateErrorCode.FORBIDDEN, message);
	}

	public static GenerateException invalidImageFormat() {
		return new GenerateException(GenerateErrorCode.INVALID_IMAGE_FORMAT, "지원하지 않는 이미지 형식입니다");
	}

	public static GenerateException noWords() {
		return new GenerateException(GenerateErrorCode.NO_WORDS, "예문 생성에 사용할 단어가 없습니다");
	}

	public static GenerateException noPatterns() {
		return new GenerateException(GenerateErrorCode.NO_PATTERNS, "예문 생성에 사용할 패턴이 없습니다");
	}

	public static GenerateException aiServiceError() {
		return new GenerateException(GenerateErrorCode.AI_SERVICE_ERROR, "예문 생성에 실패했습니다");
	}

	public static GenerateException aiServiceError(String message) {
		return new GenerateException(GenerateErrorCode.AI_SERVICE_ERROR, message);
	}

	public GenerateErrorCode getErrorCode() {
		return errorCode;
	}
}

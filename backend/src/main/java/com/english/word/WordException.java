package com.english.word;

public class WordException extends RuntimeException {

	private final WordErrorCode errorCode;

	private WordException(WordErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public static WordException badRequest(String message) {
		return new WordException(WordErrorCode.BAD_REQUEST, message);
	}

	public static WordException duplicate() {
		return new WordException(WordErrorCode.DUPLICATE, "이미 등록된 단어입니다");
	}

	public static WordException notFound() {
		return new WordException(WordErrorCode.NOT_FOUND, "단어를 찾을 수 없습니다");
	}

	public static WordException forbidden() {
		return new WordException(WordErrorCode.FORBIDDEN, "단어에 접근할 권한이 없습니다");
	}

	public WordErrorCode getErrorCode() {
		return errorCode;
	}
}

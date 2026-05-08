package com.english.review;

public class ReviewException extends RuntimeException {

	private final ReviewErrorCode errorCode;

	private ReviewException(ReviewErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public static ReviewException badRequest(String message) {
		return new ReviewException(ReviewErrorCode.BAD_REQUEST, message);
	}

	public static ReviewException notFound() {
		return new ReviewException(ReviewErrorCode.NOT_FOUND, "복습 항목을 찾을 수 없습니다");
	}

	public static ReviewException forbidden() {
		return new ReviewException(ReviewErrorCode.FORBIDDEN, "복습 항목에 접근할 권한이 없습니다");
	}

	public ReviewErrorCode getErrorCode() {
		return errorCode;
	}
}

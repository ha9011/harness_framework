package com.english.setting;

public class SettingException extends RuntimeException {

	private final SettingErrorCode errorCode;

	private SettingException(SettingErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public static SettingException unsupportedKey() {
		return new SettingException(SettingErrorCode.BAD_REQUEST, "지원하지 않는 설정입니다");
	}

	public static SettingException invalidDailyReviewCount() {
		return new SettingException(SettingErrorCode.BAD_REQUEST, "daily_review_count는 10, 20, 30만 허용됩니다");
	}

	public static SettingException badRequest(String message) {
		return new SettingException(SettingErrorCode.BAD_REQUEST, message);
	}

	public SettingErrorCode getErrorCode() {
		return errorCode;
	}
}

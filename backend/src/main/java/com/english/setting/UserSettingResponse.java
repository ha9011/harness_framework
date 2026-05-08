package com.english.setting;

public record UserSettingResponse(int dailyReviewCount) {

	public static UserSettingResponse from(UserSetting setting) {
		return new UserSettingResponse(setting.getDailyReviewCount());
	}
}

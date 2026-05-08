package com.english.setting;

import com.english.auth.AuthException;
import com.english.auth.User;
import com.english.auth.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingService {

	private static final String DAILY_REVIEW_COUNT_KEY = "daily_review_count";
	private static final int DEFAULT_DAILY_REVIEW_COUNT = 10;

	private final UserSettingRepository userSettingRepository;
	private final UserRepository userRepository;

	public UserSettingService(
			UserSettingRepository userSettingRepository,
			UserRepository userRepository
	) {
		this.userSettingRepository = userSettingRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public UserSettingResponse getSettings(Long userId) {
		return UserSettingResponse.from(settingFor(userId));
	}

	@Transactional
	public UserSettingResponse updateSetting(Long userId, String key, String value) {
		if (!DAILY_REVIEW_COUNT_KEY.equals(key)) {
			throw SettingException.unsupportedKey();
		}

		UserSetting setting = settingFor(userId);
		setting.updateDailyReviewCount(parseDailyReviewCount(value));
		return UserSettingResponse.from(setting);
	}

	@Transactional
	public int getDailyReviewCount(Long userId) {
		return settingFor(userId).getDailyReviewCount();
	}

	private UserSetting settingFor(Long userId) {
		Long requestedUserId = requireUserId(userId);
		return userSettingRepository.findByUserId(requestedUserId)
				.orElseGet(() -> userSettingRepository.save(new UserSetting(
						findUser(requestedUserId),
						DEFAULT_DAILY_REVIEW_COUNT)));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(AuthException::invalidToken);
	}

	private static int parseDailyReviewCount(String value) {
		if (value == null || value.isBlank()) {
			throw SettingException.invalidDailyReviewCount();
		}
		try {
			int dailyReviewCount = Integer.parseInt(value.trim());
			if (dailyReviewCount == 10 || dailyReviewCount == 20 || dailyReviewCount == 30) {
				return dailyReviewCount;
			}
		}
		catch (NumberFormatException exception) {
			throw SettingException.invalidDailyReviewCount();
		}
		throw SettingException.invalidDailyReviewCount();
	}

	private static Long requireUserId(Long userId) {
		if (userId == null || userId <= 0) {
			throw SettingException.badRequest("사용자 ID는 필수입니다");
		}
		return userId;
	}
}

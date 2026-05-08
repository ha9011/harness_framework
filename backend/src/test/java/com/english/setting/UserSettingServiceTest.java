package com.english.setting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.english.auth.User;
import com.english.auth.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserSettingServiceTest {

	private final UserSettingService userSettingService;
	private final UserSettingRepository userSettingRepository;
	private final UserRepository userRepository;
	private final EntityManager entityManager;

	@Autowired
	UserSettingServiceTest(
			UserSettingService userSettingService,
			UserSettingRepository userSettingRepository,
			UserRepository userRepository,
			EntityManager entityManager
	) {
		this.userSettingService = userSettingService;
		this.userSettingRepository = userSettingRepository;
		this.userRepository = userRepository;
		this.entityManager = entityManager;
	}

	@Test
	void getSettingsCreatesDefaultDailyReviewCountWhenMissing() {
		User user = saveUser("settings-default@example.com");

		UserSettingResponse response = userSettingService.getSettings(user.getId());
		UserSettingResponse secondResponse = userSettingService.getSettings(user.getId());
		flushAndClear();

		assertThat(response.dailyReviewCount()).isEqualTo(10);
		assertThat(secondResponse.dailyReviewCount()).isEqualTo(10);
		assertThat(userSettingRepository.findByUserId(user.getId()))
				.isPresent()
				.get()
				.extracting(UserSetting::getDailyReviewCount)
				.isEqualTo(10);
		assertThat(settingsForUser(user.getId())).hasSize(1);
	}

	@Test
	void updateSettingAllowsOnlyDailyReviewCountAndAcceptedValues() {
		User user = saveUser("settings-update@example.com");

		UserSettingResponse twenty = userSettingService.updateSetting(user.getId(), "daily_review_count", "20");
		UserSettingResponse thirty = userSettingService.updateSetting(user.getId(), "daily_review_count", "30");
		UserSettingResponse ten = userSettingService.updateSetting(user.getId(), "daily_review_count", "10");
		flushAndClear();

		assertThat(twenty.dailyReviewCount()).isEqualTo(20);
		assertThat(thirty.dailyReviewCount()).isEqualTo(30);
		assertThat(ten.dailyReviewCount()).isEqualTo(10);
		assertThat(userSettingRepository.findByUserId(user.getId()).orElseThrow().getDailyReviewCount())
				.isEqualTo(10);
	}

	@Test
	void updateSettingRejectsUnsupportedKeyAndInvalidDailyReviewCount() {
		User user = saveUser("settings-invalid@example.com");

		Throwable unsupportedKey = catchThrowable(() ->
				userSettingService.updateSetting(user.getId(), "notification_enabled", "20"));
		Throwable invalidValue = catchThrowable(() ->
				userSettingService.updateSetting(user.getId(), "daily_review_count", "15"));
		Throwable invalidFormat = catchThrowable(() ->
				userSettingService.updateSetting(user.getId(), "daily_review_count", "twenty"));

		assertThat(unsupportedKey)
				.isInstanceOf(SettingException.class)
				.hasMessage("지원하지 않는 설정입니다");
		assertThat(((SettingException) unsupportedKey).getErrorCode()).isEqualTo(SettingErrorCode.BAD_REQUEST);
		assertThat(invalidValue)
				.isInstanceOf(SettingException.class)
				.hasMessage("daily_review_count는 10, 20, 30만 허용됩니다");
		assertThat(((SettingException) invalidValue).getErrorCode()).isEqualTo(SettingErrorCode.BAD_REQUEST);
		assertThat(invalidFormat)
				.isInstanceOf(SettingException.class)
				.hasMessage("daily_review_count는 10, 20, 30만 허용됩니다");
		assertThat(((SettingException) invalidFormat).getErrorCode()).isEqualTo(SettingErrorCode.BAD_REQUEST);
	}

	@Test
	void settingsAreIsolatedByUser() {
		User owner = saveUser("settings-owner@example.com");
		User otherUser = saveUser("settings-other@example.com");

		userSettingService.updateSetting(owner.getId(), "daily_review_count", "20");
		UserSettingResponse otherSettings = userSettingService.getSettings(otherUser.getId());
		flushAndClear();

		assertThat(userSettingRepository.findByUserId(owner.getId()).orElseThrow().getDailyReviewCount())
				.isEqualTo(20);
		assertThat(userSettingRepository.findByUserId(otherUser.getId()).orElseThrow().getDailyReviewCount())
				.isEqualTo(10);
		assertThat(otherSettings.dailyReviewCount()).isEqualTo(10);
	}

	@Test
	void getDailyReviewCountReturnsPersistedValueAndCreatesDefaultWhenMissing() {
		User defaultUser = saveUser("settings-review-default@example.com");
		User customUser = saveUser("settings-review-custom@example.com");
		userSettingService.updateSetting(customUser.getId(), "daily_review_count", "30");

		int defaultCount = userSettingService.getDailyReviewCount(defaultUser.getId());
		int customCount = userSettingService.getDailyReviewCount(customUser.getId());
		flushAndClear();

		assertThat(defaultCount).isEqualTo(10);
		assertThat(customCount).isEqualTo(30);
		assertThat(userSettingRepository.findByUserId(defaultUser.getId())).isPresent();
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}

	private List<UserSetting> settingsForUser(Long userId) {
		return userSettingRepository.findAll()
				.stream()
				.filter(setting -> setting.getUser().getId().equals(userId))
				.toList();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}

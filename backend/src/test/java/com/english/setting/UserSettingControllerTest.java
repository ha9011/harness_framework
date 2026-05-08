package com.english.setting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.auth.AuthResult;
import com.english.auth.AuthService;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
		"auth.jwt.secret=word-controller-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT24H"
})
class UserSettingControllerTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final AuthService authService;
	private final UserSettingRepository userSettingRepository;

	@Autowired
	UserSettingControllerTest(
			MockMvc mockMvc,
			AuthService authService,
			UserSettingRepository userSettingRepository
	) {
		this.mockMvc = mockMvc;
		this.authService = authService;
		this.userSettingRepository = userSettingRepository;
	}

	@Test
	void settingsApisRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/settings"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

		mockMvc.perform(put("/api/settings/daily_review_count")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": 20
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void getSettingsReturnsDefaultForCurrentUser() throws Exception {
		AuthResult user = signup("settings-get-default");

		mockMvc.perform(get("/api/settings")
						.cookie(tokenCookie(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dailyReviewCount").value(10));

		org.assertj.core.api.Assertions.assertThat(userSettingRepository.findByUserId(user.user().id()))
				.isPresent()
				.get()
				.extracting(UserSetting::getDailyReviewCount)
				.isEqualTo(10);
	}

	@Test
	void updateDailyReviewCountAcceptsAllowedStringAndNumericValues() throws Exception {
		AuthResult user = signup("settings-update");

		mockMvc.perform(put("/api/settings/daily_review_count")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": 10
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dailyReviewCount").value(10));

		mockMvc.perform(put("/api/settings/daily_review_count")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": "20"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dailyReviewCount").value(20));

		mockMvc.perform(put("/api/settings/daily_review_count")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": 30
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dailyReviewCount").value(30));

		mockMvc.perform(get("/api/settings")
						.cookie(tokenCookie(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dailyReviewCount").value(30));
	}

	@Test
	void invalidSettingKeyAndValueReturnBadRequest() throws Exception {
		AuthResult user = signup("settings-invalid");

		mockMvc.perform(put("/api/settings/notification_enabled")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": 20
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.message").value("지원하지 않는 설정입니다"));

		mockMvc.perform(put("/api/settings/daily_review_count")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": 15
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.message").value("daily_review_count는 10, 20, 30만 허용됩니다"));

		mockMvc.perform(put("/api/settings/daily_review_count")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": "twenty"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.message").value("daily_review_count는 10, 20, 30만 허용됩니다"));
	}

	@Test
	void settingsAreScopedToCurrentAuthenticatedUser() throws Exception {
		AuthResult owner = signup("settings-owner");
		AuthResult otherUser = signup("settings-other");

		mockMvc.perform(put("/api/settings/daily_review_count")
						.cookie(tokenCookie(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "value": 20
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dailyReviewCount").value(20));

		mockMvc.perform(get("/api/settings")
						.cookie(tokenCookie(otherUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dailyReviewCount").value(10));

		mockMvc.perform(get("/api/settings")
						.cookie(tokenCookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dailyReviewCount").value(20));
	}

	private AuthResult signup(String prefix) {
		return authService.signup(prefix + "-" + UUID.randomUUID() + "@example.com", "password123", "tester");
	}

	private static Cookie tokenCookie(AuthResult result) {
		return new Cookie(TOKEN_COOKIE, result.token());
	}
}

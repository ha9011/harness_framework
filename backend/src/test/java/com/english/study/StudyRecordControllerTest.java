package com.english.study;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.auth.AuthResult;
import com.english.auth.AuthService;
import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.word.Word;
import com.english.word.WordRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class StudyRecordControllerTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final AuthService authService;
	private final UserRepository userRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;
	private final StudyRecordService studyRecordService;

	@Autowired
	StudyRecordControllerTest(
			MockMvc mockMvc,
			AuthService authService,
			UserRepository userRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository,
			StudyRecordService studyRecordService
	) {
		this.mockMvc = mockMvc;
		this.authService = authService;
		this.userRepository = userRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
		this.studyRecordService = studyRecordService;
	}

	@Test
	void studyRecordApiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/study-records"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void listStudyRecordsReturnsPagedLatestRecordsWithItemsInCurrentUserScope() throws Exception {
		AuthResult owner = signup("study-list-owner");
		AuthResult otherUser = signup("study-list-other");
		User ownerEntity = userEntity(owner);
		User otherUserEntity = userEntity(otherUser);
		Word coffee = wordRepository.save(new Word(ownerEntity, "brew coffee", "커피를 내리다"));
		Word latte = wordRepository.save(new Word(ownerEntity, "latte", "라떼"));
		Pattern pattern = patternRepository.save(new Pattern(ownerEntity, "I used to...", "과거 습관"));
		Word otherUsersWord = wordRepository.save(new Word(otherUserEntity, "other word", "다른 사용자 단어"));

		studyRecordService.recordLearning(ownerEntity, StudyItemType.WORD, coffee.getId(), LocalDate.of(2026, 5, 1));
		studyRecordService.recordLearning(ownerEntity, StudyItemType.PATTERN, pattern.getId(), LocalDate.of(2026, 5, 1));
		studyRecordService.recordLearning(ownerEntity, StudyItemType.WORD, latte.getId(), LocalDate.of(2026, 5, 8));
		studyRecordService.recordLearning(
				otherUserEntity,
				StudyItemType.WORD,
				otherUsersWord.getId(),
				LocalDate.of(2026, 5, 9));

		mockMvc.perform(get("/api/study-records")
						.cookie(tokenCookie(owner))
						.param("page", "0")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].studyDate").value("2026-05-08"))
				.andExpect(jsonPath("$.content[0].dayNumber").value(2))
				.andExpect(jsonPath("$.content[0].items.length()").value(1))
				.andExpect(jsonPath("$.content[0].items[0].type").value("WORD"))
				.andExpect(jsonPath("$.content[0].items[0].id").value(latte.getId()))
				.andExpect(jsonPath("$.content[0].items[0].name").value("latte"));

		mockMvc.perform(get("/api/study-records")
						.cookie(tokenCookie(owner))
						.param("page", "1")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.content[0].studyDate").value("2026-05-01"))
				.andExpect(jsonPath("$.content[0].dayNumber").value(1))
				.andExpect(jsonPath("$.content[0].items.length()").value(2))
				.andExpect(jsonPath("$.content[0].items[0].type").value("WORD"))
				.andExpect(jsonPath("$.content[0].items[0].id").value(coffee.getId()))
				.andExpect(jsonPath("$.content[0].items[0].name").value("brew coffee"))
				.andExpect(jsonPath("$.content[0].items[1].type").value("PATTERN"))
				.andExpect(jsonPath("$.content[0].items[1].id").value(pattern.getId()))
				.andExpect(jsonPath("$.content[0].items[1].name").value("I used to..."));
	}

	@Test
	void invalidPaginationReturnsBadRequest() throws Exception {
		AuthResult user = signup("study-validation");

		mockMvc.perform(get("/api/study-records")
						.cookie(tokenCookie(user))
						.param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(get("/api/study-records")
						.cookie(tokenCookie(user))
						.param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	private AuthResult signup(String prefix) {
		return authService.signup(prefix + "-" + UUID.randomUUID() + "@example.com", "password123", "tester");
	}

	private User userEntity(AuthResult result) {
		return userRepository.findById(result.user().id()).orElseThrow();
	}

	private static Cookie tokenCookie(AuthResult result) {
		return new Cookie(TOKEN_COOKIE, result.token());
	}
}

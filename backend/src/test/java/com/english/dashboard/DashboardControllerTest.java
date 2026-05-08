package com.english.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.auth.AuthResult;
import com.english.auth.AuthService;
import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.generate.GeneratedSentence;
import com.english.generate.GeneratedSentenceRepository;
import com.english.generate.GenerationHistory;
import com.english.generate.GenerationHistoryRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.review.ReviewDirection;
import com.english.review.ReviewItem;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemType;
import com.english.review.ReviewLog;
import com.english.review.ReviewLogRepository;
import com.english.review.ReviewResult;
import com.english.study.StudyItemType;
import com.english.study.StudyRecordService;
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
class DashboardControllerTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final AuthService authService;
	private final UserRepository userRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;
	private final GenerationHistoryRepository generationHistoryRepository;
	private final GeneratedSentenceRepository generatedSentenceRepository;
	private final ReviewItemRepository reviewItemRepository;
	private final ReviewLogRepository reviewLogRepository;
	private final StudyRecordService studyRecordService;

	@Autowired
	DashboardControllerTest(
			MockMvc mockMvc,
			AuthService authService,
			UserRepository userRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository,
			GenerationHistoryRepository generationHistoryRepository,
			GeneratedSentenceRepository generatedSentenceRepository,
			ReviewItemRepository reviewItemRepository,
			ReviewLogRepository reviewLogRepository,
			StudyRecordService studyRecordService
	) {
		this.mockMvc = mockMvc;
		this.authService = authService;
		this.userRepository = userRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
		this.generationHistoryRepository = generationHistoryRepository;
		this.generatedSentenceRepository = generatedSentenceRepository;
		this.reviewItemRepository = reviewItemRepository;
		this.reviewLogRepository = reviewLogRepository;
		this.studyRecordService = studyRecordService;
	}

	@Test
	void dashboardApiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/dashboard"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void getDashboardReturnsCurrentUsersAggregatedData() throws Exception {
		AuthResult owner = signup("dashboard-owner");
		AuthResult otherUser = signup("dashboard-other");
		User ownerEntity = userEntity(owner);
		User otherUserEntity = userEntity(otherUser);

		Word coffee = wordRepository.save(new Word(ownerEntity, "brew coffee", "커피를 내리다"));
		Word latte = wordRepository.save(new Word(ownerEntity, "latte", "라떼"));
		wordRepository.save(new Word(otherUserEntity, "other word", "다른 사용자 단어"));
		Pattern usedTo = patternRepository.save(new Pattern(ownerEntity, "I used to...", "과거 습관"));
		Pattern afraidThat = patternRepository.save(new Pattern(ownerEntity, "I'm afraid that...", "유감스럽게도"));
		patternRepository.save(new Pattern(otherUserEntity, "Other pattern", "다른 사용자 패턴"));
		GeneratedSentence sentence = saveSentence(ownerEntity, usedTo, "I used to brew coffee.", "나는 커피를 내리곤 했다.");
		saveSentence(otherUserEntity, null, "Other sentence.", "다른 사용자 문장");

		saveReviewItem(ownerEntity, ReviewItemType.WORD, coffee.getId(), ReviewDirection.RECOGNITION, LocalDate.now());
		saveReviewItem(ownerEntity, ReviewItemType.PATTERN, usedTo.getId(), ReviewDirection.RECOGNITION, LocalDate.now());
		saveReviewItem(ownerEntity, ReviewItemType.SENTENCE, sentence.getId(), ReviewDirection.RECOGNITION, LocalDate.now());
		saveReviewItem(otherUserEntity, ReviewItemType.WORD, coffee.getId(), ReviewDirection.RECOGNITION, LocalDate.now());
		ReviewItem reviewedToday = saveReviewItem(
				ownerEntity,
				ReviewItemType.WORD,
				latte.getId(),
				ReviewDirection.RECALL,
				LocalDate.now());
		reviewLogRepository.save(new ReviewLog(reviewedToday, ReviewResult.EASY));

		for (int day = 1; day <= 6; day++) {
			LocalDate studyDate = LocalDate.of(2026, 5, day);
			if (day % 2 == 0) {
				studyRecordService.recordLearning(ownerEntity, StudyItemType.PATTERN, afraidThat.getId(), studyDate);
			} else {
				studyRecordService.recordLearning(ownerEntity, StudyItemType.WORD, coffee.getId(), studyDate);
			}
		}
		Word otherUsersLatestWord = wordRepository.save(new Word(otherUserEntity, "latest other", "다른 최신 단어"));
		studyRecordService.recordLearning(
				otherUserEntity,
				StudyItemType.WORD,
				otherUsersLatestWord.getId(),
				LocalDate.of(2026, 5, 7));

		mockMvc.perform(get("/api/dashboard")
						.cookie(tokenCookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.wordCount").value(2))
				.andExpect(jsonPath("$.patternCount").value(2))
				.andExpect(jsonPath("$.sentenceCount").value(1))
				.andExpect(jsonPath("$.streak").value(1))
				.andExpect(jsonPath("$.todayReviewRemaining.word").value(2))
				.andExpect(jsonPath("$.todayReviewRemaining.pattern").value(1))
				.andExpect(jsonPath("$.todayReviewRemaining.sentence").value(1))
				.andExpect(jsonPath("$.recentStudyRecords.length()").value(5))
				.andExpect(jsonPath("$.recentStudyRecords[0].studyDate").value("2026-05-06"))
				.andExpect(jsonPath("$.recentStudyRecords[0].dayNumber").value(6))
				.andExpect(jsonPath("$.recentStudyRecords[0].items[0].type").value("PATTERN"))
				.andExpect(jsonPath("$.recentStudyRecords[0].items[0].name").value("I'm afraid that..."))
				.andExpect(jsonPath("$.recentStudyRecords[1].studyDate").value("2026-05-05"))
				.andExpect(jsonPath("$.recentStudyRecords[1].items[0].type").value("WORD"))
				.andExpect(jsonPath("$.recentStudyRecords[1].items[0].name").value("brew coffee"));
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

	private GeneratedSentence saveSentence(User user, Pattern pattern, String sentenceText, String translation) {
		GenerationHistory history = generationHistoryRepository.save(new GenerationHistory(
				user,
				"중등",
				10,
				1,
				null,
				pattern));
		return generatedSentenceRepository.save(new GeneratedSentence(
				user,
				history,
				pattern,
				sentenceText,
				translation,
				"중등"));
	}

	private ReviewItem saveReviewItem(
			User user,
			ReviewItemType itemType,
			Long itemId,
			ReviewDirection direction,
			LocalDate nextReviewDate
	) {
		return reviewItemRepository.save(new ReviewItem(user, itemType, itemId, direction, nextReviewDate));
	}
}

package com.english.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

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
import com.english.setting.UserSetting;
import com.english.setting.UserSettingRepository;
import com.english.study.StudyItemType;
import com.english.study.StudyRecordItemResponse;
import com.english.study.StudyRecordResponse;
import com.english.study.StudyRecordService;
import com.english.word.Word;
import com.english.word.WordRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class DashboardServiceTest {

	private final DashboardService dashboardService;
	private final StudyRecordService studyRecordService;
	private final UserRepository userRepository;
	private final UserSettingRepository userSettingRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;
	private final GenerationHistoryRepository generationHistoryRepository;
	private final GeneratedSentenceRepository generatedSentenceRepository;
	private final ReviewItemRepository reviewItemRepository;
	private final ReviewLogRepository reviewLogRepository;
	private final EntityManager entityManager;

	@Autowired
	DashboardServiceTest(
			DashboardService dashboardService,
			StudyRecordService studyRecordService,
			UserRepository userRepository,
			UserSettingRepository userSettingRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository,
			GenerationHistoryRepository generationHistoryRepository,
			GeneratedSentenceRepository generatedSentenceRepository,
			ReviewItemRepository reviewItemRepository,
			ReviewLogRepository reviewLogRepository,
			EntityManager entityManager
	) {
		this.dashboardService = dashboardService;
		this.studyRecordService = studyRecordService;
		this.userRepository = userRepository;
		this.userSettingRepository = userSettingRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
		this.generationHistoryRepository = generationHistoryRepository;
		this.generatedSentenceRepository = generatedSentenceRepository;
		this.reviewItemRepository = reviewItemRepository;
		this.reviewLogRepository = reviewLogRepository;
		this.entityManager = entityManager;
	}

	@Test
	void getDashboardCountsActiveOwnedLearningDataAndReturnsLatestFiveStudyRecords() {
		User owner = saveUser("dashboard-owner@example.com");
		User otherUser = saveUser("dashboard-other@example.com");
		Word coffee = saveWord(owner, "brew coffee", "커피를 내리다");
		saveWord(owner, "latte", "라떼");
		Word deletedWord = saveWord(owner, "deleted word", "삭제된 단어");
		deletedWord.softDelete();
		saveWord(otherUser, "other word", "다른 사용자 단어");
		Pattern usedTo = savePattern(owner, "I used to...", "과거 습관");
		Pattern deletedPattern = savePattern(owner, "Deleted pattern", "삭제된 패턴");
		deletedPattern.softDelete();
		savePattern(otherUser, "Other pattern", "다른 사용자 패턴");
		saveSentence(owner, "I used to brew coffee.", "나는 커피를 내리곤 했다.");
		GeneratedSentence deletedSentence = saveSentence(owner, "Deleted sentence.", "삭제된 문장");
		softDeleteSentence(deletedSentence);
		saveSentence(owner, "I drink a latte.", "나는 라떼를 마신다.");
		saveSentence(otherUser, "Other sentence.", "다른 사용자 문장");

		for (int day = 1; day <= 6; day++) {
			LocalDate studyDate = LocalDate.of(2026, 5, day);
			if (day % 2 == 0) {
				studyRecordService.recordLearning(owner, StudyItemType.PATTERN, usedTo.getId(), studyDate);
			} else {
				studyRecordService.recordLearning(owner, StudyItemType.WORD, coffee.getId(), studyDate);
			}
		}
		studyRecordService.recordLearning(otherUser, StudyItemType.WORD, saveWord(otherUser, "latest", "최신").getId(),
				LocalDate.of(2026, 5, 10));
		flushAndClear();

		DashboardResponse response = dashboardService.getDashboard(owner.getId());

		assertThat(response.wordCount()).isEqualTo(2);
		assertThat(response.patternCount()).isEqualTo(1);
		assertThat(response.sentenceCount()).isEqualTo(2);
		assertThat(response.todayReviewRemaining()).isEqualTo(new TodayReviewRemainingResponse(0, 0, 0));
		assertThat(response.streak()).isZero();
		assertThat(response.recentStudyRecords()).hasSize(5);
		assertThat(response.recentStudyRecords())
				.extracting(StudyRecordResponse::studyDate, StudyRecordResponse::dayNumber)
				.containsExactly(
						tuple(LocalDate.of(2026, 5, 6), 6),
						tuple(LocalDate.of(2026, 5, 5), 5),
						tuple(LocalDate.of(2026, 5, 4), 4),
						tuple(LocalDate.of(2026, 5, 3), 3),
						tuple(LocalDate.of(2026, 5, 2), 2));
		List<StudyRecordItemResponse> latestItems = response.recentStudyRecords().getFirst().items();
		assertThat(latestItems)
				.extracting(StudyRecordItemResponse::type, StudyRecordItemResponse::id, StudyRecordItemResponse::name)
				.containsExactly(tuple(StudyItemType.PATTERN, usedTo.getId(), "I used to..."));
	}

	@Test
	void todayReviewRemainingCountsActualDueActiveCardsWithoutDailySettingLimit() {
		User owner = saveUser("dashboard-remaining@example.com");
		User otherUser = saveUser("dashboard-remaining-other@example.com");
		userSettingRepository.save(new UserSetting(owner, 10));
		for (int index = 0; index < 12; index++) {
			Word word = saveWord(owner, "word " + index, "뜻 " + index);
			saveReviewItem(owner, ReviewItemType.WORD, word.getId(), ReviewDirection.RECOGNITION, LocalDate.now());
		}
		for (int index = 0; index < 8; index++) {
			Pattern pattern = savePattern(owner, "Pattern " + index, "설명 " + index);
			saveReviewItem(owner, ReviewItemType.PATTERN, pattern.getId(), ReviewDirection.RECOGNITION, LocalDate.now());
		}
		for (int index = 0; index < 13; index++) {
			GeneratedSentence sentence = saveSentence(owner, "Sentence " + index, "문장 " + index);
			saveReviewItem(owner, ReviewItemType.SENTENCE, sentence.getId(), ReviewDirection.RECOGNITION, LocalDate.now());
		}
		Word futureWord = saveWord(owner, "future word", "미래 단어");
		saveReviewItem(owner, ReviewItemType.WORD, futureWord.getId(), ReviewDirection.RECOGNITION,
				LocalDate.now().plusDays(1));
		ReviewItem deletedReviewItem = saveReviewItem(
				owner,
				ReviewItemType.WORD,
				saveWord(owner, "deleted review", "삭제된 카드").getId(),
				ReviewDirection.RECOGNITION,
				LocalDate.now());
		deletedReviewItem.softDelete();
		Word deletedSourceWord = saveWord(owner, "deleted source", "삭제된 원본");
		deletedSourceWord.softDelete();
		saveReviewItem(owner, ReviewItemType.WORD, deletedSourceWord.getId(), ReviewDirection.RECOGNITION,
				LocalDate.now());
		Pattern deletedSourcePattern = savePattern(owner, "Deleted source pattern", "삭제된 패턴");
		deletedSourcePattern.softDelete();
		saveReviewItem(owner, ReviewItemType.PATTERN, deletedSourcePattern.getId(), ReviewDirection.RECOGNITION,
				LocalDate.now());
		GeneratedSentence deletedSourceSentence = saveSentence(owner, "Deleted source sentence", "삭제된 문장");
		softDeleteSentence(deletedSourceSentence);
		saveReviewItem(owner, ReviewItemType.SENTENCE, deletedSourceSentence.getId(), ReviewDirection.RECOGNITION,
				LocalDate.now());
		Word otherWord = saveWord(otherUser, "other due", "다른 사용자");
		saveReviewItem(otherUser, ReviewItemType.WORD, otherWord.getId(), ReviewDirection.RECOGNITION, LocalDate.now());
		flushAndClear();

		DashboardResponse response = dashboardService.getDashboard(owner.getId());

		assertThat(response.todayReviewRemaining().word()).isEqualTo(12);
		assertThat(response.todayReviewRemaining().pattern()).isEqualTo(8);
		assertThat(response.todayReviewRemaining().sentence()).isEqualTo(13);
	}

	@Test
	void streakUsesOnlyCurrentUsersReviewLogDates() {
		User owner = saveUser("dashboard-streak@example.com");
		User otherUser = saveUser("dashboard-streak-other@example.com");
		ReviewItem ownerItem = saveReviewItem(
				owner,
				ReviewItemType.WORD,
				saveWord(owner, "streak word", "연속 단어").getId(),
				ReviewDirection.RECOGNITION,
				LocalDate.now());
		ReviewItem otherItem = saveReviewItem(
				otherUser,
				ReviewItemType.WORD,
				saveWord(otherUser, "other streak", "다른 사용자 연속").getId(),
				ReviewDirection.RECOGNITION,
				LocalDate.now());
		LocalDate today = LocalDate.now();
		saveReviewLogAt(ownerItem, today);
		saveReviewLogAt(ownerItem, today.minusDays(1));
		saveReviewLogAt(ownerItem, today.minusDays(2));
		saveReviewLogAt(ownerItem, today.minusDays(4));
		saveReviewLogAt(otherItem, today.minusDays(3));
		flushAndClear();

		DashboardResponse response = dashboardService.getDashboard(owner.getId());

		assertThat(response.streak()).isEqualTo(3);
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}

	private Word saveWord(User user, String wordText, String meaning) {
		return wordRepository.save(new Word(user, wordText, meaning));
	}

	private Pattern savePattern(User user, String template, String description) {
		return patternRepository.save(new Pattern(user, template, description));
	}

	private GeneratedSentence saveSentence(User user, String sentenceText, String translation) {
		GenerationHistory history = generationHistoryRepository.save(new GenerationHistory(
				user,
				"중등",
				10,
				1,
				null,
				null));
		return generatedSentenceRepository.save(new GeneratedSentence(
				user,
				history,
				null,
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

	private void saveReviewLogAt(ReviewItem reviewItem, LocalDate reviewedDate) {
		ReviewLog reviewLog = reviewLogRepository.save(new ReviewLog(reviewItem, ReviewResult.EASY));
		entityManager.flush();
		Instant reviewedAt = reviewedDate.atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant();
		entityManager.createNativeQuery("""
				update review_logs
				set reviewed_at = :reviewedAt
				where id = :id
				""")
				.setParameter("reviewedAt", reviewedAt)
				.setParameter("id", reviewLog.getId())
				.executeUpdate();
	}

	private void softDeleteSentence(GeneratedSentence sentence) {
		entityManager.flush();
		entityManager.createNativeQuery("""
				update generated_sentences
				set deleted = true
				where id = :id
				""")
				.setParameter("id", sentence.getId())
				.executeUpdate();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}

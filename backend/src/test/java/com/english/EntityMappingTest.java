package com.english;

import static org.assertj.core.api.Assertions.assertThat;

import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.generate.GeneratedSentence;
import com.english.generate.GeneratedSentenceRepository;
import com.english.generate.GeneratedSentenceWord;
import com.english.generate.GeneratedSentenceWordRepository;
import com.english.generate.GenerationHistory;
import com.english.generate.GenerationHistoryRepository;
import com.english.generate.SentenceSituation;
import com.english.generate.SentenceSituationRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternExample;
import com.english.pattern.PatternExampleRepository;
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
import com.english.study.StudyRecord;
import com.english.study.StudyRecordItem;
import com.english.study.StudyRecordItemRepository;
import com.english.study.StudyRecordRepository;
import com.english.word.Word;
import com.english.word.WordRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EntityMappingTest {

	private final EntityManager entityManager;
	private final UserRepository userRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;
	private final PatternExampleRepository patternExampleRepository;
	private final GenerationHistoryRepository generationHistoryRepository;
	private final GeneratedSentenceRepository generatedSentenceRepository;
	private final GeneratedSentenceWordRepository generatedSentenceWordRepository;
	private final SentenceSituationRepository sentenceSituationRepository;
	private final StudyRecordRepository studyRecordRepository;
	private final StudyRecordItemRepository studyRecordItemRepository;
	private final ReviewItemRepository reviewItemRepository;
	private final ReviewLogRepository reviewLogRepository;
	private final UserSettingRepository userSettingRepository;

	@Autowired
	EntityMappingTest(
			EntityManager entityManager,
			UserRepository userRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository,
			PatternExampleRepository patternExampleRepository,
			GenerationHistoryRepository generationHistoryRepository,
			GeneratedSentenceRepository generatedSentenceRepository,
			GeneratedSentenceWordRepository generatedSentenceWordRepository,
			SentenceSituationRepository sentenceSituationRepository,
			StudyRecordRepository studyRecordRepository,
			StudyRecordItemRepository studyRecordItemRepository,
			ReviewItemRepository reviewItemRepository,
			ReviewLogRepository reviewLogRepository,
			UserSettingRepository userSettingRepository
	) {
		this.entityManager = entityManager;
		this.userRepository = userRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
		this.patternExampleRepository = patternExampleRepository;
		this.generationHistoryRepository = generationHistoryRepository;
		this.generatedSentenceRepository = generatedSentenceRepository;
		this.generatedSentenceWordRepository = generatedSentenceWordRepository;
		this.sentenceSituationRepository = sentenceSituationRepository;
		this.studyRecordRepository = studyRecordRepository;
		this.studyRecordItemRepository = studyRecordItemRepository;
		this.reviewItemRepository = reviewItemRepository;
		this.reviewLogRepository = reviewLogRepository;
		this.userSettingRepository = userSettingRepository;
	}

	@Test
	void persistsWordPatternAndStudyRecordMappings() {
		User user = saveUser("mapping1@example.com");
		Word word = wordRepository.save(new Word(user, "make the bed", "tidy the bed"));
		Pattern pattern = patternRepository.save(new Pattern(user, "I'm afraid that...", "softens bad news"));
		PatternExample example = patternExampleRepository.save(new PatternExample(
				pattern,
				1,
				"I'm afraid that we'll be late.",
				"We will be late."));
		StudyRecord studyRecord = studyRecordRepository.save(new StudyRecord(user, LocalDate.of(2026, 5, 8), 1));
		StudyRecordItem studyRecordItem = studyRecordItemRepository.save(new StudyRecordItem(
				studyRecord,
				StudyItemType.WORD,
				word.getId()));

		entityManager.flush();
		entityManager.clear();

		Word foundWord = wordRepository.findById(word.getId()).orElseThrow();
		Pattern foundPattern = patternRepository.findById(pattern.getId()).orElseThrow();
		PatternExample foundExample = patternExampleRepository.findById(example.getId()).orElseThrow();
		StudyRecordItem foundStudyRecordItem = studyRecordItemRepository.findById(studyRecordItem.getId()).orElseThrow();

		assertThat(foundWord.getUser().getId()).isEqualTo(user.getId());
		assertThat(foundWord.isImportant()).isFalse();
		assertThat(foundWord.isDeleted()).isFalse();
		assertThat(foundWord.getCreatedAt()).isNotNull();
		assertThat(foundWord.getUpdatedAt()).isNotNull();
		assertThat(foundPattern.getTemplate()).isEqualTo("I'm afraid that...");
		assertThat(foundPattern.isDeleted()).isFalse();
		assertThat(foundExample.getPattern().getId()).isEqualTo(pattern.getId());
		assertThat(foundExample.getSortOrder()).isEqualTo(1);
		assertThat(foundStudyRecordItem.getStudyRecord().getId()).isEqualTo(studyRecord.getId());
		assertThat(foundStudyRecordItem.getItemType()).isEqualTo(StudyItemType.WORD);
		assertThat(foundStudyRecordItem.getItemId()).isEqualTo(word.getId());
	}

	@Test
	void persistsGeneratedSentenceMappings() {
		User user = saveUser("mapping2@example.com");
		Word word = wordRepository.save(new Word(user, "brew coffee", "make coffee"));
		Pattern pattern = patternRepository.save(new Pattern(user, "I tend to...", "usually do something"));
		GenerationHistory history = generationHistoryRepository.save(new GenerationHistory(
				user,
				"MIDDLE",
				10,
				1,
				word,
				pattern));
		GeneratedSentence sentence = generatedSentenceRepository.save(new GeneratedSentence(
				user,
				history,
				pattern,
				"I tend to brew coffee before work.",
				"I usually make coffee before work.",
				"MIDDLE"));
		GeneratedSentenceWord sentenceWord = generatedSentenceWordRepository.save(new GeneratedSentenceWord(sentence, word));
		SentenceSituation situation = sentenceSituationRepository.save(new SentenceSituation(
				sentence,
				"Calling a friend before leaving for work"));

		entityManager.flush();
		entityManager.clear();

		GenerationHistory foundHistory = generationHistoryRepository.findById(history.getId()).orElseThrow();
		GeneratedSentence foundSentence = generatedSentenceRepository.findById(sentence.getId()).orElseThrow();
		GeneratedSentenceWord foundSentenceWord = generatedSentenceWordRepository.findById(sentenceWord.getId()).orElseThrow();
		SentenceSituation foundSituation = sentenceSituationRepository.findById(situation.getId()).orElseThrow();

		assertThat(foundHistory.getUser().getId()).isEqualTo(user.getId());
		assertThat(foundHistory.getWord().getId()).isEqualTo(word.getId());
		assertThat(foundHistory.getPattern().getId()).isEqualTo(pattern.getId());
		assertThat(foundSentence.getGenerationHistory().getId()).isEqualTo(history.getId());
		assertThat(foundSentence.getUser().getId()).isEqualTo(user.getId());
		assertThat(foundSentence.isDeleted()).isFalse();
		assertThat(foundSentenceWord.getSentence().getId()).isEqualTo(sentence.getId());
		assertThat(foundSentenceWord.getWord().getId()).isEqualTo(word.getId());
		assertThat(foundSituation.getSentence().getId()).isEqualTo(sentence.getId());
	}

	@Test
	void persistsReviewAndSettingMappings() {
		User user = saveUser("mapping3@example.com");
		Word word = wordRepository.save(new Word(user, "sip", "drink slowly"));
		entityManager.flush();
		ReviewItem reviewItem = reviewItemRepository.save(new ReviewItem(
				user,
				ReviewItemType.WORD,
				word.getId(),
				ReviewDirection.RECOGNITION,
				LocalDate.of(2026, 5, 8)));
		ReviewLog reviewLog = reviewLogRepository.save(new ReviewLog(reviewItem, ReviewResult.EASY));
		UserSetting userSetting = userSettingRepository.save(new UserSetting(user, 20));

		entityManager.flush();
		entityManager.clear();

		ReviewItem foundReviewItem = reviewItemRepository.findById(reviewItem.getId()).orElseThrow();
		ReviewLog foundReviewLog = reviewLogRepository.findById(reviewLog.getId()).orElseThrow();
		UserSetting foundUserSetting = userSettingRepository.findById(userSetting.getId()).orElseThrow();

		assertThat(foundReviewItem.getUser().getId()).isEqualTo(user.getId());
		assertThat(foundReviewItem.getItemType()).isEqualTo(ReviewItemType.WORD);
		assertThat(foundReviewItem.getItemId()).isEqualTo(word.getId());
		assertThat(foundReviewItem.getDirection()).isEqualTo(ReviewDirection.RECOGNITION);
		assertThat(foundReviewItem.getIntervalDays()).isEqualTo(1);
		assertThat(foundReviewItem.getEaseFactor()).isEqualTo(2.5);
		assertThat(foundReviewItem.getReviewCount()).isZero();
		assertThat(foundReviewItem.getLastResult()).isNull();
		assertThat(foundReviewItem.isDeleted()).isFalse();
		assertThat(foundReviewLog.getReviewItem().getId()).isEqualTo(reviewItem.getId());
		assertThat(foundReviewLog.getResult()).isEqualTo(ReviewResult.EASY);
		assertThat(foundReviewLog.getReviewedAt()).isNotNull();
		assertThat(foundUserSetting.getUser().getId()).isEqualTo(user.getId());
		assertThat(foundUserSetting.getDailyReviewCount()).isEqualTo(20);
		assertThat(foundUserSetting.getCreatedAt()).isNotNull();
		assertThat(foundUserSetting.getUpdatedAt()).isNotNull();
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}
}

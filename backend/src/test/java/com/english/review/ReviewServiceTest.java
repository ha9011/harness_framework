package com.english.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
import com.english.setting.UserSetting;
import com.english.setting.UserSettingRepository;
import com.english.word.Word;
import com.english.word.WordRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ReviewServiceTest {

	private final ReviewService reviewService;
	private final UserRepository userRepository;
	private final UserSettingRepository userSettingRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;
	private final PatternExampleRepository patternExampleRepository;
	private final GenerationHistoryRepository generationHistoryRepository;
	private final GeneratedSentenceRepository generatedSentenceRepository;
	private final GeneratedSentenceWordRepository generatedSentenceWordRepository;
	private final SentenceSituationRepository sentenceSituationRepository;
	private final ReviewItemRepository reviewItemRepository;
	private final ReviewLogRepository reviewLogRepository;
	private final EntityManager entityManager;

	@Autowired
	ReviewServiceTest(
			ReviewService reviewService,
			UserRepository userRepository,
			UserSettingRepository userSettingRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository,
			PatternExampleRepository patternExampleRepository,
			GenerationHistoryRepository generationHistoryRepository,
			GeneratedSentenceRepository generatedSentenceRepository,
			GeneratedSentenceWordRepository generatedSentenceWordRepository,
			SentenceSituationRepository sentenceSituationRepository,
			ReviewItemRepository reviewItemRepository,
			ReviewLogRepository reviewLogRepository,
			EntityManager entityManager
	) {
		this.reviewService = reviewService;
		this.userRepository = userRepository;
		this.userSettingRepository = userSettingRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
		this.patternExampleRepository = patternExampleRepository;
		this.generationHistoryRepository = generationHistoryRepository;
		this.generatedSentenceRepository = generatedSentenceRepository;
		this.generatedSentenceWordRepository = generatedSentenceWordRepository;
		this.sentenceSituationRepository = sentenceSituationRepository;
		this.reviewItemRepository = reviewItemRepository;
		this.reviewLogRepository = reviewLogRepository;
		this.entityManager = entityManager;
	}

	@Test
	void getTodayReviewsSelectsTypeSpecificDailyCountAndSupportsExclude() {
		User user = saveUser("review-deck@example.com");
		saveSetting(user, 10);
		List<ReviewItem> wordItems = new ArrayList<>();
		List<ReviewItem> patternItems = new ArrayList<>();
		for (int index = 0; index < 12; index++) {
			Word word = saveWord(user, "word " + index, "뜻 " + index);
			wordItems.add(saveReviewItem(user, ReviewItemType.WORD, word.getId(), ReviewDirection.RECOGNITION));
			Pattern pattern = savePattern(user, "I can " + index, "~할 수 있다 " + index);
			patternItems.add(saveReviewItem(user, ReviewItemType.PATTERN, pattern.getId(), ReviewDirection.RECOGNITION));
		}

		List<ReviewCardResponse> firstWordDeck = reviewService.getTodayReviews(
				user.getId(),
				ReviewItemType.WORD,
				List.of());
		List<ReviewCardResponse> patternDeck = reviewService.getTodayReviews(
				user.getId(),
				ReviewItemType.PATTERN,
				List.of());
		List<ReviewCardResponse> extraWordDeck = reviewService.getTodayReviews(
				user.getId(),
				ReviewItemType.WORD,
				firstWordDeck.stream().map(ReviewCardResponse::reviewItemId).toList());

		assertThat(firstWordDeck).hasSize(10);
		assertThat(firstWordDeck)
				.extracting(ReviewCardResponse::itemType)
				.containsOnly(ReviewItemType.WORD);
		assertThat(firstWordDeck)
				.extracting(ReviewCardResponse::reviewItemId)
				.containsOnlyElementsOf(wordItems.stream().limit(10).map(ReviewItem::getId).toList());
		assertThat(patternDeck).hasSize(10);
		assertThat(patternDeck)
				.extracting(ReviewCardResponse::itemType)
				.containsOnly(ReviewItemType.PATTERN);
		assertThat(patternDeck)
				.extracting(ReviewCardResponse::reviewItemId)
				.containsOnlyElementsOf(patternItems.stream().limit(10).map(ReviewItem::getId).toList());
		assertThat(extraWordDeck)
				.extracting(ReviewCardResponse::reviewItemId)
				.containsExactlyInAnyOrderElementsOf(wordItems.stream().skip(10).map(ReviewItem::getId).toList());
	}

	@Test
	void getTodayReviewsAppliesDueActiveOwnershipAndPriorityBeforeShuffle() {
		User owner = saveUser("review-priority-owner@example.com");
		User otherUser = saveUser("review-priority-other@example.com");
		saveSetting(owner, 10);
		ReviewItem hard = saveWordReviewWithState(
				owner,
				"hard",
				ReviewResult.HARD,
				Instant.parse("2026-05-08T00:00:00Z"),
				99,
				LocalDate.now());
		ReviewItem oldest = saveWordReviewWithState(
				owner,
				"oldest",
				ReviewResult.MEDIUM,
				Instant.parse("2026-01-01T00:00:00Z"),
				99,
				LocalDate.now());
		List<ReviewItem> sameReviewedAt = new ArrayList<>();
		for (int count = 0; count < 11; count++) {
			sameReviewedAt.add(saveWordReviewWithState(
					owner,
					"count-" + count,
					ReviewResult.MEDIUM,
					Instant.parse("2026-02-01T00:00:00Z"),
					count,
					LocalDate.now()));
		}
		ReviewItem future = saveWordReviewWithState(
				owner,
				"future",
				ReviewResult.HARD,
				Instant.parse("2026-01-01T00:00:00Z"),
				0,
				LocalDate.now().plusDays(1));
		ReviewItem otherUserItem = saveWordReviewWithState(
				otherUser,
				"other",
				ReviewResult.HARD,
				Instant.parse("2026-01-01T00:00:00Z"),
				0,
				LocalDate.now());
		ReviewItem deletedReviewItem = saveWordReviewWithState(
				owner,
				"deleted-review",
				ReviewResult.HARD,
				Instant.parse("2026-01-01T00:00:00Z"),
				0,
				LocalDate.now());
		deletedReviewItem.softDelete();
		Word deletedWord = saveWord(owner, "deleted-word", "삭제된 단어");
		deletedWord.softDelete();
		ReviewItem deletedWordItem = saveReviewItem(
				owner,
				ReviewItemType.WORD,
				deletedWord.getId(),
				ReviewDirection.RECOGNITION);
		flushAndClear();

		List<ReviewCardResponse> deck = reviewService.getTodayReviews(owner.getId(), ReviewItemType.WORD, List.of());

		List<Long> expectedSelectedIds = new ArrayList<>();
		expectedSelectedIds.add(hard.getId());
		expectedSelectedIds.add(oldest.getId());
		expectedSelectedIds.addAll(sameReviewedAt.stream().limit(8).map(ReviewItem::getId).toList());
		List<Long> selectedIds = deck.stream().map(ReviewCardResponse::reviewItemId).toList();
		assertThat(deck).hasSize(10);
		assertThat(selectedIds)
				.containsExactlyInAnyOrderElementsOf(expectedSelectedIds)
				.doesNotContain(
						sameReviewedAt.get(8).getId(),
						sameReviewedAt.get(9).getId(),
						sameReviewedAt.get(10).getId(),
						future.getId(),
						otherUserItem.getId(),
						deletedReviewItem.getId(),
						deletedWordItem.getId());
	}

	@Test
	void getTodayReviewsAssemblesWordPatternAndSentenceCards() {
		User user = saveUser("review-card@example.com");
		saveSetting(user, 10);
		Word word = saveWord(user, "make a bed", "침대를 정리하다");
		word.update(
				word.getWord(),
				word.getMeaning(),
				"phrase",
				"/meik a bed/",
				"tidy up the bed",
				"make the bed도 같은 의미");
		Pattern pattern = savePattern(user, "I'm afraid that...", "유감스럽게도 ~인 것 같아요");
		patternExampleRepository.save(new PatternExample(
				pattern,
				1,
				"I'm afraid that we'll be late.",
				"유감스럽게도 우리는 늦을 것 같아요."));
		GeneratedSentence sentence = saveSentence(
				user,
				pattern,
				"I'm afraid that I forgot to make my bed.",
				"유감스럽게도 침대 정리를 깜빡한 것 같아요.",
				word,
				situations());
		ReviewItem wordRecognition = saveReviewItem(
				user,
				ReviewItemType.WORD,
				word.getId(),
				ReviewDirection.RECOGNITION);
		ReviewItem wordRecall = saveReviewItem(user, ReviewItemType.WORD, word.getId(), ReviewDirection.RECALL);
		ReviewItem patternRecognition = saveReviewItem(
				user,
				ReviewItemType.PATTERN,
				pattern.getId(),
				ReviewDirection.RECOGNITION);
		ReviewItem patternRecall = saveReviewItem(user, ReviewItemType.PATTERN, pattern.getId(), ReviewDirection.RECALL);
		ReviewItem sentenceRecognition = saveReviewItem(
				user,
				ReviewItemType.SENTENCE,
				sentence.getId(),
				ReviewDirection.RECOGNITION);

		List<ReviewCardResponse> wordDeck = reviewService.getTodayReviews(user.getId(), ReviewItemType.WORD, List.of());
		List<ReviewCardResponse> patternDeck = reviewService.getTodayReviews(
				user.getId(),
				ReviewItemType.PATTERN,
				List.of());
		List<ReviewCardResponse> sentenceDeck = reviewService.getTodayReviews(
				user.getId(),
				ReviewItemType.SENTENCE,
				List.of());

		ReviewCardResponse wordRecognitionCard = card(wordDeck, wordRecognition);
		assertThat(wordRecognitionCard.front()).isEqualTo(new ReviewCardFront("make a bed", null));
		assertThat(wordRecognitionCard.back()).isEqualTo(new WordRecognitionReviewBack(
				"침대를 정리하다",
				"/meik a bed/",
				"make the bed도 같은 의미",
				List.of("I'm afraid that I forgot to make my bed.")));
		ReviewCardResponse wordRecallCard = card(wordDeck, wordRecall);
		assertThat(wordRecallCard.front()).isEqualTo(new ReviewCardFront("침대를 정리하다", null));
		assertThat(wordRecallCard.back()).isEqualTo(new WordRecallReviewBack(
				"make a bed",
				"/meik a bed/",
				"make the bed도 같은 의미"));

		ReviewCardResponse patternRecognitionCard = card(patternDeck, patternRecognition);
		assertThat(patternRecognitionCard.front()).isEqualTo(new ReviewCardFront("I'm afraid that...", null));
		assertThat(patternRecognitionCard.back()).isEqualTo(new PatternRecognitionReviewBack(
				"유감스럽게도 ~인 것 같아요",
				List.of(new ReviewPatternExampleResponse(
						"I'm afraid that we'll be late.",
						"유감스럽게도 우리는 늦을 것 같아요."))));
		ReviewCardResponse patternRecallCard = card(patternDeck, patternRecall);
		assertThat(patternRecallCard.front()).isEqualTo(new ReviewCardFront("유감스럽게도 ~인 것 같아요", null));
		assertThat(patternRecallCard.back()).isEqualTo(new PatternRecallReviewBack(
				"I'm afraid that...",
				List.of(new ReviewPatternExampleResponse(
						"I'm afraid that we'll be late.",
						"유감스럽게도 우리는 늦을 것 같아요."))));

		ReviewCardResponse sentenceRecognitionCard = card(sentenceDeck, sentenceRecognition);
		assertThat(sentenceRecognitionCard.direction()).isEqualTo(ReviewDirection.RECOGNITION);
		assertThat(sentenceRecognitionCard.front().text()).isEqualTo("I'm afraid that I forgot to make my bed.");
		assertThat(sentenceRecognitionCard.front().situation()).isIn(situations());
		assertThat(sentenceRecognitionCard.back()).isEqualTo(new SentenceRecognitionReviewBack(
				"유감스럽게도 침대 정리를 깜빡한 것 같아요.",
				"I'm afraid that...",
				List.of("make a bed")));
	}

	@Test
	void recordResultVerifiesOwnershipStoresLogAndUpdatesSm2Values() {
		User owner = saveUser("review-result-owner@example.com");
		User otherUser = saveUser("review-result-other@example.com");
		Word word = saveWord(owner, "brew coffee", "커피를 내리다");
		ReviewItem reviewItem = saveReviewItem(owner, ReviewItemType.WORD, word.getId(), ReviewDirection.RECOGNITION);
		updateReviewState(
				reviewItem,
				ReviewResult.MEDIUM,
				Instant.parse("2026-05-01T00:00:00Z"),
				2,
				LocalDate.now(),
				4,
				2.0);
		flushAndClear();

		ReviewResultResponse response = reviewService.recordResult(owner.getId(), reviewItem.getId(), ReviewResult.EASY);
		Throwable forbidden = catchThrowable(() -> reviewService.recordResult(
				otherUser.getId(),
				reviewItem.getId(),
				ReviewResult.HARD));

		assertThat(response.intervalDays()).isEqualTo(10);
		assertThat(response.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(10));
		ReviewItem updated = reviewItemRepository.findById(reviewItem.getId()).orElseThrow();
		assertThat(updated.getLastResult()).isEqualTo(ReviewResult.EASY);
		assertThat(updated.getReviewCount()).isEqualTo(3);
		assertThat(updated.getIntervalDays()).isEqualTo(10);
		assertThat(updated.getEaseFactor()).isEqualTo(2.15);
		assertThat(updated.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(10));
		assertThat(updated.getLastReviewedAt()).isNotNull();
		assertThat(reviewLogRepository.findAll())
				.singleElement()
				.satisfies(log -> {
					assertThat(log.getReviewItem().getId()).isEqualTo(reviewItem.getId());
					assertThat(log.getResult()).isEqualTo(ReviewResult.EASY);
				});
		assertThat(forbidden).isInstanceOf(ReviewException.class);
		assertThat(((ReviewException) forbidden).getErrorCode()).isEqualTo(ReviewErrorCode.FORBIDDEN);
	}

	private ReviewCardResponse card(List<ReviewCardResponse> deck, ReviewItem reviewItem) {
		return deck.stream()
				.filter(card -> card.reviewItemId().equals(reviewItem.getId()))
				.findFirst()
				.orElseThrow();
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}

	private void saveSetting(User user, int dailyReviewCount) {
		userSettingRepository.save(new UserSetting(user, dailyReviewCount));
	}

	private Word saveWord(User user, String wordText, String meaning) {
		return wordRepository.save(new Word(user, wordText, meaning));
	}

	private Pattern savePattern(User user, String template, String description) {
		return patternRepository.save(new Pattern(user, template, description));
	}

	private ReviewItem saveReviewItem(
			User user,
			ReviewItemType itemType,
			Long itemId,
			ReviewDirection direction
	) {
		return reviewItemRepository.save(new ReviewItem(user, itemType, itemId, direction, LocalDate.now()));
	}

	private ReviewItem saveWordReviewWithState(
			User user,
			String wordText,
			ReviewResult lastResult,
			Instant lastReviewedAt,
			int reviewCount,
			LocalDate nextReviewDate
	) {
		Word word = saveWord(user, wordText, "뜻 " + wordText);
		ReviewItem reviewItem = reviewItemRepository.save(new ReviewItem(
				user,
				ReviewItemType.WORD,
				word.getId(),
				ReviewDirection.RECOGNITION,
				nextReviewDate));
		updateReviewState(reviewItem, lastResult, lastReviewedAt, reviewCount, nextReviewDate, 1, 2.5);
		return reviewItem;
	}

	private void updateReviewState(
			ReviewItem reviewItem,
			ReviewResult lastResult,
			Instant lastReviewedAt,
			int reviewCount,
			LocalDate nextReviewDate,
			int intervalDays,
			double easeFactor
	) {
		entityManager.flush();
		entityManager.createNativeQuery("""
				update review_items
				set last_result = :lastResult,
					last_reviewed_at = :lastReviewedAt,
					review_count = :reviewCount,
					next_review_date = :nextReviewDate,
					interval_days = :intervalDays,
					ease_factor = :easeFactor
				where id = :id
				""")
				.setParameter("lastResult", lastResult.name())
				.setParameter("lastReviewedAt", lastReviewedAt)
				.setParameter("reviewCount", reviewCount)
				.setParameter("nextReviewDate", nextReviewDate)
				.setParameter("intervalDays", intervalDays)
				.setParameter("easeFactor", easeFactor)
				.setParameter("id", reviewItem.getId())
				.executeUpdate();
		entityManager.flush();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private GeneratedSentence saveSentence(
			User user,
			Pattern pattern,
			String sentenceText,
			String translation,
			Word word,
			List<String> situations
	) {
		GenerationHistory history = generationHistoryRepository.save(new GenerationHistory(
				user,
				"중등",
				10,
				1,
				null,
				null));
		GeneratedSentence sentence = generatedSentenceRepository.save(new GeneratedSentence(
				user,
				history,
				pattern,
				sentenceText,
				translation,
				"중등"));
		generatedSentenceWordRepository.save(new GeneratedSentenceWord(sentence, word));
		situations.forEach(situation -> sentenceSituationRepository.save(new SentenceSituation(sentence, situation)));
		return sentence;
	}

	private List<String> situations() {
		return List.of(
				"아침에 급하게 나와 전화하는 상황",
				"룸메이트에게 말하는 상황",
				"아이에게 생활 습관을 알려주는 상황",
				"호텔 직원에게 사과하는 상황",
				"친구에게 오늘 아침 일을 말하는 상황");
	}
}

package com.english.generate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.review.ReviewDirection;
import com.english.review.ReviewItem;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemType;
import com.english.word.Word;
import com.english.word.WordRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class GenerateServiceTest {

	private final GenerateService generateService;
	private final FakeGenerateGeminiClient geminiClient;
	private final UserRepository userRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;
	private final GenerationHistoryRepository generationHistoryRepository;
	private final GeneratedSentenceRepository generatedSentenceRepository;
	private final GeneratedSentenceWordRepository generatedSentenceWordRepository;
	private final SentenceSituationRepository sentenceSituationRepository;
	private final ReviewItemRepository reviewItemRepository;
	private final EntityManager entityManager;

	@Autowired
	GenerateServiceTest(
			GenerateService generateService,
			FakeGenerateGeminiClient geminiClient,
			UserRepository userRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository,
			GenerationHistoryRepository generationHistoryRepository,
			GeneratedSentenceRepository generatedSentenceRepository,
			GeneratedSentenceWordRepository generatedSentenceWordRepository,
			SentenceSituationRepository sentenceSituationRepository,
			ReviewItemRepository reviewItemRepository,
			EntityManager entityManager
	) {
		this.generateService = generateService;
		this.geminiClient = geminiClient;
		this.userRepository = userRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
		this.generationHistoryRepository = generationHistoryRepository;
		this.generatedSentenceRepository = generatedSentenceRepository;
		this.generatedSentenceWordRepository = generatedSentenceWordRepository;
		this.sentenceSituationRepository = sentenceSituationRepository;
		this.reviewItemRepository = reviewItemRepository;
		this.entityManager = entityManager;
	}

	@BeforeEach
	void setUp() {
		geminiClient.reset();
	}

	@Test
	void generateStoresHistorySentencesMappingsSituationsAndSentenceReviewItems() {
		User user = saveUser("generate-save@example.com");
		Word firstWord = saveWord(user, "make a bed", "침대를 정리하다", true);
		Word secondWord = saveWord(user, "drink coffee", "커피를 마시다", false);
		Pattern pattern = patternRepository.save(new Pattern(user, "I'm afraid that...", "유감스럽게도 ~인 것 같아요"));
		geminiClient.enqueueSentences(List.of(new GeminiGeneratedSentence(
				"I'm afraid that I forgot to make my bed.",
				"유감스럽게도 침대 정리를 깜빡한 것 같아요.",
				pattern.getId(),
				List.of(firstWord.getId(), secondWord.getId()),
				situations())));

		GenerateResponse response = generateService.generate(user.getId(), "중등", 10);

		assertThat(response.generationId()).isNotNull();
		assertThat(response.sentences()).hasSize(1);
		GeneratedSentenceResponse sentenceResponse = response.sentences().getFirst();
		assertThat(sentenceResponse.sentence()).isEqualTo("I'm afraid that I forgot to make my bed.");
		assertThat(sentenceResponse.pattern().id()).isEqualTo(pattern.getId());
		assertThat(sentenceResponse.words())
				.extracting(GeneratedSentenceWordResponse::id)
				.containsExactlyInAnyOrder(firstWord.getId(), secondWord.getId());
		assertThat(sentenceResponse.situations()).containsExactlyElementsOf(situations());

		GenerationHistory history = generationHistoryRepository.findById(response.generationId()).orElseThrow();
		assertThat(history.getUser().getId()).isEqualTo(user.getId());
		assertThat(history.getLevel()).isEqualTo("중등");
		assertThat(history.getRequestedCount()).isEqualTo(10);
		assertThat(history.getActualCount()).isEqualTo(1);
		assertThat(history.getWord()).isNull();
		assertThat(history.getPattern()).isNull();

		GeneratedSentence savedSentence = generatedSentenceRepository.findById(sentenceResponse.id()).orElseThrow();
		assertThat(savedSentence.getGenerationHistory().getId()).isEqualTo(history.getId());
		assertThat(savedSentence.getPattern().getId()).isEqualTo(pattern.getId());
		assertThat(generatedSentenceWordRepository.findAll())
				.extracting(sentenceWord -> sentenceWord.getSentence().getId(), sentenceWord -> sentenceWord.getWord().getId())
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(savedSentence.getId(), firstWord.getId()),
						org.assertj.core.groups.Tuple.tuple(savedSentence.getId(), secondWord.getId()));
		assertThat(sentenceSituationRepository.findAll())
				.extracting(SentenceSituation::getSituation)
				.containsExactlyElementsOf(situations());
		assertThat(reviewItemRepository.findByUserIdAndItemTypeAndItemId(
				user.getId(),
				ReviewItemType.SENTENCE,
				savedSentence.getId()))
				.singleElement()
				.satisfies(reviewItem -> {
					assertThat(reviewItem.getDirection()).isEqualTo(ReviewDirection.RECOGNITION);
					assertThat(reviewItem.getNextReviewDate()).isEqualTo(LocalDate.now());
					assertThat(reviewItem.isDeleted()).isFalse();
				});
	}

	@Test
	void generateUsesImportantThenLowRecognitionReviewCountAndLimitsCandidatesToFifty() {
		User user = saveUser("generate-priority@example.com");
		Word importantLowCount = saveWord(user, "important low", "중요 낮은 복습", true);
		Word importantHighCount = saveWord(user, "important high", "중요 높은 복습", true);
		createRecognitionReview(user, importantLowCount, 0);
		createRecognitionReview(user, importantHighCount, 7);
		List<Word> normalWords = new ArrayList<>();
		for (int index = 0; index < 53; index++) {
			Word word = saveWord(user, "normal " + index, "일반 " + index, false);
			createRecognitionReview(user, word, 0);
			normalWords.add(word);
		}
		Pattern pattern = patternRepository.save(new Pattern(user, "I tend to...", "자주 하는 일을 말한다"));
		geminiClient.enqueueSentences(List.of(new GeminiGeneratedSentence(
				"I tend to use important low first.",
				"나는 중요한 낮은 복습 표현을 먼저 쓰는 편이다.",
				pattern.getId(),
				List.of(importantLowCount.getId()),
				situations())));

		generateService.generate(user.getId(), "고등", 30);

		GeminiSentenceGenerationRequest request = geminiClient.lastSentenceRequest();
		assertThat(request.count()).isEqualTo(30);
		assertThat(request.words()).hasSize(50);
		assertThat(request.words())
				.extracting(GeminiSentenceWordCandidate::id)
				.startsWith(importantLowCount.getId(), importantHighCount.getId());
		assertThat(request.patterns())
				.extracting(GeminiSentencePatternCandidate::id)
				.containsExactly(pattern.getId());
		assertThat(request.words())
				.extracting(GeminiSentenceWordCandidate::id)
				.containsAnyElementsOf(normalWords.stream().map(Word::getId).toList());
	}

	@Test
	void generateForWordVerifiesOwnershipAndSendsNoPatternCandidates() {
		User owner = saveUser("generate-word-owner@example.com");
		User otherUser = saveUser("generate-word-other@example.com");
		Word word = saveWord(owner, "brew coffee", "커피를 내리다", false);
		geminiClient.enqueueSentences(List.of(new GeminiGeneratedSentence(
				"I brew coffee every morning.",
				"나는 매일 아침 커피를 내린다.",
				null,
				List.of(word.getId()),
				situations())));

		GenerateResponse response = generateService.generateForWord(owner.getId(), word.getId(), "초등", 10);
		Throwable forbidden = catchThrowable(() -> generateService.generateForWord(
				otherUser.getId(),
				word.getId(),
				"초등",
				10));

		assertThat(response.sentences()).hasSize(1);
		assertThat(geminiClient.lastSentenceRequest().words())
				.extracting(GeminiSentenceWordCandidate::id)
				.containsExactly(word.getId());
		assertThat(geminiClient.lastSentenceRequest().patterns()).isEmpty();
		assertThat(forbidden).isInstanceOf(GenerateException.class);
		assertThat(((GenerateException) forbidden).getErrorCode()).isEqualTo(GenerateErrorCode.FORBIDDEN);
	}

	@Test
	void generateForPatternVerifiesOwnershipAndAutomaticallySelectsWordCandidates() {
		User owner = saveUser("generate-pattern-owner@example.com");
		User otherUser = saveUser("generate-pattern-other@example.com");
		Word word = saveWord(owner, "take a walk", "산책하다", false);
		Pattern pattern = patternRepository.save(new Pattern(owner, "Would you like to...?", "~하고 싶나요?"));
		geminiClient.enqueueSentences(List.of(new GeminiGeneratedSentence(
				"Would you like to take a walk?",
				"산책하러 갈래요?",
				pattern.getId(),
				List.of(word.getId()),
				situations())));

		GenerateResponse response = generateService.generateForPattern(owner.getId(), pattern.getId(), "유아", 10);
		Throwable forbidden = catchThrowable(() -> generateService.generateForPattern(
				otherUser.getId(),
				pattern.getId(),
				"유아",
				10));

		assertThat(response.sentences()).hasSize(1);
		assertThat(geminiClient.lastSentenceRequest().patterns())
				.extracting(GeminiSentencePatternCandidate::id)
				.containsExactly(pattern.getId());
		assertThat(geminiClient.lastSentenceRequest().words())
				.extracting(GeminiSentenceWordCandidate::id)
				.containsExactly(word.getId());
		assertThat(forbidden).isInstanceOf(GenerateException.class);
		assertThat(((GenerateException) forbidden).getErrorCode()).isEqualTo(GenerateErrorCode.FORBIDDEN);
	}

	@Test
	void geminiFailureReturnsAiServiceErrorAndDoesNotPersistGeneration() {
		User user = saveUser("generate-failure@example.com");
		saveWord(user, "study English", "영어를 공부하다", false);
		patternRepository.save(new Pattern(user, "I need to...", "~해야 한다"));
		geminiClient.failSentenceGeneration(new GeminiClientException(
				GeminiOperation.SENTENCE_GENERATION,
				GeminiFailureType.NETWORK_ERROR,
				"Gemini request failed",
				false,
				null));

		Throwable throwable = catchThrowable(() -> generateService.generate(user.getId(), "중등", 10));

		assertThat(throwable).isInstanceOf(GenerateException.class);
		assertThat(((GenerateException) throwable).getErrorCode()).isEqualTo(GenerateErrorCode.AI_SERVICE_ERROR);
		assertThat(generationHistoryRepository.count()).isZero();
		assertThat(generatedSentenceRepository.count()).isZero();
		assertThat(reviewItemRepository.findAll())
				.noneSatisfy(reviewItem -> assertThat(reviewItem.getItemType()).isEqualTo(ReviewItemType.SENTENCE));
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}

	private Word saveWord(User user, String wordText, String meaning, boolean important) {
		Word word = new Word(user, wordText, meaning);
		Word saved = wordRepository.save(word);
		if (important) {
			saved.toggleImportant();
		}
		return saved;
	}

	private void createRecognitionReview(User user, Word word, int reviewCount) {
		ReviewItem reviewItem = reviewItemRepository.save(new ReviewItem(
				user,
				ReviewItemType.WORD,
				word.getId(),
				ReviewDirection.RECOGNITION,
				LocalDate.now()));
		entityManager.flush();
		entityManager.createNativeQuery("update review_items set review_count = :reviewCount where id = :id")
				.setParameter("reviewCount", reviewCount)
				.setParameter("id", reviewItem.getId())
				.executeUpdate();
		entityManager.flush();
		entityManager.clear();
	}

	private List<String> situations() {
		return List.of(
				"아침에 급하게 나와 전화하는 상황",
				"룸메이트에게 말하는 상황",
				"아이에게 생활 습관을 알려주는 상황",
				"호텔 직원에게 사과하는 상황",
				"친구에게 오늘 아침 일을 말하는 상황");
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		@Primary
		FakeGenerateGeminiClient fakeGenerateGeminiClient() {
			return new FakeGenerateGeminiClient();
		}
	}
}

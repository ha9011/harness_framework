package com.english.generate;

import static org.assertj.core.api.Assertions.assertThat;

import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.review.ReviewDirection;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemType;
import com.english.word.Word;
import com.english.word.WordRepository;
import java.time.LocalDate;
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
class GeneratedSentenceMappingTest {

	private final GenerateService generateService;
	private final FakeGenerateGeminiClient geminiClient;
	private final UserRepository userRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;
	private final GeneratedSentenceRepository generatedSentenceRepository;
	private final GeneratedSentenceWordRepository generatedSentenceWordRepository;
	private final SentenceSituationRepository sentenceSituationRepository;
	private final ReviewItemRepository reviewItemRepository;

	@Autowired
	GeneratedSentenceMappingTest(
			GenerateService generateService,
			FakeGenerateGeminiClient geminiClient,
			UserRepository userRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository,
			GeneratedSentenceRepository generatedSentenceRepository,
			GeneratedSentenceWordRepository generatedSentenceWordRepository,
			SentenceSituationRepository sentenceSituationRepository,
			ReviewItemRepository reviewItemRepository
	) {
		this.generateService = generateService;
		this.geminiClient = geminiClient;
		this.userRepository = userRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
		this.generatedSentenceRepository = generatedSentenceRepository;
		this.generatedSentenceWordRepository = generatedSentenceWordRepository;
		this.sentenceSituationRepository = sentenceSituationRepository;
		this.reviewItemRepository = reviewItemRepository;
	}

	@BeforeEach
	void setUp() {
		geminiClient.reset();
	}

	@Test
	void ignoresInvalidWordIdsAndPatternIdButStillStoresSentenceSituationsAndReviewItem() {
		User owner = saveUser("mapping-owner@example.com");
		User otherUser = saveUser("mapping-other@example.com");
		Word validWord = wordRepository.save(new Word(owner, "call back", "다시 전화하다"));
		Word otherUsersWord = wordRepository.save(new Word(otherUser, "wrong word", "다른 사용자 단어"));
		Word deletedWord = wordRepository.save(new Word(owner, "deleted word", "삭제된 단어"));
		deletedWord.softDelete();
		Pattern requestedPattern = patternRepository.save(new Pattern(owner, "Could you...?", "부탁할 때 쓴다"));
		Pattern otherUsersPattern = patternRepository.save(new Pattern(otherUser, "I used to...", "과거 습관"));
		geminiClient.enqueueSentences(List.of(new GeminiGeneratedSentence(
				"Could you call me back later?",
				"나중에 다시 전화해 주실 수 있나요?",
				otherUsersPattern.getId(),
				List.of(validWord.getId(), otherUsersWord.getId(), deletedWord.getId(), 999_999L),
				situations())));

		GenerateResponse response = generateService.generateForPattern(owner.getId(), requestedPattern.getId(), "중등", 10);

		GeneratedSentenceResponse sentenceResponse = response.sentences().getFirst();
		assertThat(sentenceResponse.pattern()).isNull();
		assertThat(sentenceResponse.words())
				.extracting(GeneratedSentenceWordResponse::id)
				.containsExactly(validWord.getId());

		GeneratedSentence savedSentence = generatedSentenceRepository.findById(sentenceResponse.id()).orElseThrow();
		assertThat(savedSentence.getPattern()).isNull();
		assertThat(generatedSentenceWordRepository.findAll())
				.extracting(sentenceWord -> sentenceWord.getWord().getId())
				.containsExactly(validWord.getId());
		assertThat(sentenceSituationRepository.findAll())
				.extracting(SentenceSituation::getSituation)
				.containsExactlyElementsOf(situations());
		assertThat(reviewItemRepository.findByUserIdAndItemTypeAndItemId(
				owner.getId(),
				ReviewItemType.SENTENCE,
				savedSentence.getId()))
				.singleElement()
				.satisfies(reviewItem -> {
					assertThat(reviewItem.getDirection()).isEqualTo(ReviewDirection.RECOGNITION);
					assertThat(reviewItem.getNextReviewDate()).isEqualTo(LocalDate.now());
				});
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}

	private List<String> situations() {
		return List.of(
				"퇴근 전에 전화를 부탁하는 상황",
				"부재중 전화를 보고 답장하는 상황",
				"회의가 끝난 뒤 연락을 요청하는 상황",
				"가족에게 나중에 통화하자고 말하는 상황",
				"고객에게 다시 연락을 부탁하는 상황");
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

package com.english.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.auth.AuthResult;
import com.english.auth.AuthService;
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
import com.english.pattern.PatternCreateRequest;
import com.english.pattern.PatternExampleRequest;
import com.english.pattern.PatternRepository;
import com.english.pattern.PatternResponse;
import com.english.pattern.PatternService;
import com.english.word.Word;
import com.english.word.WordCreateRequest;
import com.english.word.WordRepository;
import com.english.word.WordResponse;
import com.english.word.WordService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
		"auth.jwt.secret=word-controller-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT24H"
})
class ReviewControllerTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper;
	private final AuthService authService;
	private final UserRepository userRepository;
	private final WordService wordService;
	private final WordRepository wordRepository;
	private final PatternService patternService;
	private final PatternRepository patternRepository;
	private final ReviewItemRepository reviewItemRepository;
	private final GenerationHistoryRepository generationHistoryRepository;
	private final GeneratedSentenceRepository generatedSentenceRepository;
	private final GeneratedSentenceWordRepository generatedSentenceWordRepository;
	private final SentenceSituationRepository sentenceSituationRepository;

	@Autowired
	ReviewControllerTest(
			MockMvc mockMvc,
			ObjectMapper objectMapper,
			AuthService authService,
			UserRepository userRepository,
			WordService wordService,
			WordRepository wordRepository,
			PatternService patternService,
			PatternRepository patternRepository,
			ReviewItemRepository reviewItemRepository,
			GenerationHistoryRepository generationHistoryRepository,
			GeneratedSentenceRepository generatedSentenceRepository,
			GeneratedSentenceWordRepository generatedSentenceWordRepository,
			SentenceSituationRepository sentenceSituationRepository
	) {
		this.mockMvc = mockMvc;
		this.objectMapper = objectMapper;
		this.authService = authService;
		this.userRepository = userRepository;
		this.wordService = wordService;
		this.wordRepository = wordRepository;
		this.patternService = patternService;
		this.patternRepository = patternRepository;
		this.reviewItemRepository = reviewItemRepository;
		this.generationHistoryRepository = generationHistoryRepository;
		this.generatedSentenceRepository = generatedSentenceRepository;
		this.generatedSentenceWordRepository = generatedSentenceWordRepository;
		this.sentenceSituationRepository = sentenceSituationRepository;
	}

	@Test
	void reviewApisRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/reviews/today")
						.param("type", "WORD"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

		mockMvc.perform(post("/api/reviews/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "result": "EASY"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void todayReviewsReturnsWordCardsAndParsesExclude() throws Exception {
		AuthResult user = signup("review-word");
		WordResponse word = wordService.create(user.user().id(), new WordCreateRequest(
				"make a bed",
				"침대를 정리하다",
				"phrase",
				"/meik a bed/",
				null,
				"make the bed도 같은 의미"));
		ReviewItem recognition = reviewItem(
				user.user().id(),
				ReviewItemType.WORD,
				word.id(),
				ReviewDirection.RECOGNITION);
		ReviewItem recall = reviewItem(user.user().id(), ReviewItemType.WORD, word.id(), ReviewDirection.RECALL);

		MvcResult result = mockMvc.perform(get("/api/reviews/today")
						.cookie(tokenCookie(user))
						.param("type", "WORD")
						.param("exclude", recall.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].reviewItemId").value(recognition.getId()))
				.andExpect(jsonPath("$[0].itemType").value("WORD"))
				.andExpect(jsonPath("$[0].direction").value("RECOGNITION"))
				.andExpect(jsonPath("$[0].front.text").value("make a bed"))
				.andExpect(jsonPath("$[0].back.meaning").value("침대를 정리하다"))
				.andExpect(jsonPath("$[0].back.pronunciation").value("/meik a bed/"))
				.andExpect(jsonPath("$[0].back.tip").value("make the bed도 같은 의미"))
				.andReturn();

		JsonNode cards = content(result);
		assertThat(cards).hasSize(1);
	}

	@Test
	void todayReviewsReturnsPatternAndSentenceCardStructures() throws Exception {
		AuthResult user = signup("review-structures");
		WordResponse word = wordService.create(user.user().id(), new WordCreateRequest("brew coffee", "커피를 내리다"));
		PatternResponse pattern = patternService.create(user.user().id(), new PatternCreateRequest(
				"I'm afraid that...",
				"유감스럽게도 ~인 것 같아요",
				List.of(new PatternExampleRequest(
						"I'm afraid that we'll be late.",
						"유감스럽게도 우리는 늦을 것 같아요."))));
		GeneratedSentence sentence = saveSentence(user, word.id(), pattern.id());
		ReviewItem sentenceReview = reviewItemRepository.save(new ReviewItem(
				userEntity(user),
				ReviewItemType.SENTENCE,
				sentence.getId(),
				ReviewDirection.RECOGNITION,
				LocalDate.now()));

		MvcResult patternResult = mockMvc.perform(get("/api/reviews/today")
						.cookie(tokenCookie(user))
						.param("type", "PATTERN"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode patternCards = content(patternResult);
		JsonNode patternRecognition = card(patternCards, "RECOGNITION");
		JsonNode patternRecall = card(patternCards, "RECALL");
		assertThat(patternRecognition.path("itemType").asText()).isEqualTo("PATTERN");
		assertThat(patternRecognition.path("front").path("text").asText()).isEqualTo("I'm afraid that...");
		assertThat(patternRecognition.path("back").path("description").asText()).isEqualTo("유감스럽게도 ~인 것 같아요");
		assertThat(patternRecognition.path("back").path("examples").get(0).path("sentence").asText())
				.isEqualTo("I'm afraid that we'll be late.");
		assertThat(patternRecall.path("front").path("text").asText()).isEqualTo("유감스럽게도 ~인 것 같아요");
		assertThat(patternRecall.path("back").path("template").asText()).isEqualTo("I'm afraid that...");

		mockMvc.perform(get("/api/reviews/today")
						.cookie(tokenCookie(user))
						.param("type", "SENTENCE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].reviewItemId").value(sentenceReview.getId()))
				.andExpect(jsonPath("$[0].itemType").value("SENTENCE"))
				.andExpect(jsonPath("$[0].direction").value("RECOGNITION"))
				.andExpect(jsonPath("$[0].front.text").value("I'm afraid that I brew coffee every morning."))
				.andExpect(jsonPath("$[0].front.situation").value("아침에 직접 내린 커피를 권하는 상황"))
				.andExpect(jsonPath("$[0].back.translation").value("유감스럽게도 나는 매일 아침 커피를 내려요."))
				.andExpect(jsonPath("$[0].back.pattern").value("I'm afraid that..."))
				.andExpect(jsonPath("$[0].back.words[0]").value("brew coffee"));
	}

	@Test
	void invalidReviewRequestsReturnBadRequest() throws Exception {
		AuthResult user = signup("review-validation");

		mockMvc.perform(get("/api/reviews/today")
						.cookie(tokenCookie(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(get("/api/reviews/today")
						.cookie(tokenCookie(user))
						.param("type", "INVALID"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(get("/api/reviews/today")
						.cookie(tokenCookie(user))
						.param("type", "WORD")
						.param("exclude", "1,abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(post("/api/reviews/1")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "result": "AGAIN"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void recordResultReturnsScheduleAndRestrictsOwnership() throws Exception {
		AuthResult owner = signup("review-result-owner");
		AuthResult otherUser = signup("review-result-other");
		WordResponse word = wordService.create(
				owner.user().id(),
				new WordCreateRequest("read aloud", "소리 내어 읽다"));
		ReviewItem reviewItem = reviewItem(
				owner.user().id(),
				ReviewItemType.WORD,
				word.id(),
				ReviewDirection.RECOGNITION);

		mockMvc.perform(post("/api/reviews/{reviewItemId}", reviewItem.getId())
						.cookie(tokenCookie(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "result": "EASY"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nextReviewDate").value(LocalDate.now().plusDays(3).toString()))
				.andExpect(jsonPath("$.intervalDays").value(3));

		mockMvc.perform(post("/api/reviews/{reviewItemId}", reviewItem.getId())
						.cookie(tokenCookie(otherUser))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "result": "HARD"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	private AuthResult signup(String prefix) {
		return authService.signup(prefix + "-" + UUID.randomUUID() + "@example.com", "password123", "tester");
	}

	private static Cookie tokenCookie(AuthResult result) {
		return new Cookie(TOKEN_COOKIE, result.token());
	}

	private User userEntity(AuthResult result) {
		return userRepository.findById(result.user().id()).orElseThrow();
	}

	private ReviewItem reviewItem(
			Long userId,
			ReviewItemType itemType,
			Long itemId,
			ReviewDirection direction
	) {
		return reviewItemRepository.findByUserIdAndItemTypeAndItemIdAndDirectionAndDeletedFalse(
						userId,
						itemType,
						itemId,
						direction)
				.orElseThrow();
	}

	private GeneratedSentence saveSentence(AuthResult user, Long wordId, Long patternId) {
		User owner = userEntity(user);
		Word word = wordRepository.findById(wordId).orElseThrow();
		Pattern pattern = patternRepository.findById(patternId).orElseThrow();
		GenerationHistory history = generationHistoryRepository.save(new GenerationHistory(
				owner,
				"중등",
				10,
				1,
				null,
				null));
		GeneratedSentence sentence = generatedSentenceRepository.save(new GeneratedSentence(
				owner,
				history,
				pattern,
				"I'm afraid that I brew coffee every morning.",
				"유감스럽게도 나는 매일 아침 커피를 내려요.",
				"중등"));
		generatedSentenceWordRepository.save(new GeneratedSentenceWord(sentence, word));
		sentenceSituationRepository.save(new SentenceSituation(sentence, "아침에 직접 내린 커피를 권하는 상황"));
		return sentence;
	}

	private JsonNode content(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private static JsonNode card(JsonNode cards, String direction) {
		for (JsonNode card : cards) {
			if (direction.equals(card.path("direction").asText())) {
				return card;
			}
		}
		throw new AssertionError("review card direction not found: " + direction);
	}
}

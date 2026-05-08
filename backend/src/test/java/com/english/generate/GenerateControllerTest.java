package com.english.generate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.auth.AuthResult;
import com.english.auth.AuthService;
import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.pattern.PatternCreateRequest;
import com.english.pattern.PatternResponse;
import com.english.pattern.PatternService;
import com.english.word.WordCreateRequest;
import com.english.word.WordResponse;
import com.english.word.WordService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
		"auth.jwt.secret=generate-controller-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT24H"
})
class GenerateControllerTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final AuthService authService;
	private final WordService wordService;
	private final PatternService patternService;
	private final UserRepository userRepository;
	private final GenerationHistoryRepository generationHistoryRepository;
	private final FakeGenerateGeminiClient geminiClient;

	@Autowired
	GenerateControllerTest(
			MockMvc mockMvc,
			AuthService authService,
			WordService wordService,
			PatternService patternService,
			UserRepository userRepository,
			GenerationHistoryRepository generationHistoryRepository,
			FakeGenerateGeminiClient geminiClient
	) {
		this.mockMvc = mockMvc;
		this.authService = authService;
		this.wordService = wordService;
		this.patternService = patternService;
		this.userRepository = userRepository;
		this.generationHistoryRepository = generationHistoryRepository;
		this.geminiClient = geminiClient;
	}

	@BeforeEach
	void setUp() {
		geminiClient.reset();
	}

	@Test
	void generateApisRequireAuthentication() throws Exception {
		mockMvc.perform(post("/api/generate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(post("/api/generate/word")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(post("/api/generate/pattern")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/generate/history"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void generateCreatesSentencesForCurrentUserOnly() throws Exception {
		AuthResult owner = signup("generate-controller-owner");
		AuthResult otherUser = signup("generate-controller-other");
		WordResponse word = wordService.create(owner.user().id(), new WordCreateRequest("make a bed", "침대를 정리하다"));
		PatternResponse pattern = patternService.create(owner.user().id(), new PatternCreateRequest(
				"I'm afraid that...",
				"나쁜 소식을 부드럽게 말할 때 쓴다"));
		wordService.create(otherUser.user().id(), new WordCreateRequest("drink tea", "차를 마시다"));
		patternService.create(otherUser.user().id(), new PatternCreateRequest("I used to...", "과거 습관"));
		geminiClient.enqueueSentences(List.of(new GeminiGeneratedSentence(
				"I'm afraid that I forgot to make my bed.",
				"유감스럽게도 침대 정리를 깜빡했어요.",
				pattern.id(),
				List.of(word.id()),
				situations())));

		mockMvc.perform(post("/api/generate")
						.cookie(tokenCookie(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "level": "중등",
								  "count": 30
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.generationId").isNumber())
				.andExpect(jsonPath("$.sentences.length()").value(1))
				.andExpect(jsonPath("$.sentences[0].sentence").value("I'm afraid that I forgot to make my bed."))
				.andExpect(jsonPath("$.sentences[0].translation").value("유감스럽게도 침대 정리를 깜빡했어요."))
				.andExpect(jsonPath("$.sentences[0].situations.length()").value(5))
				.andExpect(jsonPath("$.sentences[0].level").value("중등"))
				.andExpect(jsonPath("$.sentences[0].pattern.id").value(pattern.id()))
				.andExpect(jsonPath("$.sentences[0].pattern.template").value("I'm afraid that..."))
				.andExpect(jsonPath("$.sentences[0].words[0].id").value(word.id()))
				.andExpect(jsonPath("$.sentences[0].words[0].word").value("make a bed"));

		GeminiSentenceGenerationRequest request = geminiClient.lastSentenceRequest();
		org.assertj.core.api.Assertions.assertThat(request.level()).isEqualTo("중등");
		org.assertj.core.api.Assertions.assertThat(request.count()).isEqualTo(30);
		org.assertj.core.api.Assertions.assertThat(request.words())
				.extracting(GeminiSentenceWordCandidate::id)
				.containsExactly(word.id());
		org.assertj.core.api.Assertions.assertThat(request.patterns())
				.extracting(GeminiSentencePatternCandidate::id)
				.containsExactly(pattern.id());
	}

	@Test
	void generateForWordUsesOwnedWordAndAllowsDetailCounts() throws Exception {
		AuthResult owner = signup("generate-word-controller");
		WordResponse word = wordService.create(owner.user().id(), new WordCreateRequest("brew coffee", "커피를 내리다"));
		geminiClient.enqueueSentences(List.of(new GeminiGeneratedSentence(
				"I brew coffee every morning.",
				"나는 매일 아침 커피를 내려요.",
				null,
				List.of(word.id()),
				situations())));

		mockMvc.perform(post("/api/generate/word")
						.cookie(tokenCookie(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "wordId": %d,
								  "level": "초등",
								  "count": 5
								}
								""".formatted(word.id())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.generationId").isNumber())
				.andExpect(jsonPath("$.sentences[0].sentence").value("I brew coffee every morning."))
				.andExpect(jsonPath("$.sentences[0].words[0].id").value(word.id()))
				.andExpect(jsonPath("$.sentences[0].level").value("초등"));

		GeminiSentenceGenerationRequest request = geminiClient.lastSentenceRequest();
		org.assertj.core.api.Assertions.assertThat(request.count()).isEqualTo(5);
		org.assertj.core.api.Assertions.assertThat(request.words())
				.extracting(GeminiSentenceWordCandidate::id)
				.containsExactly(word.id());
		org.assertj.core.api.Assertions.assertThat(request.patterns()).isEmpty();
	}

	@Test
	void generateForPatternUsesOwnedPatternAndWordCandidates() throws Exception {
		AuthResult owner = signup("generate-pattern-controller");
		WordResponse word = wordService.create(owner.user().id(), new WordCreateRequest("take a walk", "산책하다"));
		PatternResponse pattern = patternService.create(owner.user().id(), new PatternCreateRequest(
				"Would you like to...?",
				"제안할 때 쓴다"));
		geminiClient.enqueueSentences(List.of(new GeminiGeneratedSentence(
				"Would you like to take a walk?",
				"산책하러 갈래요?",
				pattern.id(),
				List.of(word.id()),
				situations())));

		mockMvc.perform(post("/api/generate/pattern")
						.cookie(tokenCookie(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "patternId": %d,
								  "level": "고등",
								  "count": 10
								}
								""".formatted(pattern.id())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sentences[0].pattern.id").value(pattern.id()))
				.andExpect(jsonPath("$.sentences[0].words[0].id").value(word.id()))
				.andExpect(jsonPath("$.sentences[0].level").value("고등"));

		GeminiSentenceGenerationRequest request = geminiClient.lastSentenceRequest();
		org.assertj.core.api.Assertions.assertThat(request.patterns())
				.extracting(GeminiSentencePatternCandidate::id)
				.containsExactly(pattern.id());
		org.assertj.core.api.Assertions.assertThat(request.words())
				.extracting(GeminiSentenceWordCandidate::id)
				.containsExactly(word.id());
	}

	@Test
	void invalidGenerateRequestsReturnBadRequest() throws Exception {
		AuthResult user = signup("generate-validation");

		mockMvc.perform(post("/api/generate")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "level": "대학",
								  "count": 10
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(post("/api/generate")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "level": "중등",
								  "count": 5
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(post("/api/generate/word")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "wordId": 1,
								  "level": "초등",
								  "count": 20
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(post("/api/generate/pattern")
						.cookie(tokenCookie(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "level": "중등",
								  "count": 10
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));

		mockMvc.perform(get("/api/generate/history")
						.cookie(tokenCookie(user))
						.param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void generateErrorsReturnApiDesignErrorCodes() throws Exception {
		AuthResult emptyUser = signup("generate-empty");
		mockMvc.perform(post("/api/generate")
						.cookie(tokenCookie(emptyUser))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "level": "중등",
								  "count": 10
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("NO_WORDS"));

		AuthResult noPatternUser = signup("generate-no-pattern");
		wordService.create(noPatternUser.user().id(), new WordCreateRequest("study English", "영어를 공부하다"));
		mockMvc.perform(post("/api/generate")
						.cookie(tokenCookie(noPatternUser))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "level": "중등",
								  "count": 10
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("NO_PATTERNS"));

		AuthResult owner = signup("generate-ai-failure");
		wordService.create(owner.user().id(), new WordCreateRequest("read aloud", "소리 내어 읽다"));
		patternService.create(owner.user().id(), new PatternCreateRequest("I need to...", "~해야 한다"));
		geminiClient.failSentenceGeneration(new GeminiClientException(
				GeminiOperation.SENTENCE_GENERATION,
				GeminiFailureType.NETWORK_ERROR,
				"Gemini request failed",
				false,
				null));

		mockMvc.perform(post("/api/generate")
						.cookie(tokenCookie(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "level": "고등",
								  "count": 10
								}
								"""))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error").value("AI_SERVICE_ERROR"));
	}

	@Test
	void otherUsersWordAndPatternAccessReturnsForbidden() throws Exception {
		AuthResult owner = signup("generate-forbidden-owner");
		AuthResult otherUser = signup("generate-forbidden-other");
		WordResponse word = wordService.create(owner.user().id(), new WordCreateRequest("owner word", "소유자 단어"));
		PatternResponse pattern = patternService.create(owner.user().id(), new PatternCreateRequest("owner pattern", "소유자 패턴"));

		mockMvc.perform(post("/api/generate/word")
						.cookie(tokenCookie(otherUser))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "wordId": %d,
								  "level": "초등",
								  "count": 5
								}
								""".formatted(word.id())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));

		mockMvc.perform(post("/api/generate/pattern")
						.cookie(tokenCookie(otherUser))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "patternId": %d,
								  "level": "중등",
								  "count": 10
								}
								""".formatted(pattern.id())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void historyReturnsPagedCurrentUsersGenerationHistory() throws Exception {
		AuthResult owner = signup("generate-history-owner");
		AuthResult otherUser = signup("generate-history-other");
		User ownerEntity = userRepository.findById(owner.user().id()).orElseThrow();
		User otherEntity = userRepository.findById(otherUser.user().id()).orElseThrow();
		generationHistoryRepository.saveAndFlush(new GenerationHistory(ownerEntity, "초등", 10, 9, null, null));
		generationHistoryRepository.saveAndFlush(new GenerationHistory(otherEntity, "고등", 30, 30, null, null));
		GenerationHistory latestOwnerHistory = generationHistoryRepository.saveAndFlush(
				new GenerationHistory(ownerEntity, "중등", 20, 18, null, null));

		mockMvc.perform(get("/api/generate/history")
						.cookie(tokenCookie(owner))
						.param("page", "0")
						.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value(latestOwnerHistory.getId()))
				.andExpect(jsonPath("$.content[0].level").value("중등"))
				.andExpect(jsonPath("$.content[0].requestedCount").value(20))
				.andExpect(jsonPath("$.content[0].actualCount").value(18))
				.andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());
	}

	private AuthResult signup(String prefix) {
		return authService.signup(prefix + "-" + UUID.randomUUID() + "@example.com", "password123", "tester");
	}

	private static Cookie tokenCookie(AuthResult result) {
		return new Cookie(TOKEN_COOKIE, result.token());
	}

	private static List<String> situations() {
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

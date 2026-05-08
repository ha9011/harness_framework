package com.english.generate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.english.auth.AuthResult;
import com.english.auth.AuthService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Optional;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"auth.jwt.secret=image-extraction-test-secret-that-is-at-least-32-bytes",
		"auth.jwt.expiration=PT24H"
})
class ImageExtractionControllerTest {

	private static final String TOKEN_COOKIE = "token";

	private final MockMvc mockMvc;
	private final AuthService authService;
	private final FakeExtractionGeminiClient geminiClient;

	@Autowired
	ImageExtractionControllerTest(
			MockMvc mockMvc,
			AuthService authService,
			FakeExtractionGeminiClient geminiClient
	) {
		this.mockMvc = mockMvc;
		this.authService = authService;
		this.geminiClient = geminiClient;
	}

	@BeforeEach
	void setUp() {
		geminiClient.reset();
	}

	@Test
	void extractionApisRequireAuthentication() throws Exception {
		mockMvc.perform(multipart("/api/words/extract")
						.file(image("image")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

		mockMvc.perform(multipart("/api/patterns/extract")
						.file(image("image")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void extractWordsFromMultipartImage() throws Exception {
		AuthResult user = signup("word-extract");
		geminiClient.words = List.of(
				new GeminiExtractedWord("drink coffee", "커피를 마시다"),
				new GeminiExtractedWord("make a bed", "침대를 정리하다"));

		mockMvc.perform(multipart("/api/words/extract")
						.file(image("image"))
						.cookie(tokenCookie(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].word").value("drink coffee"))
				.andExpect(jsonPath("$[0].meaning").value("커피를 마시다"))
				.andExpect(jsonPath("$[1].word").value("make a bed"));

		assertThat(geminiClient.lastWordImage.mimeType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
		assertThat(geminiClient.lastWordImage.data()).containsExactly(1, 2, 3);
	}

	@Test
	void extractPatternFromMultipartImage() throws Exception {
		AuthResult user = signup("pattern-extract");
		geminiClient.pattern = Optional.of(new GeminiExtractedPattern(
				"I'm afraid that...",
				"유감스럽게도 ~인 것 같아요",
				List.of(new GeminiExtractedPatternExample(
						"I'm afraid that we'll be late.",
						"유감스럽게도 우리는 늦을 것 같아요."))));

		mockMvc.perform(multipart("/api/patterns/extract")
						.file(image("image"))
						.cookie(tokenCookie(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.template").value("I'm afraid that..."))
				.andExpect(jsonPath("$.description").value("유감스럽게도 ~인 것 같아요"))
				.andExpect(jsonPath("$.examples[0].sentence").value("I'm afraid that we'll be late."))
				.andExpect(jsonPath("$.examples[0].translation").value("유감스럽게도 우리는 늦을 것 같아요."));

		assertThat(geminiClient.lastPatternImage.mimeType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
		assertThat(geminiClient.lastPatternImage.data()).containsExactly(1, 2, 3);
	}

	@Test
	void patternExtractionReturnsEmptyObjectWhenGeminiFindsNoPattern() throws Exception {
		AuthResult user = signup("pattern-empty");

		mockMvc.perform(multipart("/api/patterns/extract")
						.file(image("image"))
						.cookie(tokenCookie(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.template").doesNotExist());
	}

	@Test
	void invalidImageFormatReturnsBadRequest() throws Exception {
		AuthResult user = signup("invalid-image");

		mockMvc.perform(multipart("/api/words/extract")
						.file(new MockMultipartFile("image", "words.txt", MediaType.TEXT_PLAIN_VALUE, new byte[] { 1 }))
						.cookie(tokenCookie(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_IMAGE_FORMAT"));
	}

	@Test
	void geminiFailureReturnsAiServiceError() throws Exception {
		AuthResult user = signup("gemini-failure");
		geminiClient.failure = new GeminiClientException(
				GeminiOperation.WORD_IMAGE_EXTRACTION,
				GeminiFailureType.MISSING_API_KEY,
				"Gemini API key is not configured",
				false,
				null);

		mockMvc.perform(multipart("/api/words/extract")
						.file(image("image"))
						.cookie(tokenCookie(user)))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error").value("AI_SERVICE_ERROR"))
				.andExpect(jsonPath("$.message").value("이미지 추출에 실패했습니다"));
	}

	private AuthResult signup(String prefix) {
		return authService.signup(prefix + "-" + UUID.randomUUID() + "@example.com", "password123", "tester");
	}

	private static MockMultipartFile image(String name) {
		return new MockMultipartFile(name, "capture.png", MediaType.IMAGE_PNG_VALUE, new byte[] { 1, 2, 3 });
	}

	private static Cookie tokenCookie(AuthResult result) {
		return new Cookie(TOKEN_COOKIE, result.token());
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		@Primary
		FakeExtractionGeminiClient fakeExtractionGeminiClient() {
			return new FakeExtractionGeminiClient();
		}
	}

	static class FakeExtractionGeminiClient implements GeminiClient {

		private List<GeminiExtractedWord> words = List.of();
		private Optional<GeminiExtractedPattern> pattern = Optional.empty();
		private GeminiClientException failure;
		private GeminiImage lastWordImage;
		private GeminiImage lastPatternImage;

		void reset() {
			words = List.of();
			pattern = Optional.empty();
			failure = null;
			lastWordImage = null;
			lastPatternImage = null;
		}

		@Override
		public List<GeminiWordEnrichment> enrichWords(List<GeminiWordInput> words) {
			throw new UnsupportedOperationException("word enrichment is not used by image extraction tests");
		}

		@Override
		public List<GeminiExtractedWord> extractWordsFromImage(GeminiImage image) {
			lastWordImage = image;
			if (failure != null) {
				throw failure;
			}
			return words;
		}

		@Override
		public Optional<GeminiExtractedPattern> extractPatternFromImage(GeminiImage image) {
			lastPatternImage = image;
			if (failure != null) {
				throw failure;
			}
			return pattern;
		}

		@Override
		public List<GeminiGeneratedSentence> generateSentences(GeminiSentenceGenerationRequest request) {
			throw new UnsupportedOperationException("sentence generation is not used by image extraction tests");
		}
	}
}

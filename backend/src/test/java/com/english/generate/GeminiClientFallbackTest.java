package com.english.generate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeminiClientFallbackTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void enrichWordsRetriesInvalidJsonAndThrowsFallbackCapableException() throws Exception {
		try (FakeGeminiServer server = new FakeGeminiServer()) {
			server.enqueueJsonText("not-json");
			server.enqueueJsonText("{\"id\": 1}");
			server.enqueueJsonText("[]");
			GeminiClient client = client(server, "test-key");

			Throwable throwable = catchThrowable(() -> client.enrichWords(List.of(
					new GeminiWordInput(1L, "make a bed", "침대를 정리하다"))));

			assertThat(throwable).isInstanceOf(GeminiClientException.class);
			GeminiClientException exception = (GeminiClientException) throwable;
			assertThat(exception.getOperation()).isEqualTo(GeminiOperation.WORD_ENRICHMENT);
			assertThat(exception.getFailureType()).isEqualTo(GeminiFailureType.PARSING_ERROR);
			assertThat(exception.isFallbackRecommended()).isTrue();
			assertThat(server.requestCount()).isEqualTo(3);
		}
	}

	@Test
	void generateSentencesRetriesServerErrorAndRateLimitBeforeSuccess() throws Exception {
		try (FakeGeminiServer server = new FakeGeminiServer()) {
			server.enqueue(500, "{\"error\":\"temporary\"}");
			server.enqueue(429, "{\"error\":\"rate limited\"}");
			server.enqueueJsonText("""
					[
					  {
					    "sentence": "I used to drink coffee here.",
					    "translation": "나는 여기서 커피를 마시곤 했다.",
					    "patternId": 2,
					    "wordIds": [1],
					    "situations": [
					      "카페 앞에서 친구와 이야기하는 상황",
					      "예전 출근길을 떠올리는 상황",
					      "동료에게 추억을 말하는 상황",
					      "오랜만에 동네에 온 상황",
					      "사진을 보며 회상하는 상황"
					    ]
					  }
					]
					""");
			GeminiClient client = client(server, "test-key");

			List<GeminiGeneratedSentence> result = client.generateSentences(
					new GeminiSentenceGenerationRequest(
							"초등",
							10,
							List.of(new GeminiSentenceWordCandidate(1L, "drink coffee", "커피를 마시다")),
							List.of(new GeminiSentencePatternCandidate(2L, "I used to...", "과거 습관"))));

			assertThat(result).hasSize(1);
			assertThat(result.getFirst().sentence()).isEqualTo("I used to drink coffee here.");
			assertThat(server.requestCount()).isEqualTo(3);
		}
	}

	@Test
	void missingApiKeyFailsBeforeNetworkRequestAndAllowsWordEnrichmentFallback() throws Exception {
		try (FakeGeminiServer server = new FakeGeminiServer()) {
			GeminiClient client = client(server, " ");

			Throwable throwable = catchThrowable(() -> client.enrichWords(List.of(
					new GeminiWordInput(1L, "coffee", "커피"))));

			assertThat(throwable).isInstanceOf(GeminiClientException.class);
			GeminiClientException exception = (GeminiClientException) throwable;
			assertThat(exception.getFailureType()).isEqualTo(GeminiFailureType.MISSING_API_KEY);
			assertThat(exception.getOperation()).isEqualTo(GeminiOperation.WORD_ENRICHMENT);
			assertThat(exception.isFallbackRecommended()).isTrue();
			assertThat(server.requestCount()).isZero();
		}
	}

	private GeminiClient client(FakeGeminiServer server, String apiKey) {
		return new GeminiApiClient(
				objectMapper,
				server.baseUri(),
				"test-model",
				apiKey,
				2,
				Duration.ZERO);
	}
}

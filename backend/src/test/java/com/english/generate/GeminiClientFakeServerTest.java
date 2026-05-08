package com.english.generate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeminiClientFakeServerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void enrichWordsSendsApiKeyAndResponseSchemaAndParsesWordMetadata() throws Exception {
		try (FakeGeminiServer server = new FakeGeminiServer()) {
			server.enqueueJsonText("""
					[
					  {
					    "id": 1,
					    "partOfSpeech": "phrase",
					    "pronunciation": "/meɪk ə bɛd/",
					    "synonyms": "tidy up the bed",
					    "tip": "make the bed도 같은 의미"
					  }
					]
					""");
			GeminiClient client = client(server);

			List<GeminiWordEnrichment> result = client.enrichWords(List.of(
					new GeminiWordInput(1L, "make a bed", "침대를 정리하다")));

			assertThat(result)
					.extracting(GeminiWordEnrichment::id, GeminiWordEnrichment::partOfSpeech)
					.containsExactly(org.assertj.core.groups.Tuple.tuple(1L, "phrase"));
			FakeGeminiServer.RecordedRequest request = server.lastRequest();
			JsonNode body = objectMapper.readTree(request.body());
			assertThat(request.uri().getPath()).isEqualTo("/v1beta/models/test-model:generateContent");
			assertThat(request.header("x-goog-api-key")).isEqualTo("test-key");
			assertThat(body.at("/generationConfig/responseMimeType").asText()).isEqualTo("application/json");
			assertThat(body.at("/generationConfig/responseSchema/type").asText()).isEqualTo("array");
			assertThat(body.at("/contents/0/parts/0/text").asText()).contains("make a bed");
			assertThat(body.at("/contents/0/parts/0/text").asText()).contains("id");
		}
	}

	@Test
	void extractWordsFromImageSendsInlineDataAndParsesWords() throws Exception {
		try (FakeGeminiServer server = new FakeGeminiServer()) {
			server.enqueueJsonText("""
					[
					  {"word": "drink coffee", "meaning": "커피를 마시다"},
					  {"word": "make a bed", "meaning": "침대를 정리하다"}
					]
					""");
			GeminiClient client = client(server);

			List<GeminiExtractedWord> result = client.extractWordsFromImage(
					new GeminiImage("image/png", new byte[] { 1, 2, 3 }));

			assertThat(result)
					.extracting(GeminiExtractedWord::word, GeminiExtractedWord::meaning)
					.containsExactly(
							org.assertj.core.groups.Tuple.tuple("drink coffee", "커피를 마시다"),
							org.assertj.core.groups.Tuple.tuple("make a bed", "침대를 정리하다"));
			JsonNode body = objectMapper.readTree(server.lastRequest().body());
			assertThat(body.at("/contents/0/parts/1/inline_data/mime_type").asText()).isEqualTo("image/png");
			assertThat(body.at("/contents/0/parts/1/inline_data/data").asText()).isEqualTo("AQID");
			assertThat(body.at("/generationConfig/responseSchema/type").asText()).isEqualTo("array");
		}
	}

	@Test
	void extractPatternFromImageParsesPatternAndExamples() throws Exception {
		try (FakeGeminiServer server = new FakeGeminiServer()) {
			server.enqueueJsonText("""
					{
					  "found": true,
					  "pattern": {
					    "template": "I'm afraid that...",
					    "description": "유감스럽게도 ~인 것 같아요",
					    "examples": [
					      {
					        "sentence": "I'm afraid that we'll be late.",
					        "translation": "유감스럽게도 우리는 늦을 것 같아요."
					      }
					    ]
					  }
					}
					""");
			GeminiClient client = client(server);

			GeminiExtractedPattern result = client.extractPatternFromImage(
					new GeminiImage("image/jpeg", new byte[] { 4, 5, 6 })).orElseThrow();

			assertThat(result.template()).isEqualTo("I'm afraid that...");
			assertThat(result.examples())
					.extracting(GeminiExtractedPatternExample::sentence)
					.containsExactly("I'm afraid that we'll be late.");
			JsonNode body = objectMapper.readTree(server.lastRequest().body());
			assertThat(body.at("/generationConfig/responseSchema/properties/found/type").asText()).isEqualTo("boolean");
			assertThat(body.at("/contents/0/parts/1/inline_data/mime_type").asText()).isEqualTo("image/jpeg");
		}
	}

	@Test
	void generateSentencesSendsCandidatesAndParsesStructuredSentences() throws Exception {
		try (FakeGeminiServer server = new FakeGeminiServer()) {
			server.enqueueJsonText("""
					[
					  {
					    "sentence": "I'm afraid that I forgot to make my bed.",
					    "translation": "유감스럽게도 침대 정리를 깜빡한 것 같아요.",
					    "patternId": 7,
					    "wordIds": [3],
					    "situations": [
					      "아침에 급하게 나와 전화하는 상황",
					      "룸메이트에게 말하는 상황",
					      "아이에게 생활 습관을 알려주는 상황",
					      "호텔 직원에게 사과하는 상황",
					      "친구에게 오늘 아침 일을 말하는 상황"
					    ]
					  }
					]
					""");
			GeminiClient client = client(server);

			List<GeminiGeneratedSentence> result = client.generateSentences(
					new GeminiSentenceGenerationRequest(
							"중등",
							10,
							List.of(new GeminiSentenceWordCandidate(3L, "make a bed", "침대를 정리하다")),
							List.of(new GeminiSentencePatternCandidate(7L, "I'm afraid that...", "유감스럽게도 ~인 것 같아요"))));

			assertThat(result).hasSize(1);
			assertThat(result.getFirst().patternId()).isEqualTo(7L);
			assertThat(result.getFirst().wordIds()).containsExactly(3L);
			assertThat(result.getFirst().situations()).hasSize(5);
			JsonNode body = objectMapper.readTree(server.lastRequest().body());
			assertThat(body.at("/contents/0/parts/0/text").asText()).contains("중등");
			assertThat(body.at("/contents/0/parts/0/text").asText()).contains("make a bed");
			assertThat(body.at("/contents/0/parts/0/text").asText()).contains("I'm afraid that...");
			assertThat(body.at("/generationConfig/responseSchema/items/properties/situations/type").asText()).isEqualTo("array");
		}
	}

	private GeminiClient client(FakeGeminiServer server) {
		return new GeminiApiClient(
				objectMapper,
				server.baseUri(),
				"test-model",
				"test-key",
				2,
				Duration.ZERO);
	}
}

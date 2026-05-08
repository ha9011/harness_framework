package com.english.generate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiApiClient implements GeminiClient {

	private static final String JSON_MIME_TYPE = "application/json";

	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final URI baseUri;
	private final String model;
	private final String apiKey;
	private final int maxRetries;
	private final Duration retryDelay;

	@Autowired
	public GeminiApiClient(
			ObjectMapper objectMapper,
			@Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
			@Value("${gemini.model:gemini-2.5-flash}") String model,
			@Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey
	) {
		this(objectMapper, URI.create(baseUrl), model, apiKey, 2, Duration.ofSeconds(1));
	}

	GeminiApiClient(
			ObjectMapper objectMapper,
			URI baseUri,
			String model,
			String apiKey,
			int maxRetries,
			Duration retryDelay
	) {
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newHttpClient();
		this.baseUri = baseUri;
		this.model = GeminiValidation.requireText(model, "model");
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.maxRetries = maxRetries;
		this.retryDelay = retryDelay == null ? Duration.ZERO : retryDelay;
	}

	@Override
	public List<GeminiWordEnrichment> enrichWords(List<GeminiWordInput> words) {
		List<GeminiWordInput> inputs = GeminiValidation.requireNonEmptyList(words, "words");
		Map<String, Object> request = textRequest(
				wordEnrichmentPrompt(inputs),
				wordEnrichmentSchema());

		return execute(
				GeminiOperation.WORD_ENRICHMENT,
				request,
				text -> parseWordEnrichments(text, inputs));
	}

	@Override
	public List<GeminiExtractedWord> extractWordsFromImage(GeminiImage image) {
		Map<String, Object> request = imageRequest(
				"""
				이미지에서 영어 단어 또는 영어 표현과 한국어 뜻을 추출해 JSON 배열로 반환하세요.
				이미지에 학습할 단어가 없으면 빈 배열을 반환하세요.
				""",
				image,
				extractedWordsSchema());

		return execute(
				GeminiOperation.WORD_IMAGE_EXTRACTION,
				request,
				text -> parse(text, new TypeReference<List<GeminiExtractedWord>>() {
				}));
	}

	@Override
	public Optional<GeminiExtractedPattern> extractPatternFromImage(GeminiImage image) {
		Map<String, Object> request = imageRequest(
				"""
				이미지에서 영어 패턴, 한국어 설명, 교재 예문과 해석을 추출해 JSON으로 반환하세요.
				패턴을 찾을 수 없으면 found=false로 반환하세요.
				예문 순서는 이미지에 나온 순서를 유지하세요.
				""",
				image,
				extractedPatternSchema());

		return execute(
				GeminiOperation.PATTERN_IMAGE_EXTRACTION,
				request,
				this::parsePatternExtraction);
	}

	@Override
	public List<GeminiGeneratedSentence> generateSentences(GeminiSentenceGenerationRequest request) {
		Map<String, Object> body = textRequest(
				sentenceGenerationPrompt(request),
				generatedSentencesSchema());

		return execute(
				GeminiOperation.SENTENCE_GENERATION,
				body,
				text -> parse(text, new TypeReference<List<GeminiGeneratedSentence>>() {
				}));
	}

	private <T> T execute(GeminiOperation operation, Map<String, Object> requestBody, ResponseParser<T> parser) {
		if (apiKey.isBlank()) {
			throw exception(
					operation,
					GeminiFailureType.MISSING_API_KEY,
					"Gemini API key is not configured",
					false,
					null);
		}

		GeminiClientException lastException = null;
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				String body = objectMapper.writeValueAsString(requestBody);
				HttpRequest request = HttpRequest.newBuilder(generateContentUri())
						.header("Content-Type", JSON_MIME_TYPE)
						.header("x-goog-api-key", apiKey)
						.timeout(Duration.ofSeconds(20))
						.POST(HttpRequest.BodyPublishers.ofString(body))
						.build();

				HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				if (shouldRetryStatus(response.statusCode())) {
					throw exception(
							operation,
							failureType(response.statusCode()),
							"Gemini API returned retryable status " + response.statusCode(),
							true,
							null);
				}
				if (response.statusCode() < 200 || response.statusCode() >= 300) {
					throw exception(
							operation,
							failureType(response.statusCode()),
							"Gemini API returned status " + response.statusCode(),
							false,
							null);
				}

				String text = extractCandidateText(response.body());
				return parser.parse(text);
			}
			catch (JsonProcessingException | IllegalArgumentException exception) {
				lastException = exception(
						operation,
						GeminiFailureType.PARSING_ERROR,
						"Gemini response parsing failed",
						true,
						exception);
			}
			catch (IOException exception) {
				lastException = exception(
						operation,
						GeminiFailureType.NETWORK_ERROR,
						"Gemini API request failed",
						true,
						exception);
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw exception(
						operation,
						GeminiFailureType.NETWORK_ERROR,
						"Gemini API request was interrupted",
						false,
						exception);
			}
			catch (GeminiClientException exception) {
				lastException = exception;
			}

			if (lastException == null || !lastException.isRetryable() || attempt == maxRetries) {
				throw lastException;
			}
			sleepBeforeRetry(operation, attempt);
		}

		throw lastException;
	}

	private URI generateContentUri() {
		String base = baseUri.toString();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8).replace("+", "%20");
		return URI.create(base + "/v1beta/models/" + encodedModel + ":generateContent");
	}

	private String extractCandidateText(String body) throws JsonProcessingException {
		GeminiGenerateContentResponse response = objectMapper.readValue(body, GeminiGenerateContentResponse.class);
		if (response.candidates() == null) {
			throw new IllegalArgumentException("Gemini response has no candidates");
		}

		return response.candidates().stream()
				.filter(candidate -> candidate.content() != null && candidate.content().parts() != null)
				.flatMap(candidate -> candidate.content().parts().stream())
				.map(GeminiGenerateContentPart::text)
				.filter(text -> text != null && !text.isBlank())
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Gemini response has no text part"));
	}

	private List<GeminiWordEnrichment> parseWordEnrichments(String text, List<GeminiWordInput> inputs)
			throws JsonProcessingException {
		List<GeminiWordEnrichment> enrichments = parse(text, new TypeReference<List<GeminiWordEnrichment>>() {
		});
		Set<Long> requestedIds = inputs.stream()
				.map(GeminiWordInput::id)
				.collect(Collectors.toSet());
		Set<Long> responseIds = enrichments.stream()
				.map(GeminiWordEnrichment::id)
				.collect(Collectors.toSet());
		if (!responseIds.containsAll(requestedIds)) {
			throw new IllegalArgumentException("Gemini word enrichment response missed requested ids");
		}
		return enrichments;
	}

	private Optional<GeminiExtractedPattern> parsePatternExtraction(String text) throws JsonProcessingException {
		GeminiPatternExtractionPayload payload = objectMapper.readValue(text, GeminiPatternExtractionPayload.class);
		if (!payload.found()) {
			return Optional.empty();
		}
		if (payload.pattern() == null) {
			throw new IllegalArgumentException("Gemini pattern extraction response missed pattern");
		}
		return Optional.of(payload.pattern());
	}

	private <T> T parse(String text, TypeReference<T> typeReference) throws JsonProcessingException {
		return objectMapper.readValue(text, typeReference);
	}

	private Map<String, Object> textRequest(String prompt, Map<String, Object> schema) {
		return generateContentRequest(List.of(Map.of("text", prompt)), schema);
	}

	private Map<String, Object> imageRequest(String prompt, GeminiImage image, Map<String, Object> schema) {
		return generateContentRequest(
				List.of(
						Map.of("text", prompt),
						Map.of("inline_data", Map.of(
								"mime_type", image.mimeType(),
								"data", Base64.getEncoder().encodeToString(image.data())))),
				schema);
	}

	private Map<String, Object> generateContentRequest(List<Map<String, Object>> parts, Map<String, Object> schema) {
		return Map.of(
				"contents", List.of(Map.of(
						"role", "user",
						"parts", parts)),
				"generationConfig", Map.of(
						"responseMimeType", JSON_MIME_TYPE,
						"responseSchema", schema));
	}

	private String wordEnrichmentPrompt(List<GeminiWordInput> words) {
		return """
				다음 영어 단어들의 품사, 발음, 유의어, 한줄 학습 팁을 JSON schema에 맞춰 반환하세요.
				id는 입력 id를 그대로 사용하세요.
				입력:
				%s
				""".formatted(toJson(words));
	}

	private String sentenceGenerationPrompt(GeminiSentenceGenerationRequest request) {
		return """
				다음 영어 단어와 패턴을 조합하여 %s 수준의 자연스러운 예문을 %d개 만들어주세요.
				시험 지문이 아니라 실제 일상에서 말할 법한 문장만 생성하세요.
				각 예문에는 사용한 wordIds, patternId, 영어 예문, 한국어 해석, 구체적인 상황 5개를 포함하세요.
				패턴 목록이 비어 있으면 patternId를 생략하거나 null로 두세요.

				단어:
				%s

				패턴:
				%s
				""".formatted(
				request.level(),
				request.count(),
				toJson(request.words()),
				toJson(request.patterns()));
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Failed to serialize Gemini prompt payload", exception);
		}
	}

	private Map<String, Object> wordEnrichmentSchema() {
		return arraySchema(objectSchema(
				List.of("id", "partOfSpeech", "pronunciation", "synonyms", "tip"),
				Map.of(
						"id", integerSchema(),
						"partOfSpeech", stringSchema(),
						"pronunciation", stringSchema(),
						"synonyms", stringSchema(),
						"tip", stringSchema())));
	}

	private Map<String, Object> extractedWordsSchema() {
		return arraySchema(objectSchema(
				List.of("word", "meaning"),
				Map.of(
						"word", stringSchema(),
						"meaning", stringSchema())));
	}

	private Map<String, Object> extractedPatternSchema() {
		Map<String, Object> example = objectSchema(
				List.of("sentence", "translation"),
				Map.of(
						"sentence", stringSchema(),
						"translation", stringSchema()));
		Map<String, Object> pattern = objectSchema(
				List.of("template", "description", "examples"),
				Map.of(
						"template", stringSchema(),
						"description", stringSchema(),
						"examples", arraySchema(example)));
		return objectSchema(
				List.of("found"),
				Map.of(
						"found", booleanSchema(),
						"pattern", pattern));
	}

	private Map<String, Object> generatedSentencesSchema() {
		return arraySchema(objectSchema(
				List.of("sentence", "translation", "wordIds", "situations"),
				Map.of(
						"sentence", stringSchema(),
						"translation", stringSchema(),
						"patternId", integerSchema(),
						"wordIds", arraySchema(integerSchema()),
						"situations", arraySchema(stringSchema()))));
	}

	private static Map<String, Object> objectSchema(List<String> required, Map<String, Object> properties) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", properties);
		schema.put("required", required);
		return schema;
	}

	private static Map<String, Object> arraySchema(Map<String, Object> items) {
		return Map.of(
				"type", "array",
				"items", items);
	}

	private static Map<String, Object> stringSchema() {
		return Map.of("type", "string");
	}

	private static Map<String, Object> integerSchema() {
		return Map.of("type", "integer");
	}

	private static Map<String, Object> booleanSchema() {
		return Map.of("type", "boolean");
	}

	private static boolean shouldRetryStatus(int statusCode) {
		return statusCode == 429 || statusCode >= 500;
	}

	private static GeminiFailureType failureType(int statusCode) {
		return statusCode == 429 ? GeminiFailureType.RATE_LIMIT : GeminiFailureType.HTTP_ERROR;
	}

	private static GeminiClientException exception(
			GeminiOperation operation,
			GeminiFailureType failureType,
			String message,
			boolean retryable,
			Throwable cause
	) {
		return new GeminiClientException(operation, failureType, message, retryable, cause);
	}

	private void sleepBeforeRetry(GeminiOperation operation, int attempt) {
		if (retryDelay.isZero() || retryDelay.isNegative()) {
			return;
		}
		long multiplier = attempt == 0 ? 1 : 3;
		try {
			Thread.sleep(retryDelay.multipliedBy(multiplier).toMillis());
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new GeminiClientException(
					operation,
					GeminiFailureType.NETWORK_ERROR,
					"Gemini retry sleep was interrupted",
					false,
					exception);
		}
	}

	@FunctionalInterface
	private interface ResponseParser<T> {
		T parse(String text) throws JsonProcessingException;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiGenerateContentResponse(List<GeminiGenerateContentCandidate> candidates) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiGenerateContentCandidate(GeminiGenerateContentContent content) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiGenerateContentContent(List<GeminiGenerateContentPart> parts) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiGenerateContentPart(String text) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record GeminiPatternExtractionPayload(boolean found, GeminiExtractedPattern pattern) {
	}
}

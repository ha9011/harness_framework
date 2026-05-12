package com.english.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

    private final String apiKey;
    private final String apiUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final long[] retryDelaysMs;

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_RETRIES = 3;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiClient(@Value("${gemini.api-key}") String apiKey,
                        @Value("${gemini.model}") String model) {
        this.apiKey = apiKey;
        this.apiUrl = BASE_URL + model + ":generateContent";
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.retryDelaysMs = new long[]{0, 1000, 3000};
    }

    // 테스트용 생성자
    GeminiClient(String apiKey, String apiUrl, RestTemplate restTemplate, long[] retryDelaysMs) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.retryDelaysMs = retryDelaysMs;
    }

    /**
     * 텍스트 프롬프트로 Gemini API 호출 후 JSON 응답 파싱
     */
    public <T> T generateContent(String prompt, Class<T> responseType) {
        Map<String, Object> requestBody = buildTextRequest(prompt);
        String jsonResponse = callWithRetry(requestBody);
        return parseResponse(jsonResponse, responseType);
    }

    /**
     * 이미지 + 텍스트 프롬프트로 Gemini API 호출 후 JSON 응답 파싱
     */
    public <T> T generateContentWithImage(byte[] imageData, String mimeType, String prompt, Class<T> responseType) {
        Map<String, Object> requestBody = buildImageRequest(imageData, mimeType, prompt);
        String jsonResponse = callWithRetry(requestBody);
        return parseResponse(jsonResponse, responseType);
    }

    private Map<String, Object> buildTextRequest(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );
    }

    private Map<String, Object> buildImageRequest(byte[] imageData, String mimeType, String prompt) {
        String base64Image = Base64.getEncoder().encodeToString(imageData);
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("inlineData", Map.of(
                                        "mimeType", mimeType,
                                        "data", base64Image
                                )),
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );
    }

    private String callWithRetry(Map<String, Object> requestBody) {
        String url = apiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                if (retryDelaysMs[attempt] > 0) {
                    Thread.sleep(retryDelaysMs[attempt]);
                }

                String body = objectMapper.writeValueAsString(requestBody);
                HttpEntity<String> entity = new HttpEntity<>(body, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);

                return extractText(response.getBody());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GeminiException("Gemini API 호출이 인터럽트되었습니다", e);
            } catch (Exception e) {
                lastException = e;
                log.warn("Gemini API 호출 실패 (시도 {}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());
            }
        }

        throw new GeminiException("Gemini API 호출이 " + MAX_RETRIES + "회 모두 실패했습니다", lastException);
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode()) {
                throw new GeminiException("Gemini 응답에서 텍스트를 찾을 수 없습니다");
            }
            return textNode.asText();
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Gemini 응답 파싱 실패", e);
        }
    }

    private <T> T parseResponse(String jsonText, Class<T> responseType) {
        try {
            return objectMapper.readValue(jsonText, responseType);
        } catch (Exception e) {
            throw new GeminiException("Gemini JSON 응답을 " + responseType.getSimpleName() + "로 변환 실패", e);
        }
    }
}

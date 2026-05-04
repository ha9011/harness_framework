package com.english.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiClientTest {

    private MockWebServer mockWebServer;
    private GeminiClient geminiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/v1beta/models/gemini-2.0-flash:generateContent").toString();
        geminiClient = new GeminiClient("test-key", baseUrl, new RestTemplate(), new long[]{0, 0, 0});
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("정상 응답 시 JSON 파싱 성공")
    void generateContent_success() {
        enqueueSuccessResponse("{\"word\":\"hello\",\"meaning\":\"안녕하세요\"}");

        TestResponse result = geminiClient.generateContent("test prompt", TestResponse.class);

        assertThat(result.word).isEqualTo("hello");
        assertThat(result.meaning).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("1회 실패 후 2회째 성공")
    void generateContent_retryOnceAndSuccess() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        enqueueSuccessResponse("{\"word\":\"retry\",\"meaning\":\"재시도\"}");

        TestResponse result = geminiClient.generateContent("test prompt", TestResponse.class);

        assertThat(result.word).isEqualTo("retry");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("3회 모두 실패 시 GeminiException 발생")
    void generateContent_allRetriesFail() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error 1"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error 2"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error 3"));

        assertThatThrownBy(() -> geminiClient.generateContent("test prompt", TestResponse.class))
                .isInstanceOf(GeminiException.class)
                .hasMessageContaining("3회 모두 실패");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("이미지 포함 요청 정상 동작")
    void generateContentWithImage_success() {
        enqueueSuccessResponse("{\"word\":\"image\",\"meaning\":\"이미지\"}");

        byte[] imageData = new byte[]{1, 2, 3};
        TestResponse result = geminiClient.generateContentWithImage(
                imageData, "image/png", "extract text", TestResponse.class);

        assertThat(result.word).isEqualTo("image");
    }

    private void enqueueSuccessResponse(String jsonContent) {
        String escaped = jsonContent.replace("\"", "\\\"");
        String geminiResponse = String.format("""
                {
                    "candidates": [{
                        "content": {
                            "parts": [{
                                "text": "%s"
                            }]
                        }
                    }]
                }
                """, escaped);
        mockWebServer.enqueue(new MockResponse()
                .setBody(geminiResponse)
                .setHeader("Content-Type", "application/json"));
    }

    static class TestResponse {
        public String word;
        public String meaning;
    }
}

package com.english.integration;

import com.english.config.GeminiException;
import com.english.config.ErrorResponse;
import com.english.generate.GenerateRequest;
import com.english.pattern.PatternCreateRequest;
import com.english.pattern.PatternResponse;
import com.english.word.WordCreateRequest;
import com.english.word.WordEnrichment;
import com.english.word.WordRepository;
import com.english.word.WordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GeminiFallbackIntegrationTest extends IntegrationTestBase {

    @Autowired
    private WordRepository wordRepository;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        authHeaders = getDefaultAuthHeaders();
    }

    @Test
    @DisplayName("단어 등록 시 Gemini 실패 → 보강 없이 저장")
    void createWord_geminiFails_savedWithoutEnrichment() {
        // given
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willThrow(new GeminiException("Gemini API 호출이 3회 모두 실패했습니다"));

        // when
        ResponseEntity<WordResponse> response = restTemplate.exchange(
                "/api/words", HttpMethod.POST,
                new HttpEntity<>(new WordCreateRequest("test", "테스트"), authHeaders),
                WordResponse.class);

        // then - 보강 없이 저장 성공
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getWord()).isEqualTo("test");
        assertThat(response.getBody().getMeaning()).isEqualTo("테스트");
        // 보강 정보 없음
        assertThat(response.getBody().getPartOfSpeech()).isNull();

        // DB 확인
        assertThat(wordRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("예문 생성 시 Gemini 실패 → 502 에러")
    void generate_geminiFails_returns502() {
        // given - 단어/패턴 등록 (보강은 성공)
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/test/", "syn", "tip"));

        restTemplate.exchange("/api/words", HttpMethod.POST,
                new HttpEntity<>(new WordCreateRequest("hello", "안녕"), authHeaders),
                WordResponse.class);
        restTemplate.exchange("/api/patterns", HttpMethod.POST,
                new HttpEntity<>(new PatternCreateRequest("I want to ~", "~하고 싶다", List.of(
                        new PatternCreateRequest.ExampleRequest("I want to go", "가고 싶다")
                )), authHeaders), PatternResponse.class);

        // 예문 생성 시 Gemini 실패
        given(geminiClient.generateContent(anyString(), eq(com.english.generate.GeminiGenerateResponse.class)))
                .willThrow(new GeminiException("Gemini API 호출이 3회 모두 실패했습니다"));

        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/generate", HttpMethod.POST,
                new HttpEntity<>(new GenerateRequest("ELEMENTARY", 10, null, null), authHeaders),
                ErrorResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().getCode()).isEqualTo("AI_SERVICE_ERROR");
    }
}

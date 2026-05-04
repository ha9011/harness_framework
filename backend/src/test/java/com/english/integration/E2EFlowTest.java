package com.english.integration;

import com.english.dashboard.DashboardResponse;
import com.english.generate.*;
import com.english.pattern.PatternCreateRequest;
import com.english.pattern.PatternResponse;
import com.english.review.ReviewCardResponse;
import com.english.review.ReviewResultRequest;
import com.english.review.ReviewResultResponse;
import com.english.word.WordCreateRequest;
import com.english.word.WordEnrichment;
import com.english.word.WordResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class E2EFlowTest extends IntegrationTestBase {

    @Test
    @DisplayName("E2E: 단어5개 -> 패턴2개 -> 예문10개 -> 복습 -> SM-2 -> 대시보드 -> 학습기록")
    void fullE2EFlow() {
        // Step 1: 단어 5개 등록
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/test/", "syn", "tip"));

        String[] words = {"apple", "banana", "coffee", "desk", "email"};
        String[] meanings = {"사과", "바나나", "커피", "책상", "이메일"};

        for (int i = 0; i < 5; i++) {
            ResponseEntity<WordResponse> res = restTemplate.postForEntity(
                    "/api/words", new WordCreateRequest(words[i], meanings[i]), WordResponse.class);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Step 2: 패턴 2개 등록
        restTemplate.postForEntity("/api/patterns",
                new PatternCreateRequest("I want to ~", "~하고 싶다", List.of(
                        new PatternCreateRequest.ExampleRequest("I want to eat", "먹고 싶다")
                )), PatternResponse.class);

        restTemplate.postForEntity("/api/patterns",
                new PatternCreateRequest("Let me ~", "~할게요", List.of(
                        new PatternCreateRequest.ExampleRequest("Let me try", "시도할게요")
                )), PatternResponse.class);

        // Step 3: 예문 10개 생성
        List<GeminiGenerateResponse.GeminiSentence> sentences = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sentences.add(new GeminiGenerateResponse.GeminiSentence(
                    "I want to eat " + words[i % 5] + ".",
                    "나는 " + meanings[i % 5] + "를 먹고 싶다.",
                    List.of((long) (i % 5 + 1)),
                    List.of("상황1", "상황2", "상황3", "상황4", "상황5")
            ));
        }
        GeminiGenerateResponse geminiResponse = new GeminiGenerateResponse(sentences);

        given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                .willReturn(geminiResponse);

        ResponseEntity<GenerateResponse> genRes = restTemplate.postForEntity(
                "/api/generate", new GenerateRequest("ELEMENTARY", 10, null, null), GenerateResponse.class);
        assertThat(genRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(genRes.getBody().getSentences()).hasSize(10);

        // Step 4: 복습 카드 조회 (WORD 탭)
        ResponseEntity<ReviewCardResponse[]> reviewRes = restTemplate.getForEntity(
                "/api/reviews/today?type=WORD", ReviewCardResponse[].class);
        assertThat(reviewRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reviewRes.getBody().length).isGreaterThan(0);

        // Step 5: SM-2 제출 (EASY, MEDIUM, HARD 각 1개)
        ReviewCardResponse[] cards = reviewRes.getBody();

        // EASY 제출
        ResponseEntity<ReviewResultResponse> easyRes = restTemplate.postForEntity(
                "/api/reviews/" + cards[0].getId(),
                new ReviewResultRequest("EASY"), ReviewResultResponse.class);
        assertThat(easyRes.getBody().getResult()).isEqualTo("EASY");
        assertThat(easyRes.getBody().getIntervalDays()).isEqualTo(3); // round(1 * 2.5 * 1.3) = 3

        if (cards.length > 1) {
            // MEDIUM 제출
            ResponseEntity<ReviewResultResponse> medRes = restTemplate.postForEntity(
                    "/api/reviews/" + cards[1].getId(),
                    new ReviewResultRequest("MEDIUM"), ReviewResultResponse.class);
            assertThat(medRes.getBody().getResult()).isEqualTo("MEDIUM");
            assertThat(medRes.getBody().getIntervalDays()).isEqualTo(3); // round(1 * 2.5) = 3
        }

        if (cards.length > 2) {
            // HARD 제출
            ResponseEntity<ReviewResultResponse> hardRes = restTemplate.postForEntity(
                    "/api/reviews/" + cards[2].getId(),
                    new ReviewResultRequest("HARD"), ReviewResultResponse.class);
            assertThat(hardRes.getBody().getResult()).isEqualTo("HARD");
            assertThat(hardRes.getBody().getIntervalDays()).isEqualTo(1); // HARD -> 1 리셋
        }

        // Step 6: 대시보드 조회 -> 카운트 검증
        ResponseEntity<DashboardResponse> dashRes = restTemplate.getForEntity(
                "/api/dashboard", DashboardResponse.class);
        assertThat(dashRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashRes.getBody().getWordCount()).isEqualTo(5);
        assertThat(dashRes.getBody().getPatternCount()).isEqualTo(2);
        assertThat(dashRes.getBody().getSentenceCount()).isEqualTo(10);

        // Step 7: 학습 기록 조회 -> Day 1 확인
        ResponseEntity<String> recordsRes = restTemplate.getForEntity(
                "/api/study-records?page=0&size=10", String.class);
        assertThat(recordsRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recordsRes.getBody()).contains("\"dayNumber\":1");
    }
}

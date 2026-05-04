package com.english.integration;

import com.english.review.*;
import com.english.word.WordCreateRequest;
import com.english.word.WordEnrichment;
import com.english.word.WordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReviewItemRepository reviewItemRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @BeforeEach
    void setUp() {
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/test/", "syn", "tip"));
    }

    @Test
    @DisplayName("카드 선정 → SM-2 제출 → next_review_date 변경 + review_log 기록")
    void reviewFlow_sm2Applied() {
        // given - 단어 등록 (review_items 자동 생성됨)
        restTemplate.postForEntity("/api/words",
                new WordCreateRequest("test", "테스트"), WordResponse.class);

        // when - 오늘 카드 조회
        ResponseEntity<ReviewCardResponse[]> cardsResponse = restTemplate.getForEntity(
                "/api/reviews/today?type=WORD", ReviewCardResponse[].class);

        assertThat(cardsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cardsResponse.getBody()).isNotNull();
        assertThat(cardsResponse.getBody().length).isGreaterThan(0);

        Long reviewItemId = cardsResponse.getBody()[0].getId();

        // SM-2 제출 (EASY)
        ReviewResultRequest request = new ReviewResultRequest("EASY");
        ResponseEntity<ReviewResultResponse> resultResponse = restTemplate.postForEntity(
                "/api/reviews/" + reviewItemId, request, ReviewResultResponse.class);

        // then
        assertThat(resultResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultResponse.getBody().getResult()).isEqualTo("EASY");
        // EASY: interval = round(1 * 2.5 * 1.3) = 3
        assertThat(resultResponse.getBody().getIntervalDays()).isEqualTo(3);

        // next_review_date 변경 확인
        ReviewItem updatedItem = reviewItemRepository.findById(reviewItemId).orElseThrow();
        assertThat(updatedItem.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(3));

        // review_log 기록 확인
        List<ReviewLog> logs = reviewLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getResult()).isEqualTo("EASY");
    }

    @Test
    @DisplayName("타입별 독립 선정 확인 - WORD 조회 시 PATTERN 제외")
    void getTodayCards_typeIndependent() {
        // given - 단어 등록
        restTemplate.postForEntity("/api/words",
                new WordCreateRequest("word1", "단어1"), WordResponse.class);

        // when - PATTERN 타입으로 조회
        ResponseEntity<ReviewCardResponse[]> patternCards = restTemplate.getForEntity(
                "/api/reviews/today?type=PATTERN", ReviewCardResponse[].class);

        // then - 패턴이 없으므로 빈 배열
        assertThat(patternCards.getBody()).isEmpty();

        // WORD로 조회하면 있음
        ResponseEntity<ReviewCardResponse[]> wordCards = restTemplate.getForEntity(
                "/api/reviews/today?type=WORD", ReviewCardResponse[].class);
        assertThat(wordCards.getBody()).isNotEmpty();
    }
}

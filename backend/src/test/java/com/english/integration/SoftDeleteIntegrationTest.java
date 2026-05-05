package com.english.integration;

import com.english.generate.*;
import com.english.pattern.PatternCreateRequest;
import com.english.pattern.PatternResponse;
import com.english.review.ReviewItem;
import com.english.review.ReviewItemRepository;
import com.english.word.WordCreateRequest;
import com.english.word.WordEnrichment;
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
class SoftDeleteIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReviewItemRepository reviewItemRepository;

    @Autowired
    private GeneratedSentenceRepository generatedSentenceRepository;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        authHeaders = getDefaultAuthHeaders();

        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/test/", "syn", "tip"));
    }

    @Test
    @DisplayName("단어 삭제 → WORD review_items deleted + 예문 유지 + SENTENCE review_items 유지")
    void deleteWord_reviewItemsDeletedButSentenceKept() {
        // given - 단어 등록
        ResponseEntity<WordResponse> wordRes = restTemplate.exchange(
                "/api/words", HttpMethod.POST,
                new HttpEntity<>(new WordCreateRequest("hello", "안녕"), authHeaders),
                WordResponse.class);
        Long wordId = wordRes.getBody().getId();

        // 패턴도 등록 (예문 생성에 필요)
        restTemplate.exchange("/api/patterns", HttpMethod.POST,
                new HttpEntity<>(new PatternCreateRequest("I want to ~", "~하고 싶다", List.of(
                        new PatternCreateRequest.ExampleRequest("I want to go", "가고 싶다")
                )), authHeaders), PatternResponse.class);

        // 예문 생성 (SENTENCE review_item 포함)
        GeminiGenerateResponse.GeminiSentence sentence = new GeminiGenerateResponse.GeminiSentence(
                "Hello world", "안녕 세계", List.of(wordId),
                List.of("s1", "s2", "s3", "s4", "s5")
        );
        GeminiGenerateResponse geminiResponse = new GeminiGenerateResponse(List.of(sentence));

        given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                .willReturn(geminiResponse);

        restTemplate.exchange("/api/generate", HttpMethod.POST,
                new HttpEntity<>(new GenerateRequest("ELEMENTARY", 1, null, null), authHeaders),
                GenerateResponse.class);

        // when - 단어 삭제
        restTemplate.exchange("/api/words/" + wordId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders), Void.class);

        // then - WORD review_items deleted=true
        List<ReviewItem> wordItems = reviewItemRepository.findByItemTypeAndItemId("WORD", wordId);
        assertThat(wordItems).allMatch(ReviewItem::isDeleted);

        // 예문은 유지됨
        List<GeneratedSentence> sentences = generatedSentenceRepository.findAll();
        assertThat(sentences).hasSize(1);

        // SENTENCE review_items는 유지됨
        List<ReviewItem> sentenceItems = reviewItemRepository.findByItemTypeAndItemId(
                "SENTENCE", sentences.get(0).getId());
        assertThat(sentenceItems).hasSize(1);
        assertThat(sentenceItems.get(0).isDeleted()).isFalse();
    }

    @Test
    @DisplayName("패턴 삭제 → PATTERN review_items deleted 확인")
    void deletePattern_reviewItemsDeleted() {
        // given
        ResponseEntity<PatternResponse> patternRes = restTemplate.exchange(
                "/api/patterns", HttpMethod.POST,
                new HttpEntity<>(new PatternCreateRequest("Let's ~", "~하자", List.of(
                        new PatternCreateRequest.ExampleRequest("Let's go", "가자")
                )), authHeaders), PatternResponse.class);
        Long patternId = patternRes.getBody().getId();

        // when
        restTemplate.exchange("/api/patterns/" + patternId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders), Void.class);

        // then
        List<ReviewItem> patternItems = reviewItemRepository.findByItemTypeAndItemId("PATTERN", patternId);
        assertThat(patternItems).allMatch(ReviewItem::isDeleted);
    }
}

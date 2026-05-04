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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GenerateIntegrationTest extends IntegrationTestBase {

    @Autowired
    private GeneratedSentenceRepository generatedSentenceRepository;

    @Autowired
    private GenerationHistoryRepository generationHistoryRepository;

    @Autowired
    private ReviewItemRepository reviewItemRepository;

    @BeforeEach
    void setUp() {
        // AI 보강 mock
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/test/", "syn", "tip"));
    }

    @Test
    @DisplayName("단어+패턴 등록 → 예문 생성 → situations 5개 + sentence_words 매핑 확인")
    void generate_fullFlow() {
        // given - 단어, 패턴 등록
        ResponseEntity<WordResponse> wordRes = restTemplate.postForEntity(
                "/api/words", new WordCreateRequest("coffee", "커피"), WordResponse.class);
        Long wordId = wordRes.getBody().getId();

        restTemplate.postForEntity("/api/patterns",
                new PatternCreateRequest("I want to ~", "~하고 싶다", List.of(
                        new PatternCreateRequest.ExampleRequest("I want to go", "나는 가고 싶다")
                )), PatternResponse.class);

        // Gemini 예문 생성 mock
        GeminiGenerateResponse.GeminiSentence sentence = new GeminiGenerateResponse.GeminiSentence(
                "I want to drink coffee.",
                "나는 커피를 마시고 싶다.",
                List.of(wordId),
                List.of("카페에서", "아침에 일어나서", "회의 전에", "점심 후에", "친구와 함께")
        );
        GeminiGenerateResponse geminiResponse = new GeminiGenerateResponse(List.of(sentence));

        given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                .willReturn(geminiResponse);

        // when
        GenerateRequest request = new GenerateRequest("ELEMENTARY", 1, null, null);
        ResponseEntity<GenerateResponse> response = restTemplate.postForEntity(
                "/api/generate", request, GenerateResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getSentences()).hasSize(1);

        // DB 검증: situations 5개
        List<GeneratedSentence> sentences = generatedSentenceRepository.findAll();
        assertThat(sentences).hasSize(1);
        assertThat(sentences.get(0).getSituations()).hasSize(5);

        // sentence_words 매핑 확인
        assertThat(sentences.get(0).getSentenceWords()).hasSize(1);
        assertThat(sentences.get(0).getSentenceWords().get(0).getWordId()).isEqualTo(wordId);

        // generation_history 기록 확인
        List<GenerationHistory> history = generationHistoryRepository.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getActualCount()).isEqualTo(1);

        // SENTENCE review_item 생성 확인
        List<ReviewItem> sentenceReviews = reviewItemRepository.findByItemTypeAndItemId(
                "SENTENCE", sentences.get(0).getId());
        assertThat(sentenceReviews).hasSize(1);
        assertThat(sentenceReviews.get(0).getDirection()).isEqualTo("RECOGNITION");
    }
}

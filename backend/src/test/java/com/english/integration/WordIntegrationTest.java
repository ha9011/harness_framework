package com.english.integration;

import com.english.review.ReviewItem;
import com.english.review.ReviewItemRepository;
import com.english.study.StudyRecordItemRepository;
import com.english.study.StudyRecordRepository;
import com.english.word.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WordIntegrationTest extends IntegrationTestBase {

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private ReviewItemRepository reviewItemRepository;

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    @Autowired
    private StudyRecordItemRepository studyRecordItemRepository;

    @Test
    @DisplayName("단어 등록 → DB 저장 + review_items 2개 + study_record 생성")
    void createWord_fullFlow() {
        // given
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/hɛˈloʊ/", "hi, hey", "인사할 때 사용"));

        WordCreateRequest request = new WordCreateRequest("hello", "안녕하세요");

        // when
        ResponseEntity<WordResponse> response = restTemplate.postForEntity(
                "/api/words", request, WordResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getWord()).isEqualTo("hello");
        assertThat(response.getBody().getPartOfSpeech()).isEqualTo("명사");

        // DB 저장 확인
        List<Word> words = wordRepository.findByDeletedFalse();
        assertThat(words).hasSize(1);

        // review_items 2개 생성 확인 (RECOGNITION + RECALL)
        List<ReviewItem> reviewItems = reviewItemRepository.findByItemTypeAndItemId("WORD", words.get(0).getId());
        assertThat(reviewItems).hasSize(2);
        assertThat(reviewItems).extracting(ReviewItem::getDirection)
                .containsExactlyInAnyOrder("RECOGNITION", "RECALL");

        // study_record 생성 확인
        assertThat(studyRecordRepository.findAll()).hasSize(1);
        assertThat(studyRecordItemRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("단어 벌크 등록 → saved/skipped 카운트 확인")
    void bulkCreate_savedAndSkipped() {
        // given
        given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                .willReturn(new WordEnrichment("명사", "/test/", "syn", "tip"));

        // 먼저 하나 등록
        restTemplate.postForEntity("/api/words",
                new WordCreateRequest("apple", "사과"), WordResponse.class);

        // when - 벌크로 중복 포함 등록
        List<WordCreateRequest> requests = List.of(
                new WordCreateRequest("apple", "사과"),   // 중복
                new WordCreateRequest("banana", "바나나"),
                new WordCreateRequest("cherry", "체리")
        );

        ResponseEntity<BulkCreateResponse> response = restTemplate.postForEntity(
                "/api/words/bulk", requests, BulkCreateResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getSaved()).isEqualTo(2);
        assertThat(response.getBody().getSkipped()).isEqualTo(1);
    }
}

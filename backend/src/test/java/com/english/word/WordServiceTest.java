package com.english.word;

import com.english.auth.User;
import com.english.config.DuplicateException;
import com.english.config.EmptyRequestException;
import com.english.config.GeminiClient;
import com.english.config.GeminiException;
import com.english.config.NotFoundException;
import com.english.review.ReviewItem;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemService;
import com.english.study.StudyRecord;
import com.english.study.StudyRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock
    private WordRepository wordRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private StudyRecordService studyRecordService;

    @Mock
    private ReviewItemService reviewItemService;

    @Mock
    private ReviewItemRepository reviewItemRepository;

    @InjectMocks
    private WordService wordService;

    private WordCreateRequest createRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("test@test.com", "password", "테스터");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        createRequest = new WordCreateRequest("apple", "사과");
    }

    @Nested
    @DisplayName("단건 등록")
    class CreateWord {

        @Test
        @DisplayName("성공 + AI 보강 호출 확인")
        void createSuccess() {
            // given
            given(wordRepository.existsByWordAndUserAndDeletedFalse("apple", testUser)).willReturn(false);

            WordEnrichment enrichment = new WordEnrichment("명사", "ˈæpəl", "fruit", "An apple a day keeps the doctor away");
            given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class))).willReturn(enrichment);

            Word savedWord = new Word(testUser, "apple", "사과");
            savedWord.enrich("명사", "ˈæpəl", "fruit", "An apple a day keeps the doctor away");
            given(wordRepository.save(any(Word.class))).willReturn(savedWord);

            StudyRecord record = new StudyRecord(testUser, 1, LocalDate.now());
            given(studyRecordService.getOrCreateTodayRecord(testUser)).willReturn(record);

            // when
            WordResponse response = wordService.create(testUser, createRequest);

            // then
            assertThat(response.getWord()).isEqualTo("apple");
            assertThat(response.getMeaning()).isEqualTo("사과");
            assertThat(response.getPartOfSpeech()).isEqualTo("명사");
            assertThat(response.getPronunciation()).isEqualTo("ˈæpəl");

            verify(geminiClient).generateContent(anyString(), eq(WordEnrichment.class));
            verify(studyRecordService).getOrCreateTodayRecord(testUser);
            verify(studyRecordService).addItem(eq(record), eq("WORD"), any());
            verify(reviewItemService).createWordReviewItems(eq(testUser), any());
        }

        @Test
        @DisplayName("AI 보강 실패 → 보강 없이 저장")
        void createWithEnrichmentFailure() {
            // given
            given(wordRepository.existsByWordAndUserAndDeletedFalse("apple", testUser)).willReturn(false);
            given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                    .willThrow(new GeminiException("API 오류"));

            Word savedWord = new Word(testUser, "apple", "사과");
            given(wordRepository.save(any(Word.class))).willReturn(savedWord);

            StudyRecord record = new StudyRecord(testUser, 1, LocalDate.now());
            given(studyRecordService.getOrCreateTodayRecord(testUser)).willReturn(record);

            // when
            WordResponse response = wordService.create(testUser, createRequest);

            // then
            assertThat(response.getWord()).isEqualTo("apple");
            assertThat(response.getPartOfSpeech()).isNull();
            verify(wordRepository).save(any(Word.class));
        }

        @Test
        @DisplayName("중복 단어 등록 → DuplicateException")
        void createDuplicate() {
            // given
            given(wordRepository.existsByWordAndUserAndDeletedFalse("apple", testUser)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> wordService.create(testUser, createRequest))
                    .isInstanceOf(DuplicateException.class);

            verify(wordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("벌크 등록")
    class BulkCreate {

        @Test
        @DisplayName("성공 — saved/skipped/enrichmentFailed 카운트")
        void bulkCreateSuccess() {
            // given
            List<WordCreateRequest> requests = List.of(
                    new WordCreateRequest("apple", "사과"),
                    new WordCreateRequest("banana", "바나나"),
                    new WordCreateRequest("cherry", "체리")
            );

            // apple은 이미 존재 (skipped)
            given(wordRepository.existsByWordAndUserAndDeletedFalse("apple", testUser)).willReturn(true);
            given(wordRepository.existsByWordAndUserAndDeletedFalse("banana", testUser)).willReturn(false);
            given(wordRepository.existsByWordAndUserAndDeletedFalse("cherry", testUser)).willReturn(false);

            // banana 보강 성공
            WordEnrichment enrichment = new WordEnrichment("명사", "bəˈnænə", "plantain", "노란 과일");
            given(geminiClient.generateContent(contains("banana"), eq(WordEnrichment.class))).willReturn(enrichment);
            // cherry 보강 실패
            given(geminiClient.generateContent(contains("cherry"), eq(WordEnrichment.class)))
                    .willThrow(new GeminiException("API 오류"));

            Word banana = new Word(testUser, "banana", "바나나");
            banana.enrich("명사", "bəˈnænə", "plantain", "노란 과일");
            Word cherry = new Word(testUser, "cherry", "체리");

            given(wordRepository.save(any(Word.class)))
                    .willAnswer(invocation -> {
                        Word w = invocation.getArgument(0);
                        if ("banana".equals(w.getWord())) return banana;
                        return cherry;
                    });

            StudyRecord record = new StudyRecord(testUser, 1, LocalDate.now());
            given(studyRecordService.getOrCreateTodayRecord(testUser)).willReturn(record);

            // when
            BulkCreateResponse response = wordService.bulkCreate(testUser, requests);

            // then
            assertThat(response.getSaved()).isEqualTo(2);
            assertThat(response.getSkipped()).isEqualTo(1);
            assertThat(response.getEnrichmentFailed()).isEqualTo(1);
            assertThat(response.getWords()).hasSize(2);
        }

        @Test
        @DisplayName("빈 배열 → EmptyRequestException")
        void bulkCreateEmpty() {
            assertThatThrownBy(() -> wordService.bulkCreate(testUser, List.of()))
                    .isInstanceOf(EmptyRequestException.class);
        }
    }

    @Nested
    @DisplayName("목록 조회")
    class GetList {

        @Test
        @DisplayName("페이지네이션 조회")
        void getListPaginated() {
            // given
            Word word = new Word(testUser, "apple", "사과");
            Page<Word> page = new PageImpl<>(List.of(word), PageRequest.of(0, 20), 1);
            given(wordRepository.findAllWithFilters(eq(testUser), isNull(), isNull(), eq(false), any(Pageable.class)))
                    .willReturn(page);

            // when
            Page<WordListResponse> result = wordService.getList(testUser, null, null, false, "latest", PageRequest.of(0, 20));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getWord()).isEqualTo("apple");
        }

        @Test
        @DisplayName("검색/필터 적용")
        void getListWithFilters() {
            // given
            Word word = new Word(testUser, "apple", "사과");
            Page<Word> page = new PageImpl<>(List.of(word));
            given(wordRepository.findAllWithFilters(eq(testUser), eq("app"), eq("명사"), eq(true), any(Pageable.class)))
                    .willReturn(page);

            // when
            Page<WordListResponse> result = wordService.getList(testUser, "app", "명사", true, "latest", PageRequest.of(0, 20));

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("상세 조회")
    class GetDetail {

        @Test
        @DisplayName("성공 + 예문 목록")
        void getDetailSuccess() {
            // given
            Word word = new Word(testUser, "apple", "사과");
            given(wordRepository.findByIdAndUserAndDeletedFalse(1L, testUser)).willReturn(Optional.of(word));

            // when
            WordDetailResponse response = wordService.getDetail(testUser, 1L);

            // then
            assertThat(response.getWord()).isEqualTo("apple");
            assertThat(response.getExamples()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID → NotFoundException")
        void getDetailNotFound() {
            // given
            given(wordRepository.findByIdAndUserAndDeletedFalse(999L, testUser)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> wordService.getDetail(testUser, 999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("중요 토글")
    class ToggleImportant {

        @Test
        @DisplayName("성공 — isImportant 반전")
        void toggleSuccess() {
            // given
            Word word = new Word(testUser, "apple", "사과");
            given(wordRepository.findByIdAndUserAndDeletedFalse(1L, testUser)).willReturn(Optional.of(word));

            // when
            WordResponse response = wordService.toggleImportant(testUser, 1L);

            // then
            assertThat(response.isImportant()).isTrue();
        }
    }

    @Nested
    @DisplayName("soft delete")
    class Delete {

        @Test
        @DisplayName("성공 → WORD review_items만 삭제")
        void deleteSuccess() {
            // given
            Word word = new Word(testUser, "apple", "사과");
            given(wordRepository.findByIdAndUserAndDeletedFalse(1L, testUser)).willReturn(Optional.of(word));

            // when
            wordService.delete(testUser, 1L);

            // then
            assertThat(word.isDeleted()).isTrue();
            verify(reviewItemRepository).softDeleteByUserAndItemTypeAndItemId(testUser, "WORD", 1L);
        }

        @Test
        @DisplayName("존재하지 않는 ID → NotFoundException")
        void deleteNotFound() {
            // given
            given(wordRepository.findByIdAndUserAndDeletedFalse(999L, testUser)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> wordService.delete(testUser, 999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("벌크 보강 프롬프트")
    class BulkEnrichmentPrompt {

        @Test
        @DisplayName("프롬프트에 모든 단어가 JSON 배열로 포함된다")
        void promptContainsAllWordsAsJsonArray() {
            // given
            List<WordCreateRequest> requests = List.of(
                    new WordCreateRequest("apple", "사과"),
                    new WordCreateRequest("banana", "바나나"),
                    new WordCreateRequest("cherry", "체리")
            );

            // when
            String prompt = wordService.buildBulkEnrichmentPrompt(requests);

            // then
            assertThat(prompt).contains("\"word\":\"apple\"");
            assertThat(prompt).contains("\"word\":\"banana\"");
            assertThat(prompt).contains("\"word\":\"cherry\"");
            assertThat(prompt).contains("\"meaning\":\"사과\"");
            assertThat(prompt).contains("\"meaning\":\"바나나\"");
            assertThat(prompt).contains("\"meaning\":\"체리\"");
            // JSON 배열 형태 확인
            assertThat(prompt).contains("[{");
            assertThat(prompt).contains("}]");
        }

        @Test
        @DisplayName("프롬프트에 응답 형식 지시가 포함된다")
        void promptContainsResponseFormatInstruction() {
            // given
            List<WordCreateRequest> requests = List.of(
                    new WordCreateRequest("apple", "사과")
            );

            // when
            String prompt = wordService.buildBulkEnrichmentPrompt(requests);

            // then
            assertThat(prompt).contains("enrichments");
            assertThat(prompt).contains("partOfSpeech");
            assertThat(prompt).contains("pronunciation");
            assertThat(prompt).contains("synonyms");
            assertThat(prompt).contains("tip");
            assertThat(prompt).contains("word");
        }
    }

    @Nested
    @DisplayName("등록 시 연동 확인")
    class IntegrationChecks {

        @Test
        @DisplayName("등록 시 study_records 자동 생성 확인")
        void studyRecordCreated() {
            // given
            given(wordRepository.existsByWordAndUserAndDeletedFalse("apple", testUser)).willReturn(false);
            given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                    .willThrow(new GeminiException("fail"));
            Word savedWord = new Word(testUser, "apple", "사과");
            given(wordRepository.save(any(Word.class))).willReturn(savedWord);
            StudyRecord record = new StudyRecord(testUser, 1, LocalDate.now());
            given(studyRecordService.getOrCreateTodayRecord(testUser)).willReturn(record);

            // when
            wordService.create(testUser, createRequest);

            // then
            verify(studyRecordService).getOrCreateTodayRecord(testUser);
        }

        @Test
        @DisplayName("등록 시 study_record_items에 (WORD, wordId) 추가 확인")
        void studyRecordItemAdded() {
            // given
            given(wordRepository.existsByWordAndUserAndDeletedFalse("apple", testUser)).willReturn(false);
            given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                    .willThrow(new GeminiException("fail"));
            Word savedWord = new Word(testUser, "apple", "사과");
            given(wordRepository.save(any(Word.class))).willReturn(savedWord);
            StudyRecord record = new StudyRecord(testUser, 1, LocalDate.now());
            given(studyRecordService.getOrCreateTodayRecord(testUser)).willReturn(record);

            // when
            wordService.create(testUser, createRequest);

            // then
            verify(studyRecordService).addItem(eq(record), eq("WORD"), any());
        }

        @Test
        @DisplayName("등록 시 review_items 2개(RECOGNITION+RECALL) 생성 확인")
        void reviewItemsCreated() {
            // given
            given(wordRepository.existsByWordAndUserAndDeletedFalse("apple", testUser)).willReturn(false);
            given(geminiClient.generateContent(anyString(), eq(WordEnrichment.class)))
                    .willThrow(new GeminiException("fail"));
            Word savedWord = new Word(testUser, "apple", "사과");
            given(wordRepository.save(any(Word.class))).willReturn(savedWord);
            StudyRecord record = new StudyRecord(testUser, 1, LocalDate.now());
            given(studyRecordService.getOrCreateTodayRecord(testUser)).willReturn(record);

            // when
            wordService.create(testUser, createRequest);

            // then
            verify(reviewItemService).createWordReviewItems(eq(testUser), any());
        }
    }
}

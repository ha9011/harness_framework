package com.english.generate;

import com.english.config.GeminiClient;
import com.english.config.GeminiException;
import com.english.config.NoPatternsException;
import com.english.config.NoWordsException;
import com.english.config.NotFoundException;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.review.ReviewItem;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemService;
import com.english.study.StudyRecord;
import com.english.study.StudyRecordService;
import com.english.word.Word;
import com.english.word.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateServiceTest {

    @Mock
    private WordRepository wordRepository;

    @Mock
    private PatternRepository patternRepository;

    @Mock
    private GeneratedSentenceRepository generatedSentenceRepository;

    @Mock
    private GenerationHistoryRepository generationHistoryRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private ReviewItemService reviewItemService;

    @Mock
    private StudyRecordService studyRecordService;

    @Mock
    private ReviewItemRepository reviewItemRepository;

    @InjectMocks
    private GenerateService generateService;

    private List<Word> sampleWords;
    private List<Pattern> samplePatterns;
    private GeminiGenerateResponse sampleGeminiResponse;

    @BeforeEach
    void setUp() {
        sampleWords = List.of(
                createWord(1L, "apple", "사과", true),
                createWord(2L, "banana", "바나나", false),
                createWord(3L, "cherry", "체리", false)
        );

        samplePatterns = List.of(
                createPattern(1L, "I want to ~", "~하고 싶다"),
                createPattern(2L, "I used to ~", "~하곤 했다")
        );

        sampleGeminiResponse = new GeminiGenerateResponse(List.of(
                new GeminiGenerateResponse.GeminiSentence(
                        "I want to eat an apple.",
                        "나는 사과를 먹고 싶다.",
                        List.of(1L),
                        List.of("카페에서 메뉴를 고르며", "아침에 과일을 먹으며", "마트에서 장보며", "친구와 대화하며", "요리를 준비하며")
                ),
                new GeminiGenerateResponse.GeminiSentence(
                        "I used to eat bananas every day.",
                        "나는 매일 바나나를 먹곤 했다.",
                        List.of(2L),
                        List.of("건강 습관을 이야기하며", "어릴 때를 회상하며", "식단을 바꾸며", "친구에게 추천하며", "다이어트 경험을 나누며")
                )
        ));
    }

    private Word createWord(Long id, String word, String meaning, boolean important) {
        Word w = new Word(word, meaning);
        // Reflection으로 id 설정
        try {
            var idField = Word.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(w, id);
            if (important) {
                w.toggleImportant();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return w;
    }

    private Pattern createPattern(Long id, String template, String description) {
        Pattern p = new Pattern(template, description);
        try {
            var idField = Pattern.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return p;
    }

    @Nested
    @DisplayName("일반 생성")
    class Generate {

        @Test
        @DisplayName("성공: level + count → Gemini 호출 → 예문 저장 + situation 5개 + word 매핑")
        void generateSuccess() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, null, null);

            given(wordRepository.findByDeletedFalse()).willReturn(sampleWords);
            given(patternRepository.findByDeletedFalse(any())).willReturn(new PageImpl<>(samplePatterns));
            given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                    .willReturn(sampleGeminiResponse);
            given(generatedSentenceRepository.save(any(GeneratedSentence.class)))
                    .willAnswer(invocation -> {
                        GeneratedSentence s = invocation.getArgument(0);
                        try {
                            var idField = GeneratedSentence.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(s, 1L);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return s;
                    });
            given(generationHistoryRepository.save(any(GenerationHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            // word 존재 확인
            given(wordRepository.existsById(1L)).willReturn(true);
            given(wordRepository.existsById(2L)).willReturn(true);

            // when
            GenerateResponse response = generateService.generate(request);

            // then
            assertThat(response.getSentences()).hasSize(2);
            assertThat(response.getSentences().get(0).getEnglishSentence()).isEqualTo("I want to eat an apple.");
            assertThat(response.getSentences().get(0).getSituations()).hasSize(5);

            verify(generatedSentenceRepository, times(2)).save(any(GeneratedSentence.class));
            verify(reviewItemService, times(2)).createSentenceReviewItem(any());
            verify(generationHistoryRepository).save(any(GenerationHistory.class));
        }

        @Test
        @DisplayName("단어 0개 → NoWordsException")
        void generateNoWords() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, null, null);
            given(wordRepository.findByDeletedFalse()).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> generateService.generate(request))
                    .isInstanceOf(NoWordsException.class);
        }

        @Test
        @DisplayName("패턴 0개 → NoPatternsException")
        void generateNoPatterns() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, null, null);
            given(wordRepository.findByDeletedFalse()).willReturn(sampleWords);
            given(patternRepository.findByDeletedFalse(any())).willReturn(Page.empty());

            // when & then
            assertThatThrownBy(() -> generateService.generate(request))
                    .isInstanceOf(NoPatternsException.class);
        }
    }

    @Nested
    @DisplayName("단어 지정 생성")
    class GenerateByWord {

        @Test
        @DisplayName("성공: 지정 단어 포함 예문 생성")
        void generateByWordSuccess() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, 1L, null);

            given(wordRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleWords.get(0)));
            given(wordRepository.findByDeletedFalse()).willReturn(sampleWords);
            given(patternRepository.findByDeletedFalse(any())).willReturn(new PageImpl<>(samplePatterns));
            given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                    .willReturn(sampleGeminiResponse);
            given(generatedSentenceRepository.save(any(GeneratedSentence.class)))
                    .willAnswer(invocation -> {
                        GeneratedSentence s = invocation.getArgument(0);
                        try {
                            var idField = GeneratedSentence.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(s, 1L);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return s;
                    });
            given(generationHistoryRepository.save(any(GenerationHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(wordRepository.existsById(1L)).willReturn(true);
            given(wordRepository.existsById(2L)).willReturn(true);

            // when
            GenerateResponse response = generateService.generateByWord(1L, request);

            // then
            assertThat(response.getSentences()).hasSize(2);
            // generation_history에 wordId 기록 확인
            ArgumentCaptor<GenerationHistory> historyCaptor = ArgumentCaptor.forClass(GenerationHistory.class);
            verify(generationHistoryRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getWordId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 wordId → NotFoundException")
        void generateByWordNotFound() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, 1L, null);
            given(wordRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> generateService.generateByWord(999L, request))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("패턴 지정 생성")
    class GenerateByPattern {

        @Test
        @DisplayName("성공: 지정 패턴으로 예문 생성")
        void generateByPatternSuccess() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, null, 1L);

            given(patternRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(samplePatterns.get(0)));
            given(wordRepository.findByDeletedFalse()).willReturn(sampleWords);
            given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                    .willReturn(sampleGeminiResponse);
            given(generatedSentenceRepository.save(any(GeneratedSentence.class)))
                    .willAnswer(invocation -> {
                        GeneratedSentence s = invocation.getArgument(0);
                        try {
                            var idField = GeneratedSentence.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(s, 1L);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return s;
                    });
            given(generationHistoryRepository.save(any(GenerationHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(wordRepository.existsById(1L)).willReturn(true);
            given(wordRepository.existsById(2L)).willReturn(true);

            // when
            GenerateResponse response = generateService.generateByPattern(1L, request);

            // then
            assertThat(response.getSentences()).hasSize(2);
            ArgumentCaptor<GenerationHistory> historyCaptor = ArgumentCaptor.forClass(GenerationHistory.class);
            verify(generationHistoryRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getPatternId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 patternId → NotFoundException")
        void generateByPatternNotFound() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, null, 1L);
            given(patternRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> generateService.generateByPattern(999L, request))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("단어 선택 우선순위")
    class WordSelection {

        @Test
        @DisplayName("⭐중요 > 복습 적은 것 순서, 최대 50개")
        void wordSelectionPriority() {
            // given
            // 60개 단어 생성 (최대 50개만 선택되는지 확인)
            List<Word> manyWords = new ArrayList<>();
            for (int i = 1; i <= 60; i++) {
                manyWords.add(createWord((long) i, "word" + i, "뜻" + i, i <= 5)); // 5개는 중요
            }

            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, null, null);
            given(wordRepository.findByDeletedFalse()).willReturn(manyWords);
            given(patternRepository.findByDeletedFalse(any())).willReturn(new PageImpl<>(samplePatterns));

            GeminiGenerateResponse simpleResponse = new GeminiGenerateResponse(List.of(
                    new GeminiGenerateResponse.GeminiSentence(
                            "Test sentence.", "테스트 문장.",
                            List.of(1L), List.of("상황1", "상황2", "상황3", "상황4", "상황5")
                    )
            ));
            given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                    .willReturn(simpleResponse);
            given(generatedSentenceRepository.save(any(GeneratedSentence.class)))
                    .willAnswer(invocation -> {
                        GeneratedSentence s = invocation.getArgument(0);
                        try {
                            var idField = GeneratedSentence.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(s, 1L);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return s;
                    });
            given(generationHistoryRepository.save(any(GenerationHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(wordRepository.existsById(1L)).willReturn(true);

            // when
            generateService.generate(request);

            // then — Gemini 프롬프트에 최대 50개 단어만 전달되었는지 확인
            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            verify(geminiClient).generateContent(promptCaptor.capture(), eq(GeminiGenerateResponse.class));
            // 프롬프트에 "word51"~"word60"은 포함되지 않아야 함 (최대 50개)
            String prompt = promptCaptor.getValue();
            // 중요 단어(word1~word5)는 반드시 포함
            assertThat(prompt).contains("word1");
            assertThat(prompt).contains("word5");
        }
    }

    @Nested
    @DisplayName("Gemini 응답 처리")
    class GeminiResponseHandling {

        @Test
        @DisplayName("잘못된 wordId → 예문 저장, 매핑만 무시")
        void invalidWordIdIgnored() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, null, null);

            GeminiGenerateResponse responseWithBadId = new GeminiGenerateResponse(List.of(
                    new GeminiGenerateResponse.GeminiSentence(
                            "Test sentence.", "테스트 문장.",
                            List.of(1L, 999L), // 999L은 존재하지 않는 ID
                            List.of("상황1", "상황2", "상황3", "상황4", "상황5")
                    )
            ));

            given(wordRepository.findByDeletedFalse()).willReturn(sampleWords);
            given(patternRepository.findByDeletedFalse(any())).willReturn(new PageImpl<>(samplePatterns));
            given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                    .willReturn(responseWithBadId);
            given(generatedSentenceRepository.save(any(GeneratedSentence.class)))
                    .willAnswer(invocation -> {
                        GeneratedSentence s = invocation.getArgument(0);
                        try {
                            var idField = GeneratedSentence.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(s, 1L);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return s;
                    });
            given(generationHistoryRepository.save(any(GenerationHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(wordRepository.existsById(1L)).willReturn(true);
            given(wordRepository.existsById(999L)).willReturn(false);

            // when
            GenerateResponse response = generateService.generate(request);

            // then — 예문 자체는 저장됨
            assertThat(response.getSentences()).hasSize(1);
            verify(generatedSentenceRepository).save(any(GeneratedSentence.class));
        }

        @Test
        @DisplayName("요청 30개인데 25개만 생성 → actualCount=25")
        void partialGeneration() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 30, null, null);

            // Gemini가 2개만 반환 (30개 요청했지만)
            given(wordRepository.findByDeletedFalse()).willReturn(sampleWords);
            given(patternRepository.findByDeletedFalse(any())).willReturn(new PageImpl<>(samplePatterns));
            given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                    .willReturn(sampleGeminiResponse); // 2개만 반환
            given(generatedSentenceRepository.save(any(GeneratedSentence.class)))
                    .willAnswer(invocation -> {
                        GeneratedSentence s = invocation.getArgument(0);
                        try {
                            var idField = GeneratedSentence.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(s, 1L);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return s;
                    });
            given(generationHistoryRepository.save(any(GenerationHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(wordRepository.existsById(1L)).willReturn(true);
            given(wordRepository.existsById(2L)).willReturn(true);

            // when
            generateService.generate(request);

            // then
            ArgumentCaptor<GenerationHistory> historyCaptor = ArgumentCaptor.forClass(GenerationHistory.class);
            verify(generationHistoryRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getRequestedCount()).isEqualTo(30);
            assertThat(historyCaptor.getValue().getActualCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("review_items 자동 생성")
    class ReviewItemCreation {

        @Test
        @DisplayName("예문마다 SENTENCE RECOGNITION review_item 생성")
        void sentenceReviewItemCreated() {
            // given
            GenerateRequest request = new GenerateRequest("ELEMENTARY", 10, null, null);

            given(wordRepository.findByDeletedFalse()).willReturn(sampleWords);
            given(patternRepository.findByDeletedFalse(any())).willReturn(new PageImpl<>(samplePatterns));
            given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                    .willReturn(sampleGeminiResponse);
            given(generatedSentenceRepository.save(any(GeneratedSentence.class)))
                    .willAnswer(invocation -> {
                        GeneratedSentence s = invocation.getArgument(0);
                        try {
                            var idField = GeneratedSentence.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(s, 1L);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return s;
                    });
            given(generationHistoryRepository.save(any(GenerationHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(wordRepository.existsById(1L)).willReturn(true);
            given(wordRepository.existsById(2L)).willReturn(true);

            // when
            generateService.generate(request);

            // then — 예문 2개 → review_item 2개
            verify(reviewItemService, times(2)).createSentenceReviewItem(any());
        }
    }

    @Nested
    @DisplayName("generation_history 기록")
    class HistoryRecording {

        @Test
        @DisplayName("일반 생성 → wordId/patternId null")
        void historyNormalGeneration() {
            // given
            GenerateRequest request = new GenerateRequest("INTERMEDIATE", 10, null, null);

            given(wordRepository.findByDeletedFalse()).willReturn(sampleWords);
            given(patternRepository.findByDeletedFalse(any())).willReturn(new PageImpl<>(samplePatterns));
            given(geminiClient.generateContent(anyString(), eq(GeminiGenerateResponse.class)))
                    .willReturn(sampleGeminiResponse);
            given(generatedSentenceRepository.save(any(GeneratedSentence.class)))
                    .willAnswer(invocation -> {
                        GeneratedSentence s = invocation.getArgument(0);
                        try {
                            var idField = GeneratedSentence.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(s, 1L);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return s;
                    });
            given(generationHistoryRepository.save(any(GenerationHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(wordRepository.existsById(anyLong())).willReturn(true);

            // when
            generateService.generate(request);

            // then
            ArgumentCaptor<GenerationHistory> captor = ArgumentCaptor.forClass(GenerationHistory.class);
            verify(generationHistoryRepository).save(captor.capture());
            GenerationHistory history = captor.getValue();
            assertThat(history.getLevel()).isEqualTo("INTERMEDIATE");
            assertThat(history.getWordId()).isNull();
            assertThat(history.getPatternId()).isNull();
        }

        @Test
        @DisplayName("이력 조회 → 페이지네이션")
        void getHistory() {
            // given
            GenerationHistory history = new GenerationHistory("ELEMENTARY", 10, 10, null, null);
            Page<GenerationHistory> page = new PageImpl<>(List.of(history), PageRequest.of(0, 20), 1);
            given(generationHistoryRepository.findAllByOrderByCreatedAtDesc(any())).willReturn(page);

            // when
            Page<GenerationHistoryResponse> result = generateService.getHistory(PageRequest.of(0, 20));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getLevel()).isEqualTo("ELEMENTARY");
        }
    }
}

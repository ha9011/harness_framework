package com.english.pattern;

import com.english.config.*;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemService;
import com.english.study.StudyRecord;
import com.english.study.StudyRecordService;
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
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatternServiceTest {

    @Mock
    private PatternRepository patternRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private StudyRecordService studyRecordService;

    @Mock
    private ReviewItemService reviewItemService;

    @Mock
    private ReviewItemRepository reviewItemRepository;

    @InjectMocks
    private PatternService patternService;

    @Nested
    @DisplayName("패턴 등록")
    class CreatePattern {

        @Test
        @DisplayName("성공 + 교재 예문 순서 보존")
        void createSuccess() {
            // given
            PatternCreateRequest request = new PatternCreateRequest(
                    "I want to ~",
                    "~하고 싶다",
                    List.of(
                            new PatternCreateRequest.ExampleRequest("I want to go home.", "집에 가고 싶다."),
                            new PatternCreateRequest.ExampleRequest("I want to eat pizza.", "피자를 먹고 싶다.")
                    )
            );

            given(patternRepository.existsByTemplateAndDeletedFalse("I want to ~")).willReturn(false);

            Pattern savedPattern = new Pattern("I want to ~", "~하고 싶다");
            savedPattern.addExample("I want to go home.", "집에 가고 싶다.", 0);
            savedPattern.addExample("I want to eat pizza.", "피자를 먹고 싶다.", 1);
            given(patternRepository.save(any(Pattern.class))).willReturn(savedPattern);

            StudyRecord record = new StudyRecord(1, LocalDate.now());
            given(studyRecordService.getOrCreateTodayRecord()).willReturn(record);

            // when
            PatternResponse response = patternService.create(request);

            // then
            assertThat(response.getTemplate()).isEqualTo("I want to ~");
            assertThat(response.getDescription()).isEqualTo("~하고 싶다");

            verify(patternRepository).save(any(Pattern.class));
            verify(studyRecordService).getOrCreateTodayRecord();
            verify(studyRecordService).addItem(eq(record), eq("PATTERN"), any());
            verify(reviewItemService).createPatternReviewItems(any());
        }

        @Test
        @DisplayName("중복 패턴 → DuplicateException")
        void createDuplicate() {
            // given
            PatternCreateRequest request = new PatternCreateRequest("I want to ~", "~하고 싶다", List.of());
            given(patternRepository.existsByTemplateAndDeletedFalse("I want to ~")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> patternService.create(request))
                    .isInstanceOf(DuplicateException.class);

            verify(patternRepository, never()).save(any());
        }

        @Test
        @DisplayName("등록 시 study_records 자동 생성")
        void studyRecordCreated() {
            // given
            PatternCreateRequest request = new PatternCreateRequest("I want to ~", "~하고 싶다", List.of());
            given(patternRepository.existsByTemplateAndDeletedFalse("I want to ~")).willReturn(false);

            Pattern savedPattern = new Pattern("I want to ~", "~하고 싶다");
            given(patternRepository.save(any(Pattern.class))).willReturn(savedPattern);

            StudyRecord record = new StudyRecord(1, LocalDate.now());
            given(studyRecordService.getOrCreateTodayRecord()).willReturn(record);

            // when
            patternService.create(request);

            // then
            verify(studyRecordService).getOrCreateTodayRecord();
            verify(studyRecordService).addItem(eq(record), eq("PATTERN"), any());
        }

        @Test
        @DisplayName("등록 시 review_items 2개(RECOGNITION+RECALL) 생성")
        void reviewItemsCreated() {
            // given
            PatternCreateRequest request = new PatternCreateRequest("I want to ~", "~하고 싶다", List.of());
            given(patternRepository.existsByTemplateAndDeletedFalse("I want to ~")).willReturn(false);

            Pattern savedPattern = new Pattern("I want to ~", "~하고 싶다");
            given(patternRepository.save(any(Pattern.class))).willReturn(savedPattern);

            StudyRecord record = new StudyRecord(1, LocalDate.now());
            given(studyRecordService.getOrCreateTodayRecord()).willReturn(record);

            // when
            patternService.create(request);

            // then
            verify(reviewItemService).createPatternReviewItems(any());
        }
    }

    @Nested
    @DisplayName("목록 조회")
    class GetList {

        @Test
        @DisplayName("페이지네이션 조회")
        void getListPaginated() {
            // given
            Pattern pattern = new Pattern("I want to ~", "~하고 싶다");
            Page<Pattern> page = new PageImpl<>(List.of(pattern), PageRequest.of(0, 20), 1);
            given(patternRepository.findByDeletedFalse(any())).willReturn(page);

            // when
            Page<PatternListResponse> result = patternService.getList(PageRequest.of(0, 20));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTemplate()).isEqualTo("I want to ~");
        }
    }

    @Nested
    @DisplayName("상세 조회")
    class GetDetail {

        @Test
        @DisplayName("성공")
        void getDetailSuccess() {
            // given
            Pattern pattern = new Pattern("I want to ~", "~하고 싶다");
            pattern.addExample("I want to go.", "가고 싶다.", 0);
            given(patternRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(pattern));

            // when
            PatternDetailResponse response = patternService.getDetail(1L);

            // then
            assertThat(response.getTemplate()).isEqualTo("I want to ~");
            assertThat(response.getExamples()).hasSize(1);
            assertThat(response.getExamples().get(0).getSentence()).isEqualTo("I want to go.");
        }

        @Test
        @DisplayName("존재하지 않는 ID → NotFoundException")
        void getDetailNotFound() {
            // given
            given(patternRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> patternService.getDetail(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("soft delete")
    class Delete {

        @Test
        @DisplayName("성공 → PATTERN review_items만 삭제")
        void deleteSuccess() {
            // given
            Pattern pattern = new Pattern("I want to ~", "~하고 싶다");
            given(patternRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(pattern));

            // when
            patternService.delete(1L);

            // then
            assertThat(pattern.isDeleted()).isTrue();
            verify(reviewItemRepository).softDeleteByItemTypeAndItemId("PATTERN", 1L);
        }

        @Test
        @DisplayName("존재하지 않는 ID → NotFoundException")
        void deleteNotFound() {
            // given
            given(patternRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> patternService.delete(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("이미지 추출")
    class Extract {

        @Test
        @DisplayName("패턴 이미지 추출 성공")
        void extractPatternSuccess() {
            // given
            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.png", "image/png", new byte[]{1, 2, 3});

            PatternExtractResponse extractResult = new PatternExtractResponse(
                    "I want to ~", "~하고 싶다",
                    List.of(new PatternExtractResponse.ExtractedExample("I want to go.", "가고 싶다."))
            );
            given(geminiClient.generateContentWithImage(any(), eq("image/png"), anyString(), eq(PatternExtractResponse.class)))
                    .willReturn(extractResult);

            // when
            PatternExtractResponse response = patternService.extractFromImage(image);

            // then
            assertThat(response.getTemplate()).isEqualTo("I want to ~");
            assertThat(response.getExamples()).hasSize(1);
        }

        @Test
        @DisplayName("단어 이미지 추출 성공")
        void extractWordsSuccess() {
            // given
            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

            WordExtractResponse extractResult = new WordExtractResponse(
                    List.of(new WordExtractResponse.ExtractedWord("apple", "사과"))
            );
            given(geminiClient.generateContentWithImage(any(), eq("image/jpeg"), anyString(), eq(WordExtractResponse.class)))
                    .willReturn(extractResult);

            // when
            WordExtractResponse response = patternService.extractWordsFromImage(image);

            // then
            assertThat(response.getWords()).hasSize(1);
            assertThat(response.getWords().get(0).getWord()).isEqualTo("apple");
        }

        @Test
        @DisplayName("이미지 형식 오류 → InvalidImageException")
        void extractInvalidFormat() {
            // given
            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.txt", "text/plain", new byte[]{1, 2, 3});

            // when & then
            assertThatThrownBy(() -> patternService.extractFromImage(image))
                    .isInstanceOf(InvalidImageException.class);
        }

        @Test
        @DisplayName("Gemini 장애 → GeminiException")
        void extractGeminiFailure() {
            // given
            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.png", "image/png", new byte[]{1, 2, 3});

            given(geminiClient.generateContentWithImage(any(), eq("image/png"), anyString(), eq(PatternExtractResponse.class)))
                    .willThrow(new GeminiException("API 오류"));

            // when & then
            assertThatThrownBy(() -> patternService.extractFromImage(image))
                    .isInstanceOf(GeminiException.class);
        }
    }
}

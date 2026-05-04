package com.english.review;

import com.english.config.NotFoundException;
import com.english.generate.GeneratedSentence;
import com.english.generate.GeneratedSentenceRepository;
import com.english.generate.GeneratedSentenceWord;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.setting.SettingService;
import com.english.setting.UserSettingResponse;
import com.english.word.Word;
import com.english.word.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewItemRepository reviewItemRepository;

    @Mock
    private ReviewLogRepository reviewLogRepository;

    @Mock
    private WordRepository wordRepository;

    @Mock
    private PatternRepository patternRepository;

    @Mock
    private GeneratedSentenceRepository generatedSentenceRepository;

    @Mock
    private SettingService settingService;

    @InjectMocks
    private ReviewService reviewService;

    @Nested
    @DisplayName("카드 선정 - getTodayCards")
    class GetTodayCards {

        @BeforeEach
        void setUp() {
            given(settingService.getSetting()).willReturn(new UserSettingResponse(10));
        }

        @Test
        @DisplayName("type=WORD → WORD 타입만 반환")
        void wordTypeOnly() {
            // given
            ReviewItem wordItem = createReviewItem(1L, "WORD", 1L, "RECOGNITION");
            given(reviewItemRepository.findTodayCards(eq("WORD"), any(LocalDate.class), anyList()))
                    .willReturn(List.of(wordItem));

            Word word = new Word("hello", "안녕");
            given(wordRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(word));
            given(generatedSentenceRepository.findByWordId(1L)).willReturn(Collections.emptyList());

            // when
            List<ReviewCardResponse> result = reviewService.getTodayCards("WORD", Collections.emptyList());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getItemType()).isEqualTo("WORD");
        }

        @Test
        @DisplayName("type=PATTERN → PATTERN 타입만 반환")
        void patternTypeOnly() {
            // given
            ReviewItem patternItem = createReviewItem(2L, "PATTERN", 1L, "RECOGNITION");
            given(reviewItemRepository.findTodayCards(eq("PATTERN"), any(LocalDate.class), anyList()))
                    .willReturn(List.of(patternItem));

            Pattern pattern = new Pattern("I want to ~", "~하고 싶다");
            setId(pattern, 1L);
            given(patternRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(pattern));

            // when
            List<ReviewCardResponse> result = reviewService.getTodayCards("PATTERN", Collections.emptyList());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getItemType()).isEqualTo("PATTERN");
        }

        @Test
        @DisplayName("type=SENTENCE → SENTENCE 타입만 반환")
        void sentenceTypeOnly() {
            // given
            ReviewItem sentenceItem = createReviewItem(3L, "SENTENCE", 1L, "RECOGNITION");
            given(reviewItemRepository.findTodayCards(eq("SENTENCE"), any(LocalDate.class), anyList()))
                    .willReturn(List.of(sentenceItem));

            GeneratedSentence sentence = new GeneratedSentence("Hello world", "안녕 세상", "ELEMENTARY");
            sentence.addSituation("카페에서 친구에게 인사할 때");
            setId(sentence, 1L);
            given(generatedSentenceRepository.findById(1L)).willReturn(Optional.of(sentence));

            // when
            List<ReviewCardResponse> result = reviewService.getTodayCards("SENTENCE", Collections.emptyList());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getItemType()).isEqualTo("SENTENCE");
        }

        @Test
        @DisplayName("복습 대상 0개 → 빈 배열 반환")
        void emptyResult() {
            // given
            given(reviewItemRepository.findTodayCards(eq("WORD"), any(LocalDate.class), anyList()))
                    .willReturn(Collections.emptyList());

            // when
            List<ReviewCardResponse> result = reviewService.getTodayCards("WORD", Collections.emptyList());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("exclude 파라미터로 이미 한 카드 제외")
        void excludeCards() {
            // given
            List<Long> exclude = List.of(1L, 2L);
            given(reviewItemRepository.findTodayCards(eq("WORD"), any(LocalDate.class), eq(exclude)))
                    .willReturn(Collections.emptyList());

            // when
            List<ReviewCardResponse> result = reviewService.getTodayCards("WORD", exclude);

            // then
            assertThat(result).isEmpty();
            verify(reviewItemRepository).findTodayCards("WORD", LocalDate.now(), exclude);
        }

        @Test
        @DisplayName("SENTENCE 카드 front에 situation 포함")
        void sentenceCardHasSituation() {
            // given
            ReviewItem sentenceItem = createReviewItem(3L, "SENTENCE", 1L, "RECOGNITION");
            given(reviewItemRepository.findTodayCards(eq("SENTENCE"), any(LocalDate.class), anyList()))
                    .willReturn(List.of(sentenceItem));

            GeneratedSentence sentence = new GeneratedSentence("I want coffee", "커피 주세요", "ELEMENTARY");
            sentence.addSituation("카페에서 주문할 때");
            sentence.addSituation("아침에 눈 뜨자마자");
            sentence.addSituation("회의 중 졸릴 때");
            setId(sentence, 1L);
            given(generatedSentenceRepository.findById(1L)).willReturn(Optional.of(sentence));

            // when
            List<ReviewCardResponse> result = reviewService.getTodayCards("SENTENCE", Collections.emptyList());

            // then
            assertThat(result.get(0).getFront().getSituation()).isNotNull();
            assertThat(result.get(0).getFront().getEnglishSentence()).isEqualTo("I want coffee");
            assertThat(result.get(0).getBack().getKoreanTranslation()).isEqualTo("커피 주세요");
        }

        @Test
        @DisplayName("WORD RECOGNITION 카드 back에 examples 최대 3개")
        void wordRecognitionBackExamples() {
            // given
            ReviewItem wordItem = createReviewItem(1L, "WORD", 1L, "RECOGNITION");
            given(reviewItemRepository.findTodayCards(eq("WORD"), any(LocalDate.class), anyList()))
                    .willReturn(List.of(wordItem));

            Word word = new Word("hello", "안녕");
            given(wordRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(word));

            // 4개 예문 생성 → 최대 3개 반환
            GeneratedSentence s1 = new GeneratedSentence("Hello there", "안녕 거기", "ELEMENTARY");
            GeneratedSentence s2 = new GeneratedSentence("Hello world", "안녕 세상", "ELEMENTARY");
            GeneratedSentence s3 = new GeneratedSentence("Hello friend", "안녕 친구", "ELEMENTARY");
            GeneratedSentence s4 = new GeneratedSentence("Hello again", "또 안녕", "ELEMENTARY");
            given(generatedSentenceRepository.findByWordId(1L)).willReturn(List.of(s1, s2, s3, s4));

            // when
            List<ReviewCardResponse> result = reviewService.getTodayCards("WORD", Collections.emptyList());

            // then
            assertThat(result.get(0).getBack().getExamples()).hasSize(3);
        }

        @Test
        @DisplayName("WORD RECOGNITION 카드 - 예문 3개 이하면 전부 반환")
        void wordRecognitionBackExamplesLessThan3() {
            // given
            ReviewItem wordItem = createReviewItem(1L, "WORD", 1L, "RECOGNITION");
            given(reviewItemRepository.findTodayCards(eq("WORD"), any(LocalDate.class), anyList()))
                    .willReturn(List.of(wordItem));

            Word word = new Word("hello", "안녕");
            given(wordRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(word));

            GeneratedSentence s1 = new GeneratedSentence("Hello there", "안녕 거기", "ELEMENTARY");
            GeneratedSentence s2 = new GeneratedSentence("Hello world", "안녕 세상", "ELEMENTARY");
            given(generatedSentenceRepository.findByWordId(1L)).willReturn(List.of(s1, s2));

            // when
            List<ReviewCardResponse> result = reviewService.getTodayCards("WORD", Collections.emptyList());

            // then
            assertThat(result.get(0).getBack().getExamples()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("SM-2 적용 - submitResult")
    class SubmitResult {

        @Test
        @DisplayName("EASY: interval × ease_factor × 1.3, ease_factor += 0.15")
        void easyResult() {
            // given
            ReviewItem item = createReviewItem(1L, "WORD", 1L, "RECOGNITION");
            // 기본값: intervalDays=1, easeFactor=2.5
            given(reviewItemRepository.findById(1L)).willReturn(Optional.of(item));
            given(reviewLogRepository.save(any(ReviewLog.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            ReviewResultResponse result = reviewService.submitResult(1L, "EASY");

            // then
            // new_interval = round(1 * 2.5 * 1.3) = round(3.25) = 3
            assertThat(result.getIntervalDays()).isEqualTo(3);
            // ease_factor = 2.5 + 0.15 = 2.65
            assertThat(result.getEaseFactor()).isEqualTo(2.65);
            assertThat(result.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(3));
            assertThat(result.getReviewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("MEDIUM: interval × ease_factor, ease_factor 유지")
        void mediumResult() {
            // given
            ReviewItem item = createReviewItem(1L, "WORD", 1L, "RECOGNITION");
            given(reviewItemRepository.findById(1L)).willReturn(Optional.of(item));
            given(reviewLogRepository.save(any(ReviewLog.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            ReviewResultResponse result = reviewService.submitResult(1L, "MEDIUM");

            // then
            // new_interval = round(1 * 2.5) = 3 (반올림)
            assertThat(result.getIntervalDays()).isEqualTo(3);
            // ease_factor 유지 = 2.5
            assertThat(result.getEaseFactor()).isEqualTo(2.5);
            assertThat(result.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(3));
            assertThat(result.getReviewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("HARD: interval = 1, ease_factor = max(1.3, ef - 0.2)")
        void hardResult() {
            // given
            ReviewItem item = createReviewItem(1L, "WORD", 1L, "RECOGNITION");
            given(reviewItemRepository.findById(1L)).willReturn(Optional.of(item));
            given(reviewLogRepository.save(any(ReviewLog.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            ReviewResultResponse result = reviewService.submitResult(1L, "HARD");

            // then
            assertThat(result.getIntervalDays()).isEqualTo(1);
            // ease_factor = max(1.3, 2.5 - 0.2) = 2.3
            assertThat(result.getEaseFactor()).isEqualTo(2.3);
            assertThat(result.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
            assertThat(result.getReviewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("review_count 증가 + review_log 기록")
        void reviewCountAndLog() {
            // given
            ReviewItem item = createReviewItem(1L, "WORD", 1L, "RECOGNITION");
            given(reviewItemRepository.findById(1L)).willReturn(Optional.of(item));
            given(reviewLogRepository.save(any(ReviewLog.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            reviewService.submitResult(1L, "EASY");

            // then
            verify(reviewLogRepository).save(any(ReviewLog.class));
            assertThat(item.getReviewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("존재하지 않는 reviewItem → NotFoundException")
        void notFound() {
            // given
            given(reviewItemRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.submitResult(999L, "EASY"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // === 헬퍼 메서드 ===

    private ReviewItem createReviewItem(Long id, String itemType, Long itemId, String direction) {
        ReviewItem item = new ReviewItem(itemType, itemId, direction);
        setId(item, id);
        return item;
    }

    @SuppressWarnings("unchecked")
    private <T> void setId(T entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

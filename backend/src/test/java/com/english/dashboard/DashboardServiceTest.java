package com.english.dashboard;

import com.english.auth.User;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewLogRepository;
import com.english.study.StudyRecord;
import com.english.study.StudyRecordItemRepository;
import com.english.study.StudyRecordRepository;
import com.english.word.WordRepository;
import com.english.pattern.PatternRepository;
import com.english.generate.GeneratedSentenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private WordRepository wordRepository;
    @Mock
    private PatternRepository patternRepository;
    @Mock
    private GeneratedSentenceRepository generatedSentenceRepository;
    @Mock
    private ReviewItemRepository reviewItemRepository;
    @Mock
    private ReviewLogRepository reviewLogRepository;
    @Mock
    private StudyRecordRepository studyRecordRepository;
    @Mock
    private StudyRecordItemRepository studyRecordItemRepository;

    @InjectMocks
    private DashboardService dashboardService;

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
    }

    @Test
    @DisplayName("단어/패턴/예문 카운트 (deleted=false만)")
    void counts() {
        given(wordRepository.countByUserAndDeletedFalse(testUser)).willReturn(10L);
        given(patternRepository.countByUserAndDeletedFalse(testUser)).willReturn(5L);
        given(generatedSentenceRepository.countByUser(testUser)).willReturn(20L);
        given(reviewLogRepository.findDistinctReviewDates(testUser)).willReturn(Collections.emptyList());
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("WORD"), any(LocalDate.class))).willReturn(0L);
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("PATTERN"), any(LocalDate.class))).willReturn(0L);
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("SENTENCE"), any(LocalDate.class))).willReturn(0L);
        given(studyRecordRepository.findTop5ByUserOrderByCreatedAtDesc(testUser)).willReturn(Collections.emptyList());

        DashboardResponse result = dashboardService.getDashboard(testUser);

        assertThat(result.getWordCount()).isEqualTo(10);
        assertThat(result.getPatternCount()).isEqualTo(5);
        assertThat(result.getSentenceCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("streak: 오늘 복습함 → 오늘부터 역순 연속일")
    void streak_today() {
        setupDefaultMocks();
        LocalDate today = LocalDate.now();
        // 오늘, 어제, 그제 연속 복습
        given(reviewLogRepository.findDistinctReviewDates(testUser))
                .willReturn(List.of(today, today.minusDays(1), today.minusDays(2)));

        DashboardResponse result = dashboardService.getDashboard(testUser);

        assertThat(result.getStreak()).isEqualTo(3);
    }

    @Test
    @DisplayName("streak: 오늘 안 함, 어제 함 → 어제부터 역순 연속일")
    void streak_yesterday() {
        setupDefaultMocks();
        LocalDate today = LocalDate.now();
        // 어제, 그제 연속 복습 (오늘은 안 함)
        given(reviewLogRepository.findDistinctReviewDates(testUser))
                .willReturn(List.of(today.minusDays(1), today.minusDays(2)));

        DashboardResponse result = dashboardService.getDashboard(testUser);

        assertThat(result.getStreak()).isEqualTo(2);
    }

    @Test
    @DisplayName("streak: 복습 기록 없음 → 0")
    void streak_none() {
        setupDefaultMocks();
        given(reviewLogRepository.findDistinctReviewDates(testUser)).willReturn(Collections.emptyList());

        DashboardResponse result = dashboardService.getDashboard(testUser);

        assertThat(result.getStreak()).isEqualTo(0);
    }

    @Test
    @DisplayName("streak: 연속이 끊긴 경우")
    void streak_broken() {
        setupDefaultMocks();
        LocalDate today = LocalDate.now();
        // 오늘, 어제 복습 + 3일 전 (그제 빠짐 → 연속 2일)
        given(reviewLogRepository.findDistinctReviewDates(testUser))
                .willReturn(List.of(today, today.minusDays(1), today.minusDays(3)));

        DashboardResponse result = dashboardService.getDashboard(testUser);

        assertThat(result.getStreak()).isEqualTo(2);
    }

    @Test
    @DisplayName("타입별 todayReviewRemaining")
    void todayReviewRemaining() {
        given(wordRepository.countByUserAndDeletedFalse(testUser)).willReturn(0L);
        given(patternRepository.countByUserAndDeletedFalse(testUser)).willReturn(0L);
        given(generatedSentenceRepository.countByUser(testUser)).willReturn(0L);
        given(reviewLogRepository.findDistinctReviewDates(testUser)).willReturn(Collections.emptyList());
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("WORD"), any(LocalDate.class))).willReturn(5L);
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("PATTERN"), any(LocalDate.class))).willReturn(3L);
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("SENTENCE"), any(LocalDate.class))).willReturn(8L);
        given(studyRecordRepository.findTop5ByUserOrderByCreatedAtDesc(testUser)).willReturn(Collections.emptyList());

        DashboardResponse result = dashboardService.getDashboard(testUser);

        assertThat(result.getTodayReviewRemaining().getWord()).isEqualTo(5);
        assertThat(result.getTodayReviewRemaining().getPattern()).isEqualTo(3);
        assertThat(result.getTodayReviewRemaining().getSentence()).isEqualTo(8);
    }

    @Test
    @DisplayName("최근 학습 기록 5개")
    void recentStudyRecords() {
        setupDefaultMocks();
        given(reviewLogRepository.findDistinctReviewDates(testUser)).willReturn(Collections.emptyList());

        StudyRecord r1 = new StudyRecord(testUser, 3, LocalDate.now());
        StudyRecord r2 = new StudyRecord(testUser, 2, LocalDate.now().minusDays(1));
        setId(r1, 1L);
        setId(r2, 2L);
        given(studyRecordRepository.findTop5ByUserOrderByCreatedAtDesc(testUser)).willReturn(List.of(r1, r2));
        given(studyRecordItemRepository.countByStudyRecordIdAndItemType(1L, "WORD")).willReturn(3L);
        given(studyRecordItemRepository.countByStudyRecordIdAndItemType(1L, "PATTERN")).willReturn(1L);
        given(studyRecordItemRepository.countByStudyRecordIdAndItemType(2L, "WORD")).willReturn(2L);
        given(studyRecordItemRepository.countByStudyRecordIdAndItemType(2L, "PATTERN")).willReturn(0L);

        DashboardResponse result = dashboardService.getDashboard(testUser);

        assertThat(result.getRecentStudyRecords()).hasSize(2);
        assertThat(result.getRecentStudyRecords().get(0).getDayNumber()).isEqualTo(3);
        assertThat(result.getRecentStudyRecords().get(0).getWordCount()).isEqualTo(3);
        assertThat(result.getRecentStudyRecords().get(0).getPatternCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("첫 사용자 (모든 카운트 0)")
    void firstUser() {
        given(wordRepository.countByUserAndDeletedFalse(testUser)).willReturn(0L);
        given(patternRepository.countByUserAndDeletedFalse(testUser)).willReturn(0L);
        given(generatedSentenceRepository.countByUser(testUser)).willReturn(0L);
        given(reviewLogRepository.findDistinctReviewDates(testUser)).willReturn(Collections.emptyList());
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("WORD"), any(LocalDate.class))).willReturn(0L);
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("PATTERN"), any(LocalDate.class))).willReturn(0L);
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("SENTENCE"), any(LocalDate.class))).willReturn(0L);
        given(studyRecordRepository.findTop5ByUserOrderByCreatedAtDesc(testUser)).willReturn(Collections.emptyList());

        DashboardResponse result = dashboardService.getDashboard(testUser);

        assertThat(result.getWordCount()).isEqualTo(0);
        assertThat(result.getPatternCount()).isEqualTo(0);
        assertThat(result.getSentenceCount()).isEqualTo(0);
        assertThat(result.getStreak()).isEqualTo(0);
        assertThat(result.getRecentStudyRecords()).isEmpty();
    }

    private void setupDefaultMocks() {
        given(wordRepository.countByUserAndDeletedFalse(testUser)).willReturn(0L);
        given(patternRepository.countByUserAndDeletedFalse(testUser)).willReturn(0L);
        given(generatedSentenceRepository.countByUser(testUser)).willReturn(0L);
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("WORD"), any(LocalDate.class))).willReturn(0L);
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("PATTERN"), any(LocalDate.class))).willReturn(0L);
        given(reviewItemRepository.countTodayRemaining(eq(testUser), eq("SENTENCE"), any(LocalDate.class))).willReturn(0L);
        given(studyRecordRepository.findTop5ByUserOrderByCreatedAtDesc(testUser)).willReturn(Collections.emptyList());
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

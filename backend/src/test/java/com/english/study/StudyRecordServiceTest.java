package com.english.study;

import com.english.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudyRecordServiceTest {

    @Mock
    private StudyRecordRepository studyRecordRepository;

    @Mock
    private StudyRecordItemRepository studyRecordItemRepository;

    @InjectMocks
    private StudyRecordService studyRecordService;

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
    @DisplayName("학습 기록 목록 조회 - 최신순, 페이지네이션")
    void getRecords() {
        // given
        StudyRecord r1 = new StudyRecord(testUser, 2, LocalDate.now());
        StudyRecord r2 = new StudyRecord(testUser, 1, LocalDate.now().minusDays(1));
        setId(r1, 1L);
        setId(r2, 2L);

        PageRequest pageable = PageRequest.of(0, 10);
        given(studyRecordRepository.findByUserOrderByCreatedAtDesc(testUser, pageable))
                .willReturn(new PageImpl<>(List.of(r1, r2), pageable, 2));
        given(studyRecordItemRepository.countByStudyRecordIdAndItemType(1L, "WORD")).willReturn(3L);
        given(studyRecordItemRepository.countByStudyRecordIdAndItemType(1L, "PATTERN")).willReturn(1L);
        given(studyRecordItemRepository.countByStudyRecordIdAndItemType(2L, "WORD")).willReturn(5L);
        given(studyRecordItemRepository.countByStudyRecordIdAndItemType(2L, "PATTERN")).willReturn(2L);

        // when
        Page<StudyRecordResponse> result = studyRecordService.getRecords(testUser, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getDayNumber()).isEqualTo(2);
        assertThat(result.getContent().get(0).getWordCount()).isEqualTo(3);
        assertThat(result.getContent().get(0).getPatternCount()).isEqualTo(1);
        assertThat(result.getContent().get(1).getDayNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("학습 기록 없을 때 빈 페이지 반환")
    void getRecords_empty() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        given(studyRecordRepository.findByUserOrderByCreatedAtDesc(testUser, pageable))
                .willReturn(Page.empty(pageable));

        // when
        Page<StudyRecordResponse> result = studyRecordService.getRecords(testUser, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
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

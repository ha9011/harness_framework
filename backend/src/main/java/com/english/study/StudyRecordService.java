package com.english.study;

import com.english.auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StudyRecordService {

    private final StudyRecordRepository studyRecordRepository;
    private final StudyRecordItemRepository studyRecordItemRepository;

    /**
     * 오늘 날짜 레코드가 없으면 생성, 있으면 반환
     */
    @Transactional
    public StudyRecord getOrCreateTodayRecord(User user) {
        LocalDate today = LocalDate.now();
        return studyRecordRepository.findByUserAndCreatedAt(user, today)
                .orElseGet(() -> {
                    int nextDayNumber = studyRecordRepository.findMaxDayNumber(user) + 1;
                    return studyRecordRepository.save(new StudyRecord(user, nextDayNumber, today));
                });
    }

    /**
     * 학습 기록에 아이템 추가
     */
    @Transactional
    public void addItem(StudyRecord record, String itemType, Long itemId) {
        StudyRecordItem item = new StudyRecordItem(record.getId(), itemType, itemId);
        studyRecordItemRepository.save(item);
    }

    /**
     * 학습 기록 목록 조회 (최신순, 페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<StudyRecordResponse> getRecords(User user, Pageable pageable) {
        return studyRecordRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(record -> new StudyRecordResponse(
                        record.getId(),
                        record.getDayNumber(),
                        record.getCreatedAt().toString(),
                        (int) studyRecordItemRepository.countByStudyRecordIdAndItemType(record.getId(), "WORD"),
                        (int) studyRecordItemRepository.countByStudyRecordIdAndItemType(record.getId(), "PATTERN")
                ));
    }
}

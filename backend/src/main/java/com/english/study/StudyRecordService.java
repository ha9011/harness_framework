package com.english.study;

import lombok.RequiredArgsConstructor;
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
    public StudyRecord getOrCreateTodayRecord() {
        LocalDate today = LocalDate.now();
        return studyRecordRepository.findByCreatedAt(today)
                .orElseGet(() -> {
                    int nextDayNumber = studyRecordRepository.findMaxDayNumber() + 1;
                    return studyRecordRepository.save(new StudyRecord(nextDayNumber, today));
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
}

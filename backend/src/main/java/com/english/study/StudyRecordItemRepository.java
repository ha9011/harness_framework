package com.english.study;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRecordItemRepository extends JpaRepository<StudyRecordItem, Long> {

    long countByStudyRecordIdAndItemType(Long studyRecordId, String itemType);
}

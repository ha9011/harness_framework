package com.english.study;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRecordItemRepository extends JpaRepository<StudyRecordItem, Long> {

	List<StudyRecordItem> findByStudyRecordId(Long studyRecordId);

	boolean existsByStudyRecordIdAndItemTypeAndItemId(Long studyRecordId, StudyItemType itemType, Long itemId);
}

package com.english.study;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRecordItemRepository extends JpaRepository<StudyRecordItem, Long> {

	List<StudyRecordItem> findByStudyRecordId(Long studyRecordId);

	List<StudyRecordItem> findByStudyRecordIdInOrderByIdAsc(Collection<Long> studyRecordIds);

	boolean existsByStudyRecordIdAndItemTypeAndItemId(Long studyRecordId, StudyItemType itemType, Long itemId);
}

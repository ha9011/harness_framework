package com.english.study;

import com.english.auth.User;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyRecordService {

	private final StudyRecordRepository studyRecordRepository;
	private final StudyRecordItemRepository studyRecordItemRepository;

	public StudyRecordService(
			StudyRecordRepository studyRecordRepository,
			StudyRecordItemRepository studyRecordItemRepository
	) {
		this.studyRecordRepository = studyRecordRepository;
		this.studyRecordItemRepository = studyRecordItemRepository;
	}

	@Transactional
	public StudyRecord recordLearning(User user, StudyItemType itemType, Long itemId, LocalDate studyDate) {
		StudyRecord studyRecord = studyRecordRepository.findByUserIdAndStudyDate(user.getId(), studyDate)
				.orElseGet(() -> studyRecordRepository.save(new StudyRecord(
						user,
						studyDate,
						nextDayNumber(user.getId()))));

		if (!studyRecordItemRepository.existsByStudyRecordIdAndItemTypeAndItemId(
				studyRecord.getId(),
				itemType,
				itemId)) {
			studyRecordItemRepository.save(new StudyRecordItem(studyRecord, itemType, itemId));
		}

		return studyRecord;
	}

	private int nextDayNumber(Long userId) {
		return Math.toIntExact(studyRecordRepository.countByUserId(userId) + 1);
	}
}

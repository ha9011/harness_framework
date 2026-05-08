package com.english.study;

import static org.assertj.core.api.Assertions.assertThat;

import com.english.auth.User;
import com.english.auth.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class StudyRecordServiceTest {

	private final StudyRecordService studyRecordService;
	private final UserRepository userRepository;
	private final StudyRecordRepository studyRecordRepository;
	private final StudyRecordItemRepository studyRecordItemRepository;

	@Autowired
	StudyRecordServiceTest(
			StudyRecordService studyRecordService,
			UserRepository userRepository,
			StudyRecordRepository studyRecordRepository,
			StudyRecordItemRepository studyRecordItemRepository
	) {
		this.studyRecordService = studyRecordService;
		this.userRepository = userRepository;
		this.studyRecordRepository = studyRecordRepository;
		this.studyRecordItemRepository = studyRecordItemRepository;
	}

	@Test
	void recordLearningCreatesOneRecordPerDateAndAvoidsDuplicateItems() {
		User user = saveUser("study-record@example.com");
		LocalDate firstDay = LocalDate.of(2026, 5, 8);

		StudyRecord firstRecord = studyRecordService.recordLearning(user, StudyItemType.WORD, 100L, firstDay);
		StudyRecord duplicateRecord = studyRecordService.recordLearning(user, StudyItemType.WORD, 100L, firstDay);
		StudyRecord sameDayPatternRecord = studyRecordService.recordLearning(user, StudyItemType.PATTERN, 200L, firstDay);

		assertThat(firstRecord.getId()).isEqualTo(duplicateRecord.getId()).isEqualTo(sameDayPatternRecord.getId());
		assertThat(firstRecord.getDayNumber()).isEqualTo(1);
		assertThat(studyRecordRepository.findByUserIdAndStudyDate(user.getId(), firstDay)).isPresent();
		List<StudyRecordItem> items = studyRecordItemRepository.findByStudyRecordId(firstRecord.getId());
		assertThat(items)
				.extracting(StudyRecordItem::getItemType, StudyRecordItem::getItemId)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(StudyItemType.WORD, 100L),
						org.assertj.core.groups.Tuple.tuple(StudyItemType.PATTERN, 200L));
	}

	@Test
	void dayNumberIsSequentialPerUserByActualStudyDates() {
		User user = saveUser("study-sequence@example.com");
		User otherUser = saveUser("study-sequence-other@example.com");

		StudyRecord firstDay = studyRecordService.recordLearning(
				user,
				StudyItemType.WORD,
				1L,
				LocalDate.of(2026, 5, 1));
		StudyRecord secondStudyDay = studyRecordService.recordLearning(
				user,
				StudyItemType.PATTERN,
				2L,
				LocalDate.of(2026, 5, 8));
		StudyRecord otherUsersFirstDay = studyRecordService.recordLearning(
				otherUser,
				StudyItemType.WORD,
				1L,
				LocalDate.of(2026, 5, 8));

		assertThat(firstDay.getDayNumber()).isEqualTo(1);
		assertThat(secondStudyDay.getDayNumber()).isEqualTo(2);
		assertThat(otherUsersFirstDay.getDayNumber()).isEqualTo(1);
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}
}

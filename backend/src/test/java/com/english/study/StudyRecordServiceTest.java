package com.english.study;

import static org.assertj.core.api.Assertions.assertThat;

import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.word.Word;
import com.english.word.WordRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;

	@Autowired
	StudyRecordServiceTest(
			StudyRecordService studyRecordService,
			UserRepository userRepository,
			StudyRecordRepository studyRecordRepository,
			StudyRecordItemRepository studyRecordItemRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository
	) {
		this.studyRecordService = studyRecordService;
		this.userRepository = userRepository;
		this.studyRecordRepository = studyRecordRepository;
		this.studyRecordItemRepository = studyRecordItemRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
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

	@Test
	void recordLearningCanBeCalledWithUserIdAndUsesToday() {
		User user = saveUser("study-user-id@example.com");
		Word word = wordRepository.save(new Word(user, "coffee", "커피"));

		StudyRecord record = studyRecordService.recordLearning(user.getId(), StudyItemType.WORD, word.getId());

		assertThat(record.getStudyDate()).isEqualTo(LocalDate.now());
		assertThat(record.getDayNumber()).isEqualTo(1);
		assertThat(studyRecordItemRepository.findByStudyRecordId(record.getId()))
				.extracting(StudyRecordItem::getItemType, StudyRecordItem::getItemId)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(StudyItemType.WORD, word.getId()));
	}

	@Test
	void getStudyRecordsReturnsLatestPageWithItemDisplayNames() {
		User user = saveUser("study-query@example.com");
		User otherUser = saveUser("study-query-other@example.com");
		Word coffee = wordRepository.save(new Word(user, "brew coffee", "커피를 내리다"));
		Word latte = wordRepository.save(new Word(user, "latte", "라떼"));
		Pattern pattern = patternRepository.save(new Pattern(user, "I used to...", "과거 습관"));
		Word otherUsersWord = wordRepository.save(new Word(otherUser, "other word", "다른 사용자 단어"));

		studyRecordService.recordLearning(user, StudyItemType.WORD, coffee.getId(), LocalDate.of(2026, 5, 1));
		studyRecordService.recordLearning(user, StudyItemType.PATTERN, pattern.getId(), LocalDate.of(2026, 5, 1));
		studyRecordService.recordLearning(user, StudyItemType.WORD, latte.getId(), LocalDate.of(2026, 5, 8));
		studyRecordService.recordLearning(otherUser, StudyItemType.WORD, otherUsersWord.getId(), LocalDate.of(2026, 5, 9));

		Page<StudyRecordResponse> firstPage = studyRecordService.getStudyRecords(user.getId(), 0, 1);
		Page<StudyRecordResponse> secondPage = studyRecordService.getStudyRecords(user.getId(), 1, 1);

		assertThat(firstPage.getTotalElements()).isEqualTo(2);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);
		assertThat(firstPage.getContent())
				.extracting(StudyRecordResponse::studyDate, StudyRecordResponse::dayNumber)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 5, 8), 2));
		assertThat(firstPage.getContent().getFirst().items())
				.extracting(StudyRecordItemResponse::type, StudyRecordItemResponse::id, StudyRecordItemResponse::name)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(StudyItemType.WORD, latte.getId(), "latte"));

		assertThat(secondPage.getContent())
				.extracting(StudyRecordResponse::studyDate, StudyRecordResponse::dayNumber)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 5, 1), 1));
		assertThat(secondPage.getContent().getFirst().items())
				.extracting(StudyRecordItemResponse::type, StudyRecordItemResponse::id, StudyRecordItemResponse::name)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(StudyItemType.WORD, coffee.getId(), "brew coffee"),
						org.assertj.core.groups.Tuple.tuple(StudyItemType.PATTERN, pattern.getId(), "I used to..."));
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}
}

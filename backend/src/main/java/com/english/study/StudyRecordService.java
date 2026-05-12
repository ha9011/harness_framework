package com.english.study;

import com.english.auth.AuthException;
import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.word.Word;
import com.english.word.WordRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyRecordService {

	private final StudyRecordRepository studyRecordRepository;
	private final StudyRecordItemRepository studyRecordItemRepository;
	private final UserRepository userRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;

	public StudyRecordService(
			StudyRecordRepository studyRecordRepository,
			StudyRecordItemRepository studyRecordItemRepository,
			UserRepository userRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository
	) {
		this.studyRecordRepository = studyRecordRepository;
		this.studyRecordItemRepository = studyRecordItemRepository;
		this.userRepository = userRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
	}

	@Transactional
	public StudyRecord recordLearning(Long userId, StudyItemType itemType, Long itemId) {
		return recordLearning(userId, itemType, itemId, LocalDate.now());
	}

	@Transactional
	public StudyRecord recordLearning(Long userId, StudyItemType itemType, Long itemId, LocalDate studyDate) {
		return recordLearning(findUser(userId), itemType, itemId, studyDate);
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

	@Transactional(readOnly = true)
	public Page<StudyRecordResponse> getStudyRecords(Long userId, int page, int size) {
		Page<StudyRecord> studyRecords = studyRecordRepository.findByUserIdOrderByStudyDateDescIdDesc(
				userId,
				PageRequest.of(page, size));
		List<Long> recordIds = studyRecords.getContent().stream()
				.map(StudyRecord::getId)
				.toList();
		List<StudyRecordItem> items = recordIds.isEmpty()
				? List.of()
				: studyRecordItemRepository.findByStudyRecordIdInOrderByIdAsc(recordIds);
		Map<Long, List<StudyRecordItem>> itemsByRecordId = items.stream()
				.collect(Collectors.groupingBy(
						item -> item.getStudyRecord().getId(),
						LinkedHashMap::new,
						Collectors.toList()));
		Map<Long, String> wordNames = wordNames(userId, items);
		Map<Long, String> patternNames = patternNames(userId, items);

		return studyRecords.map(record -> new StudyRecordResponse(
				record.getId(),
				record.getStudyDate(),
				record.getDayNumber(),
				itemsByRecordId.getOrDefault(record.getId(), List.of()).stream()
						.map(item -> toItemResponse(item, wordNames, patternNames))
						.toList()));
	}

	private int nextDayNumber(Long userId) {
		return Math.toIntExact(studyRecordRepository.countByUserId(userId) + 1);
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(AuthException::invalidToken);
	}

	private Map<Long, String> wordNames(Long userId, List<StudyRecordItem> items) {
		Set<Long> wordIds = items.stream()
				.filter(item -> item.getItemType() == StudyItemType.WORD)
				.map(StudyRecordItem::getItemId)
				.collect(Collectors.toSet());
		if (wordIds.isEmpty()) {
			return Map.of();
		}
		return wordRepository.findAllById(wordIds).stream()
				.filter(word -> word.getUser().getId().equals(userId))
				.collect(Collectors.toMap(Word::getId, Word::getWord));
	}

	private Map<Long, String> patternNames(Long userId, List<StudyRecordItem> items) {
		Set<Long> patternIds = items.stream()
				.filter(item -> item.getItemType() == StudyItemType.PATTERN)
				.map(StudyRecordItem::getItemId)
				.collect(Collectors.toSet());
		if (patternIds.isEmpty()) {
			return Map.of();
		}
		return patternRepository.findAllById(patternIds).stream()
				.filter(pattern -> pattern.getUser().getId().equals(userId))
				.collect(Collectors.toMap(Pattern::getId, Pattern::getTemplate));
	}

	private StudyRecordItemResponse toItemResponse(
			StudyRecordItem item,
			Map<Long, String> wordNames,
			Map<Long, String> patternNames
	) {
		String name = switch (item.getItemType()) {
			case WORD -> wordNames.get(item.getItemId());
			case PATTERN -> patternNames.get(item.getItemId());
		};
		if (name == null) {
			name = "(삭제됨)";
		}
		return new StudyRecordItemResponse(item.getItemType(), item.getItemId(), name);
	}
}

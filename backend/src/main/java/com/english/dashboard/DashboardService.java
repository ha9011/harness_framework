package com.english.dashboard;

import com.english.review.ReviewItemType;
import com.english.study.StudyRecordResponse;
import com.english.study.StudyRecordService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

	private final EntityManager entityManager;
	private final StudyRecordService studyRecordService;
	private final StreakCalculator streakCalculator;

	public DashboardService(
			EntityManager entityManager,
			StudyRecordService studyRecordService,
			StreakCalculator streakCalculator
	) {
		this.entityManager = entityManager;
		this.studyRecordService = studyRecordService;
		this.streakCalculator = streakCalculator;
	}

	@Transactional(readOnly = true)
	public DashboardResponse getDashboard(Long userId) {
		Long requestedUserId = requireId(userId);
		LocalDate today = LocalDate.now();
		TodayReviewRemainingResponse todayReviewRemaining = new TodayReviewRemainingResponse(
				countDueReviewItems(requestedUserId, ReviewItemType.WORD, today),
				countDueReviewItems(requestedUserId, ReviewItemType.PATTERN, today),
				countDueReviewItems(requestedUserId, ReviewItemType.SENTENCE, today));
		List<StudyRecordResponse> recentStudyRecords = studyRecordService
				.getStudyRecords(requestedUserId, 0, 5)
				.getContent();

		return new DashboardResponse(
				countActiveWords(requestedUserId),
				countActivePatterns(requestedUserId),
				countActiveSentences(requestedUserId),
				streakCalculator.calculate(reviewedDates(requestedUserId), today),
				todayReviewRemaining,
				recentStudyRecords);
	}

	private long countActiveWords(Long userId) {
		return entityManager.createQuery("""
				select count(word)
				from Word word
				where word.user.id = :userId
					and word.deleted = false
				""", Long.class)
				.setParameter("userId", userId)
				.getSingleResult();
	}

	private long countActivePatterns(Long userId) {
		return entityManager.createQuery("""
				select count(pattern)
				from Pattern pattern
				where pattern.user.id = :userId
					and pattern.deleted = false
				""", Long.class)
				.setParameter("userId", userId)
				.getSingleResult();
	}

	private long countActiveSentences(Long userId) {
		return entityManager.createQuery("""
				select count(sentence)
				from GeneratedSentence sentence
				where sentence.user.id = :userId
					and sentence.deleted = false
				""", Long.class)
				.setParameter("userId", userId)
				.getSingleResult();
	}

	private long countDueReviewItems(Long userId, ReviewItemType itemType, LocalDate today) {
		return switch (itemType) {
			case WORD -> countDueWordReviewItems(userId, today);
			case PATTERN -> countDuePatternReviewItems(userId, today);
			case SENTENCE -> countDueSentenceReviewItems(userId, today);
		};
	}

	private long countDueWordReviewItems(Long userId, LocalDate today) {
		return entityManager.createQuery("""
				select count(item)
				from ReviewItem item
				where item.user.id = :userId
					and item.itemType = :itemType
					and item.deleted = false
					and item.nextReviewDate <= :today
					and exists (
						select word.id
						from Word word
						where word.id = item.itemId
							and word.user.id = :userId
							and word.deleted = false
					)
				""", Long.class)
				.setParameter("userId", userId)
				.setParameter("itemType", ReviewItemType.WORD)
				.setParameter("today", today)
				.getSingleResult();
	}

	private long countDuePatternReviewItems(Long userId, LocalDate today) {
		return entityManager.createQuery("""
				select count(item)
				from ReviewItem item
				where item.user.id = :userId
					and item.itemType = :itemType
					and item.deleted = false
					and item.nextReviewDate <= :today
					and exists (
						select pattern.id
						from Pattern pattern
						where pattern.id = item.itemId
							and pattern.user.id = :userId
							and pattern.deleted = false
					)
				""", Long.class)
				.setParameter("userId", userId)
				.setParameter("itemType", ReviewItemType.PATTERN)
				.setParameter("today", today)
				.getSingleResult();
	}

	private long countDueSentenceReviewItems(Long userId, LocalDate today) {
		return entityManager.createQuery("""
				select count(item)
				from ReviewItem item
				where item.user.id = :userId
					and item.itemType = :itemType
					and item.deleted = false
					and item.nextReviewDate <= :today
					and exists (
						select sentence.id
						from GeneratedSentence sentence
						where sentence.id = item.itemId
							and sentence.user.id = :userId
							and sentence.deleted = false
					)
				""", Long.class)
				.setParameter("userId", userId)
				.setParameter("itemType", ReviewItemType.SENTENCE)
				.setParameter("today", today)
				.getSingleResult();
	}

	private Set<LocalDate> reviewedDates(Long userId) {
		List<Instant> reviewedAt = entityManager.createQuery("""
				select log.reviewedAt
				from ReviewLog log
				where log.reviewItem.user.id = :userId
				""", Instant.class)
				.setParameter("userId", userId)
				.getResultList();
		ZoneId zoneId = ZoneId.systemDefault();
		return reviewedAt.stream()
				.map(instant -> LocalDate.ofInstant(instant, zoneId))
				.collect(Collectors.toSet());
	}

	private Long requireId(Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("사용자 ID는 필수입니다");
		}
		return userId;
	}
}

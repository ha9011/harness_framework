package com.english.review;

import com.english.auth.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewItemCreationService {

	private static final List<ReviewDirection> BIDIRECTIONAL_DIRECTIONS = List.of(
			ReviewDirection.RECOGNITION,
			ReviewDirection.RECALL);

	private final ReviewItemRepository reviewItemRepository;

	public ReviewItemCreationService(ReviewItemRepository reviewItemRepository) {
		this.reviewItemRepository = reviewItemRepository;
	}

	@Transactional
	public void createWordReviewItems(User user, Long wordId, LocalDate nextReviewDate) {
		createBidirectionalReviewItems(user, ReviewItemType.WORD, wordId, nextReviewDate);
	}

	@Transactional
	public void createPatternReviewItems(User user, Long patternId, LocalDate nextReviewDate) {
		createBidirectionalReviewItems(user, ReviewItemType.PATTERN, patternId, nextReviewDate);
	}

	@Transactional
	public void softDeleteReviewItems(User user, ReviewItemType itemType, Long itemId) {
		reviewItemRepository.findByUserIdAndItemTypeAndItemIdAndDeletedFalse(
						user.getId(),
						itemType,
						itemId)
				.forEach(ReviewItem::softDelete);
	}

	private void createBidirectionalReviewItems(
			User user,
			ReviewItemType itemType,
			Long itemId,
			LocalDate nextReviewDate
	) {
		BIDIRECTIONAL_DIRECTIONS.forEach(direction -> createIfMissing(
				user,
				itemType,
				itemId,
				direction,
				nextReviewDate));
	}

	private void createIfMissing(
			User user,
			ReviewItemType itemType,
			Long itemId,
			ReviewDirection direction,
			LocalDate nextReviewDate
	) {
		boolean exists = reviewItemRepository.findByUserIdAndItemTypeAndItemIdAndDirectionAndDeletedFalse(
				user.getId(),
				itemType,
				itemId,
				direction).isPresent();

		if (!exists) {
			reviewItemRepository.save(new ReviewItem(user, itemType, itemId, direction, nextReviewDate));
		}
	}
}

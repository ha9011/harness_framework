package com.english.review;

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
class ReviewItemCreationTest {

	private final ReviewItemCreationService reviewItemCreationService;
	private final UserRepository userRepository;
	private final ReviewItemRepository reviewItemRepository;

	@Autowired
	ReviewItemCreationTest(
			ReviewItemCreationService reviewItemCreationService,
			UserRepository userRepository,
			ReviewItemRepository reviewItemRepository
	) {
		this.reviewItemCreationService = reviewItemCreationService;
		this.userRepository = userRepository;
		this.reviewItemRepository = reviewItemRepository;
	}

	@Test
	void createWordReviewItemsCreatesRecognitionAndRecallOnlyOnce() {
		User user = saveUser("review-word@example.com");
		LocalDate today = LocalDate.of(2026, 5, 8);

		reviewItemCreationService.createWordReviewItems(user, 10L, today);
		reviewItemCreationService.createWordReviewItems(user, 10L, today);

		List<ReviewItem> reviewItems = reviewItemRepository.findByUserIdAndItemTypeAndItemId(
				user.getId(),
				ReviewItemType.WORD,
				10L);
		assertThat(reviewItems).hasSize(2);
		assertThat(reviewItems)
				.extracting(ReviewItem::getDirection)
				.containsExactlyInAnyOrder(ReviewDirection.RECOGNITION, ReviewDirection.RECALL);
		assertThat(reviewItems)
				.allSatisfy(reviewItem -> {
					assertThat(reviewItem.getNextReviewDate()).isEqualTo(today);
					assertThat(reviewItem.isDeleted()).isFalse();
				});
	}

	@Test
	void createPatternReviewItemsCreatesRecognitionAndRecallOnlyOnce() {
		User user = saveUser("review-pattern@example.com");
		LocalDate today = LocalDate.of(2026, 5, 8);

		reviewItemCreationService.createPatternReviewItems(user, 20L, today);
		reviewItemCreationService.createPatternReviewItems(user, 20L, today);

		List<ReviewItem> reviewItems = reviewItemRepository.findByUserIdAndItemTypeAndItemId(
				user.getId(),
				ReviewItemType.PATTERN,
				20L);
		assertThat(reviewItems).hasSize(2);
		assertThat(reviewItems)
				.extracting(ReviewItem::getDirection)
				.containsExactlyInAnyOrder(ReviewDirection.RECOGNITION, ReviewDirection.RECALL);
	}

	@Test
	void softDeleteReviewItemsOnlyDeletesMatchingUserTypeAndItem() {
		User owner = saveUser("review-delete-owner@example.com");
		User otherUser = saveUser("review-delete-other@example.com");
		LocalDate today = LocalDate.of(2026, 5, 8);
		reviewItemCreationService.createWordReviewItems(owner, 1L, today);
		reviewItemCreationService.createPatternReviewItems(owner, 1L, today);
		reviewItemCreationService.createWordReviewItems(otherUser, 1L, today);
		ReviewItem sentenceReview = reviewItemRepository.save(new ReviewItem(
				owner,
				ReviewItemType.SENTENCE,
				1L,
				ReviewDirection.RECOGNITION,
				today));

		reviewItemCreationService.softDeleteReviewItems(owner, ReviewItemType.WORD, 1L);

		assertThat(reviewItemRepository.findByUserIdAndItemTypeAndItemId(owner.getId(), ReviewItemType.WORD, 1L))
				.allSatisfy(reviewItem -> assertThat(reviewItem.isDeleted()).isTrue());
		assertThat(reviewItemRepository.findByUserIdAndItemTypeAndItemId(owner.getId(), ReviewItemType.PATTERN, 1L))
				.allSatisfy(reviewItem -> assertThat(reviewItem.isDeleted()).isFalse());
		assertThat(reviewItemRepository.findByUserIdAndItemTypeAndItemId(otherUser.getId(), ReviewItemType.WORD, 1L))
				.allSatisfy(reviewItem -> assertThat(reviewItem.isDeleted()).isFalse());
		assertThat(reviewItemRepository.findById(sentenceReview.getId()).orElseThrow().isDeleted()).isFalse();
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}
}

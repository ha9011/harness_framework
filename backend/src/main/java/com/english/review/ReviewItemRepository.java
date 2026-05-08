package com.english.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewItemRepository extends JpaRepository<ReviewItem, Long> {

	List<ReviewItem> findByUserIdAndItemTypeAndItemId(Long userId, ReviewItemType itemType, Long itemId);

	List<ReviewItem> findByUserIdAndItemTypeAndItemIdAndDeletedFalse(
			Long userId,
			ReviewItemType itemType,
			Long itemId);

	Optional<ReviewItem> findByUserIdAndItemTypeAndItemIdAndDirectionAndDeletedFalse(
			Long userId,
			ReviewItemType itemType,
			Long itemId,
			ReviewDirection direction);

	List<ReviewItem> findByUserIdAndItemTypeAndDirectionAndDeletedFalse(
			Long userId,
			ReviewItemType itemType,
			ReviewDirection direction);
}

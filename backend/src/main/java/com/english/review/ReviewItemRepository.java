package com.english.review;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	@Query("""
			select item
			from ReviewItem item
			where item.user.id = :userId
				and item.itemType = :itemType
				and item.deleted = false
				and item.nextReviewDate <= :today
			order by
				case when item.lastResult = :hardResult then 0 else 1 end,
				case when item.lastReviewedAt is null then 0 else 1 end,
				item.lastReviewedAt asc,
				item.reviewCount asc,
				item.id asc
			""")
	List<ReviewItem> findDueReviewItems(
			@Param("userId") Long userId,
			@Param("itemType") ReviewItemType itemType,
			@Param("today") LocalDate today,
			@Param("hardResult") ReviewResult hardResult);
}

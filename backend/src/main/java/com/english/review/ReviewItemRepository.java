package com.english.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReviewItemRepository extends JpaRepository<ReviewItem, Long> {

    @Modifying
    @Query("UPDATE ReviewItem r SET r.deleted = true WHERE r.itemType = :itemType AND r.itemId = :itemId")
    void softDeleteByItemTypeAndItemId(@Param("itemType") String itemType, @Param("itemId") Long itemId);

    List<ReviewItem> findByItemTypeAndItemId(String itemType, Long itemId);

    @Query("SELECT COUNT(r) FROM ReviewItem r WHERE r.itemType = :itemType " +
            "AND r.nextReviewDate <= :today AND r.deleted = false")
    long countTodayRemaining(@Param("itemType") String itemType, @Param("today") LocalDate today);

    @Query("SELECT r FROM ReviewItem r WHERE r.itemType = :itemType " +
            "AND r.nextReviewDate <= :today AND r.deleted = false " +
            "AND r.id NOT IN :excludeIds " +
            "ORDER BY CASE WHEN r.lastResult = 'HARD' THEN 0 ELSE 1 END, " +
            "r.lastReviewedAt ASC NULLS FIRST, r.reviewCount ASC")
    List<ReviewItem> findTodayCards(@Param("itemType") String itemType,
                                    @Param("today") LocalDate today,
                                    @Param("excludeIds") List<Long> excludeIds);
}

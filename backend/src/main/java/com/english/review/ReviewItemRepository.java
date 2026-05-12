package com.english.review;

import com.english.auth.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReviewItemRepository extends JpaRepository<ReviewItem, Long> {

    @Modifying
    @Query("UPDATE ReviewItem r SET r.deleted = true WHERE r.user = :user AND r.itemType = :itemType AND r.itemId = :itemId")
    void softDeleteByUserAndItemTypeAndItemId(@Param("user") User user, @Param("itemType") String itemType, @Param("itemId") Long itemId);

    List<ReviewItem> findByItemTypeAndItemId(String itemType, Long itemId);

    @Query("SELECT COUNT(r) FROM ReviewItem r WHERE r.user = :user AND r.itemType = :itemType " +
            "AND r.nextReviewDate <= :today AND r.deleted = false")
    long countTodayRemaining(@Param("user") User user, @Param("itemType") String itemType, @Param("today") LocalDate today);

    @Query("SELECT r FROM ReviewItem r WHERE r.user = :user AND r.itemType = :itemType " +
            "AND r.nextReviewDate <= :today AND r.deleted = false " +
            "AND r.id NOT IN :excludeIds " +
            "ORDER BY CASE WHEN r.lastResult = 'HARD' THEN 0 ELSE 1 END, " +
            "r.lastReviewedAt ASC NULLS FIRST, r.reviewCount ASC")
    List<ReviewItem> findTodayCards(@Param("user") User user,
                                    @Param("itemType") String itemType,
                                    @Param("today") LocalDate today,
                                    @Param("excludeIds") List<Long> excludeIds);

    @Query("SELECT r FROM ReviewItem r WHERE r.user = :user AND r.itemType = :itemType " +
            "AND r.nextReviewDate <= :today AND r.deleted = false " +
            "AND r.id NOT IN :excludeIds " +
            "ORDER BY CASE WHEN r.lastResult = 'HARD' THEN 0 ELSE 1 END, " +
            "r.lastReviewedAt ASC NULLS FIRST, r.reviewCount ASC")
    List<ReviewItem> findTodayCards(@Param("user") User user,
                                    @Param("itemType") String itemType,
                                    @Param("today") LocalDate today,
                                    @Param("excludeIds") List<Long> excludeIds,
                                    Pageable pageable);
}

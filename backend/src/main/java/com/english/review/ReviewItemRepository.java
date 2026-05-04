package com.english.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewItemRepository extends JpaRepository<ReviewItem, Long> {

    @Modifying
    @Query("UPDATE ReviewItem r SET r.deleted = true WHERE r.itemType = :itemType AND r.itemId = :itemId")
    void softDeleteByItemTypeAndItemId(@Param("itemType") String itemType, @Param("itemId") Long itemId);

    List<ReviewItem> findByItemTypeAndItemId(String itemType, Long itemId);
}

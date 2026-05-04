package com.english.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    @Query("SELECT DISTINCT CAST(r.createdAt AS LocalDate) FROM ReviewLog r ORDER BY CAST(r.createdAt AS LocalDate) DESC")
    List<LocalDate> findDistinctReviewDates();
}

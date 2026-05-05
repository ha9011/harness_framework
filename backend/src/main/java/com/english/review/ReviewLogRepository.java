package com.english.review;

import com.english.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    @Query("SELECT DISTINCT CAST(r.createdAt AS LocalDate) FROM ReviewLog r WHERE r.reviewItem.user = :user ORDER BY CAST(r.createdAt AS LocalDate) DESC")
    List<LocalDate> findDistinctReviewDates(@Param("user") User user);
}

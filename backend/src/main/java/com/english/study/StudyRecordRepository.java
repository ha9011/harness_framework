package com.english.study;

import com.english.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    Optional<StudyRecord> findByUserAndCreatedAt(User user, LocalDate date);

    @Query("SELECT COALESCE(MAX(s.dayNumber), 0) FROM StudyRecord s WHERE s.user = :user")
    Integer findMaxDayNumber(@Param("user") User user);

    List<StudyRecord> findTop5ByUserOrderByCreatedAtDesc(User user);

    Page<StudyRecord> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}

package com.english.study;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    Optional<StudyRecord> findByCreatedAt(LocalDate date);

    @Query("SELECT COALESCE(MAX(s.dayNumber), 0) FROM StudyRecord s")
    Integer findMaxDayNumber();
}

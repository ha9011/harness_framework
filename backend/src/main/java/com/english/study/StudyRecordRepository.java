package com.english.study;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

	Optional<StudyRecord> findByUserIdAndStudyDate(Long userId, LocalDate studyDate);

	long countByUserId(Long userId);
}

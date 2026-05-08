package com.english.generate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationHistoryRepository extends JpaRepository<GenerationHistory, Long> {

	Page<GenerationHistory> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}

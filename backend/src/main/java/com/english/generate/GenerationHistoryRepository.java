package com.english.generate;

import com.english.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationHistoryRepository extends JpaRepository<GenerationHistory, Long> {

    Page<GenerationHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}

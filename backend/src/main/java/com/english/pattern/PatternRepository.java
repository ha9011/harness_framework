package com.english.pattern;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternRepository extends JpaRepository<Pattern, Long> {

	boolean existsByUserIdAndTemplateIgnoreCaseAndDeletedFalse(Long userId, String template);

	Optional<Pattern> findByUserIdAndTemplateIgnoreCaseAndDeletedFalse(Long userId, String template);

	Page<Pattern> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);
}

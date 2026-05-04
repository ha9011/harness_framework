package com.english.pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatternRepository extends JpaRepository<Pattern, Long> {

    boolean existsByTemplateAndDeletedFalse(String template);

    Optional<Pattern> findByIdAndDeletedFalse(Long id);

    Page<Pattern> findByDeletedFalse(Pageable pageable);
}

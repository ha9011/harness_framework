package com.english.pattern;

import com.english.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatternRepository extends JpaRepository<Pattern, Long> {

    boolean existsByTemplateAndUserAndDeletedFalse(String template, User user);

    Optional<Pattern> findByIdAndUserAndDeletedFalse(Long id, User user);

    Page<Pattern> findByUserAndDeletedFalse(User user, Pageable pageable);

    long countByUserAndDeletedFalse(User user);
}

package com.english.pattern;

import com.english.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatternRepository extends JpaRepository<Pattern, Long> {

    boolean existsByTemplateAndUserAndDeletedFalse(String template, User user);

    Optional<Pattern> findByIdAndUserAndDeletedFalse(Long id, User user);

    Page<Pattern> findByUserAndDeletedFalse(User user, Pageable pageable);

    long countByUserAndDeletedFalse(User user);

    @Query("SELECT DISTINCT p FROM Pattern p LEFT JOIN FETCH p.examples WHERE p.id IN :ids AND p.user = :user AND p.deleted = false")
    List<Pattern> findByIdInWithExamples(@Param("ids") List<Long> ids, @Param("user") User user);
}

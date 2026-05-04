package com.english.word;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByDeletedFalse();

    Optional<Word> findByWordAndDeletedFalse(String word);

    Optional<Word> findByIdAndDeletedFalse(Long id);

    boolean existsByWordAndDeletedFalse(String word);

    long countByDeletedFalse();

    @Query("SELECT w FROM Word w WHERE w.deleted = false " +
            "AND (:search IS NULL OR LOWER(w.word) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(w.meaning) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:partOfSpeech IS NULL OR w.partOfSpeech = :partOfSpeech) " +
            "AND (:importantOnly = false OR w.isImportant = true)")
    Page<Word> findAllWithFilters(
            @Param("search") String search,
            @Param("partOfSpeech") String partOfSpeech,
            @Param("importantOnly") boolean importantOnly,
            Pageable pageable);
}

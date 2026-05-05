package com.english.word;

import com.english.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByUserAndDeletedFalse(User user);

    Optional<Word> findByIdAndUserAndDeletedFalse(Long id, User user);

    boolean existsByWordAndUserAndDeletedFalse(String word, User user);

    long countByUserAndDeletedFalse(User user);

    boolean existsByIdAndUser(Long id, User user);

    @Query("SELECT w FROM Word w WHERE w.user = :user AND w.deleted = false " +
            "AND (:search IS NULL OR LOWER(w.word) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR LOWER(w.meaning) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "AND (:partOfSpeech IS NULL OR w.partOfSpeech = :partOfSpeech) " +
            "AND (:importantOnly = false OR w.isImportant = true)")
    Page<Word> findAllWithFilters(
            @Param("user") User user,
            @Param("search") String search,
            @Param("partOfSpeech") String partOfSpeech,
            @Param("importantOnly") boolean importantOnly,
            Pageable pageable);
}

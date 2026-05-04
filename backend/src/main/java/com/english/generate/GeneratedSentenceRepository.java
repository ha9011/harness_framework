package com.english.generate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GeneratedSentenceRepository extends JpaRepository<GeneratedSentence, Long> {

    @Query("SELECT gs FROM GeneratedSentence gs JOIN gs.sentenceWords sw WHERE sw.wordId = :wordId")
    List<GeneratedSentence> findByWordId(@Param("wordId") Long wordId);
}

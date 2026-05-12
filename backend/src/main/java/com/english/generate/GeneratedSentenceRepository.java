package com.english.generate;

import com.english.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GeneratedSentenceRepository extends JpaRepository<GeneratedSentence, Long> {

    @Query("SELECT gs FROM GeneratedSentence gs JOIN gs.sentenceWords sw WHERE sw.wordId = :wordId")
    List<GeneratedSentence> findByWordId(@Param("wordId") Long wordId);

    long countByUser(User user);

    @Query("SELECT DISTINCT gs FROM GeneratedSentence gs LEFT JOIN FETCH gs.situations WHERE gs.id IN :ids")
    List<GeneratedSentence> findByIdInWithSituations(@Param("ids") List<Long> ids);

    @Query("SELECT sw.wordId, gs FROM GeneratedSentence gs JOIN gs.sentenceWords sw WHERE sw.wordId IN :wordIds")
    List<Object[]> findByWordIdInWithMapping(@Param("wordIds") List<Long> wordIds);
}

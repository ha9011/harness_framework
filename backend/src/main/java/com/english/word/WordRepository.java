package com.english.word;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WordRepository extends JpaRepository<Word, Long>, JpaSpecificationExecutor<Word> {

	boolean existsByUserIdAndWordIgnoreCaseAndDeletedFalse(Long userId, String word);

	Optional<Word> findByUserIdAndWordIgnoreCaseAndDeletedFalse(Long userId, String word);

	List<Word> findByUserIdAndDeletedFalse(Long userId);
}

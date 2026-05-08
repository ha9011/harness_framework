package com.english.pattern;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternExampleRepository extends JpaRepository<PatternExample, Long> {

	List<PatternExample> findByPatternIdOrderBySortOrderAsc(Long patternId);

	void deleteByPatternId(Long patternId);
}

package com.english.generate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;

public record PatternGenerateRequest(
		@NotNull @Positive Long patternId,
		@NotBlank String level,
		@NotNull Integer count
) {

	private static final Set<Integer> PATTERN_GENERATION_COUNTS = Set.of(10, 20, 30);

	String requiredLevel() {
		return GenerateRequest.requireLevel(level);
	}

	int patternCount() {
		return GenerateRequest.requireAllowedCount(count, PATTERN_GENERATION_COUNTS, "패턴 생성 개수는 10, 20, 30만 가능합니다");
	}
}

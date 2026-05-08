package com.english.generate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;

public record WordGenerateRequest(
		@NotNull @Positive Long wordId,
		@NotBlank String level,
		@NotNull Integer count
) {

	private static final Set<Integer> DETAIL_GENERATION_COUNTS = Set.of(5, 10);

	String requiredLevel() {
		return GenerateRequest.requireLevel(level);
	}

	int detailCount() {
		return GenerateRequest.requireAllowedCount(count, DETAIL_GENERATION_COUNTS, "단어 상세 생성 개수는 5, 10만 가능합니다");
	}
}

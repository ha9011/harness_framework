package com.english.generate;

import java.time.Instant;

public record GenerationHistoryResponse(
		Long id,
		String level,
		int requestedCount,
		int actualCount,
		Instant createdAt
) {

	public static GenerationHistoryResponse from(GenerationHistory history) {
		return new GenerationHistoryResponse(
				history.getId(),
				history.getLevel(),
				history.getRequestedCount(),
				history.getActualCount(),
				history.getCreatedAt());
	}
}

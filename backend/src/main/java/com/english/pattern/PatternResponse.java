package com.english.pattern;

import java.time.Instant;
import java.util.List;

public record PatternResponse(
		Long id,
		String template,
		String description,
		List<PatternExampleResponse> examples,
		Instant createdAt,
		Instant updatedAt
) {

	public PatternResponse {
		examples = List.copyOf(examples);
	}

	public static PatternResponse from(Pattern pattern, List<PatternExample> examples) {
		return new PatternResponse(
				pattern.getId(),
				pattern.getTemplate(),
				pattern.getDescription(),
				examples.stream()
						.map(PatternExampleResponse::from)
						.toList(),
				pattern.getCreatedAt(),
				pattern.getUpdatedAt());
	}
}

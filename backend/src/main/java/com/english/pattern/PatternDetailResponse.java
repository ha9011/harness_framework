package com.english.pattern;

import java.time.Instant;
import java.util.List;

public record PatternDetailResponse(
		Long id,
		String template,
		String description,
		List<PatternExampleResponse> examples,
		Instant createdAt,
		Instant updatedAt,
		List<PatternGeneratedSentenceResponse> generatedSentences
) {

	public PatternDetailResponse {
		examples = List.copyOf(examples);
		generatedSentences = List.copyOf(generatedSentences);
	}

	public static PatternDetailResponse from(PatternResponse pattern) {
		return new PatternDetailResponse(
				pattern.id(),
				pattern.template(),
				pattern.description(),
				pattern.examples(),
				pattern.createdAt(),
				pattern.updatedAt(),
				List.of());
	}
}

package com.english.generate;

import java.util.List;

public record GeminiSentenceGenerationRequest(
		String level,
		int count,
		List<GeminiSentenceWordCandidate> words,
		List<GeminiSentencePatternCandidate> patterns
) {

	public GeminiSentenceGenerationRequest {
		level = GeminiValidation.requireText(level, "level");
		if (count <= 0) {
			throw new IllegalArgumentException("count must be positive");
		}
		words = GeminiValidation.requireNonEmptyList(words, "words");
		patterns = GeminiValidation.copyList(patterns);
	}
}

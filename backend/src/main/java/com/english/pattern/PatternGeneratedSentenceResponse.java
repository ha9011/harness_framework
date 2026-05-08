package com.english.pattern;

import java.time.Instant;

public record PatternGeneratedSentenceResponse(
		Long id,
		String sentence,
		String translation,
		String level,
		Instant createdAt
) {
}

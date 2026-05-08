package com.english.word;

import java.time.Instant;

public record WordGeneratedSentenceResponse(
		Long id,
		String sentence,
		String translation,
		String level,
		Instant createdAt
) {
}

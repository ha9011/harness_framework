package com.english.generate;

import java.util.List;

public record GeneratedSentenceResponse(
		Long id,
		String sentence,
		String translation,
		List<String> situations,
		String level,
		GeneratedSentencePatternResponse pattern,
		List<GeneratedSentenceWordResponse> words
) {

	public GeneratedSentenceResponse {
		situations = List.copyOf(situations);
		words = List.copyOf(words);
	}
}

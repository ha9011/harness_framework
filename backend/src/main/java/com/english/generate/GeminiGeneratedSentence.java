package com.english.generate;

import java.util.List;

public record GeminiGeneratedSentence(
		String sentence,
		String translation,
		Long patternId,
		List<Long> wordIds,
		List<String> situations
) {

	public GeminiGeneratedSentence {
		sentence = GeminiValidation.requireText(sentence, "sentence");
		translation = GeminiValidation.requireText(translation, "translation");
		wordIds = GeminiValidation.requireNonEmptyList(wordIds, "wordIds");
		wordIds.forEach(wordId -> GeminiValidation.requireId(wordId, "word id"));
		situations = GeminiValidation.requireNonEmptyList(situations, "situations");
		if (situations.size() != 5) {
			throw new IllegalArgumentException("situations must contain exactly 5 items");
		}
		situations.forEach(situation -> GeminiValidation.requireText(situation, "situation"));
	}
}

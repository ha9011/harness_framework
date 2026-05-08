package com.english.generate;

public record GeminiSentenceWordCandidate(Long id, String word, String meaning) {

	public GeminiSentenceWordCandidate {
		id = GeminiValidation.requireId(id, "word id");
		word = GeminiValidation.requireText(word, "word");
		meaning = GeminiValidation.requireText(meaning, "meaning");
	}
}

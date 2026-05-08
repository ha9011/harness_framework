package com.english.generate;

public record GeminiWordInput(Long id, String word, String meaning) {

	public GeminiWordInput {
		id = GeminiValidation.requireId(id, "word id");
		word = GeminiValidation.requireText(word, "word");
		meaning = GeminiValidation.requireText(meaning, "meaning");
	}
}

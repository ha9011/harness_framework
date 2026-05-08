package com.english.generate;

public record GeminiExtractedWord(String word, String meaning) {

	public GeminiExtractedWord {
		word = GeminiValidation.requireText(word, "word");
		meaning = GeminiValidation.requireText(meaning, "meaning");
	}
}

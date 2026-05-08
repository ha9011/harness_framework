package com.english.generate;

public record GeminiExtractedPatternExample(String sentence, String translation) {

	public GeminiExtractedPatternExample {
		sentence = GeminiValidation.requireText(sentence, "sentence");
		translation = GeminiValidation.requireText(translation, "translation");
	}
}

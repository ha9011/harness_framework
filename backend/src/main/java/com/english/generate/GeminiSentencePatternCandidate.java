package com.english.generate;

public record GeminiSentencePatternCandidate(Long id, String template, String description) {

	public GeminiSentencePatternCandidate {
		id = GeminiValidation.requireId(id, "pattern id");
		template = GeminiValidation.requireText(template, "template");
		description = GeminiValidation.requireText(description, "description");
	}
}

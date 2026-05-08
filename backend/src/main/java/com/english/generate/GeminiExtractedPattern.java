package com.english.generate;

import java.util.List;

public record GeminiExtractedPattern(
		String template,
		String description,
		List<GeminiExtractedPatternExample> examples
) {

	public GeminiExtractedPattern {
		template = GeminiValidation.requireText(template, "template");
		description = GeminiValidation.requireText(description, "description");
		examples = GeminiValidation.copyList(examples);
	}
}

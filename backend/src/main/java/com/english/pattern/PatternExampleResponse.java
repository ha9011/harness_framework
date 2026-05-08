package com.english.pattern;

public record PatternExampleResponse(
		Long id,
		int sortOrder,
		String sentence,
		String translation
) {

	public static PatternExampleResponse from(PatternExample example) {
		return new PatternExampleResponse(
				example.getId(),
				example.getSortOrder(),
				example.getSentence(),
				example.getTranslation());
	}
}

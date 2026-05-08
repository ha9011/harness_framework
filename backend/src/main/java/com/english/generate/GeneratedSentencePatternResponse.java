package com.english.generate;

import com.english.pattern.Pattern;

public record GeneratedSentencePatternResponse(Long id, String template) {

	public static GeneratedSentencePatternResponse from(Pattern pattern) {
		return new GeneratedSentencePatternResponse(pattern.getId(), pattern.getTemplate());
	}
}

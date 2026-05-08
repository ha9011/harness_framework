package com.english.word;

public record WordSearchCondition(
		String search,
		String partOfSpeech,
		Boolean importantOnly
) {
}

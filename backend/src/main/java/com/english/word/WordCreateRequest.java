package com.english.word;

public record WordCreateRequest(
		String word,
		String meaning,
		String partOfSpeech,
		String pronunciation,
		String synonyms,
		String tip
) {

	public WordCreateRequest(String word, String meaning) {
		this(word, meaning, null, null, null, null);
	}
}

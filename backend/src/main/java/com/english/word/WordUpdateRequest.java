package com.english.word;

public record WordUpdateRequest(
		String word,
		String meaning,
		String partOfSpeech,
		String pronunciation,
		String synonyms,
		String tip
) {

	public WordUpdateRequest(String word, String meaning) {
		this(word, meaning, null, null, null, null);
	}
}

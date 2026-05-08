package com.english.word;

import java.time.Instant;

public record WordResponse(
		Long id,
		String word,
		String meaning,
		String partOfSpeech,
		String pronunciation,
		String synonyms,
		String tip,
		boolean important,
		Instant createdAt,
		Instant updatedAt
) {

	public static WordResponse from(Word word) {
		return new WordResponse(
				word.getId(),
				word.getWord(),
				word.getMeaning(),
				word.getPartOfSpeech(),
				word.getPronunciation(),
				word.getSynonyms(),
				word.getTip(),
				word.isImportant(),
				word.getCreatedAt(),
				word.getUpdatedAt());
	}
}

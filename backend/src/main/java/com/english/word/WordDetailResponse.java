package com.english.word;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record WordDetailResponse(
		Long id,
		String word,
		String meaning,
		String partOfSpeech,
		String pronunciation,
		String synonyms,
		String tip,
		@JsonProperty("isImportant")
		boolean important,
		Instant createdAt,
		Instant updatedAt,
		List<WordGeneratedSentenceResponse> generatedSentences
) {

	public WordDetailResponse {
		generatedSentences = List.copyOf(generatedSentences);
	}

	public static WordDetailResponse from(WordResponse word) {
		return new WordDetailResponse(
				word.id(),
				word.word(),
				word.meaning(),
				word.partOfSpeech(),
				word.pronunciation(),
				word.synonyms(),
				word.tip(),
				word.important(),
				word.createdAt(),
				word.updatedAt(),
				List.of());
	}
}

package com.english.word;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordUpdateRequest(
		@NotBlank
		@Size(max = 200)
		String word,
		@NotBlank
		@Size(max = 500)
		String meaning,
		@Size(max = 50)
		String partOfSpeech,
		@Size(max = 200)
		String pronunciation,
		@Size(max = 500)
		String synonyms,
		@Size(max = 500)
		String tip
) {

	public WordUpdateRequest(String word, String meaning) {
		this(word, meaning, null, null, null, null);
	}
}

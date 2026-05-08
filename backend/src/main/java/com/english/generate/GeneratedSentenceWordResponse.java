package com.english.generate;

import com.english.word.Word;

public record GeneratedSentenceWordResponse(Long id, String word) {

	public static GeneratedSentenceWordResponse from(Word word) {
		return new GeneratedSentenceWordResponse(word.getId(), word.getWord());
	}
}

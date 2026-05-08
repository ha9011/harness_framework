package com.english.review;

import java.util.List;

public record SentenceRecognitionReviewBack(
		String translation,
		String pattern,
		List<String> words
) implements ReviewCardBack {
}

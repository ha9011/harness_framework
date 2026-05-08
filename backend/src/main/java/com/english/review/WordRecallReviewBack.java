package com.english.review;

public record WordRecallReviewBack(
		String word,
		String pronunciation,
		String tip
) implements ReviewCardBack {
}

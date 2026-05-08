package com.english.review;

import java.util.List;

public record WordRecognitionReviewBack(
		String meaning,
		String pronunciation,
		String tip,
		List<String> examples
) implements ReviewCardBack {
}

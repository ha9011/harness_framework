package com.english.review;

import java.util.List;

public record PatternRecognitionReviewBack(
		String description,
		List<ReviewPatternExampleResponse> examples
) implements ReviewCardBack {
}

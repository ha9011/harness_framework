package com.english.review;

import java.util.List;

public record PatternRecallReviewBack(
		String template,
		List<ReviewPatternExampleResponse> examples
) implements ReviewCardBack {
}

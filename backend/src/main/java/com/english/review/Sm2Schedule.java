package com.english.review;

import java.time.LocalDate;

public record Sm2Schedule(
		LocalDate nextReviewDate,
		int intervalDays,
		double easeFactor
) {
}

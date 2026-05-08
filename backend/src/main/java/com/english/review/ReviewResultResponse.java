package com.english.review;

import java.time.LocalDate;

public record ReviewResultResponse(
		LocalDate nextReviewDate,
		int intervalDays
) {
}

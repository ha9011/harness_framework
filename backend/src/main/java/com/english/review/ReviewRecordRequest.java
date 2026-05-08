package com.english.review;

import jakarta.validation.constraints.NotNull;

public record ReviewRecordRequest(
		@NotNull
		ReviewResult result
) {
}

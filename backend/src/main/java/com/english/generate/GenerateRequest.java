package com.english.generate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record GenerateRequest(
		@NotBlank String level,
		@NotNull Integer count
) {

	private static final Set<String> ALLOWED_LEVELS = Set.of("유아", "초등", "중등", "고등");
	private static final Set<Integer> FULL_GENERATION_COUNTS = Set.of(10, 20, 30);

	String requiredLevel() {
		return requireLevel(level);
	}

	int fullCount() {
		return requireAllowedCount(count, FULL_GENERATION_COUNTS, "전체 생성 개수는 10, 20, 30만 가능합니다");
	}

	static String requireLevel(String level) {
		if (level == null || level.isBlank()) {
			throw GenerateException.badRequest("난이도는 필수입니다");
		}
		String normalized = level.trim();
		if (!ALLOWED_LEVELS.contains(normalized)) {
			throw GenerateException.badRequest("지원하지 않는 난이도입니다");
		}
		return normalized;
	}

	static int requireAllowedCount(Integer count, Set<Integer> allowedCounts, String message) {
		if (count == null || !allowedCounts.contains(count)) {
			throw GenerateException.badRequest(message);
		}
		return count;
	}
}

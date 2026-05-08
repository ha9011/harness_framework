package com.english.generate;

import java.util.List;

final class GeminiValidation {

	private GeminiValidation() {
	}

	static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value.trim();
	}

	static Long requireId(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value;
	}

	static <T> List<T> requireNonEmptyList(List<T> values, String fieldName) {
		if (values == null || values.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return List.copyOf(values);
	}

	static <T> List<T> copyList(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}

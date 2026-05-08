package com.english.pattern;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatternExampleRequest(
		@NotBlank
		@Size(max = 500)
		String sentence,
		@NotBlank
		@Size(max = 500)
		String translation
) {
}

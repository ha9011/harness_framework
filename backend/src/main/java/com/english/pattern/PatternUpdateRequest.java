package com.english.pattern;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PatternUpdateRequest(
		@NotBlank
		@Size(max = 255)
		String template,
		@NotBlank
		@Size(max = 500)
		String description,
		List<@Valid PatternExampleRequest> examples
) {

	public PatternUpdateRequest {
		examples = examples == null ? List.of() : List.copyOf(examples);
	}

	public PatternUpdateRequest(String template, String description) {
		this(template, description, List.of());
	}
}

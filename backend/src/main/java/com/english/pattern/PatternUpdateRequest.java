package com.english.pattern;

import java.util.List;

public record PatternUpdateRequest(
		String template,
		String description,
		List<PatternExampleRequest> examples
) {

	public PatternUpdateRequest {
		examples = examples == null ? List.of() : List.copyOf(examples);
	}

	public PatternUpdateRequest(String template, String description) {
		this(template, description, List.of());
	}
}

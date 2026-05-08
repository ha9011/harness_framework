package com.english.pattern;

import java.util.List;

public record PatternCreateRequest(
		String template,
		String description,
		List<PatternExampleRequest> examples
) {

	public PatternCreateRequest {
		examples = examples == null ? List.of() : List.copyOf(examples);
	}

	public PatternCreateRequest(String template, String description) {
		this(template, description, List.of());
	}
}

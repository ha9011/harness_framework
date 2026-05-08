package com.english.generate;

import java.util.List;

public record GenerateResponse(
		Long generationId,
		List<GeneratedSentenceResponse> sentences
) {

	public GenerateResponse {
		sentences = List.copyOf(sentences);
	}
}

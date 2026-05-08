package com.english.generate;

public record GeminiWordEnrichment(
		Long id,
		String partOfSpeech,
		String pronunciation,
		String synonyms,
		String tip
) {

	public GeminiWordEnrichment {
		id = GeminiValidation.requireId(id, "word enrichment id");
		partOfSpeech = GeminiValidation.requireText(partOfSpeech, "partOfSpeech");
		pronunciation = GeminiValidation.requireText(pronunciation, "pronunciation");
		synonyms = GeminiValidation.requireText(synonyms, "synonyms");
		tip = GeminiValidation.requireText(tip, "tip");
	}
}

package com.english.word;

import java.util.List;

public record WordBulkSaveResponse(
		List<WordResponse> saved,
		List<WordBulkSkippedItem> skipped,
		List<WordBulkEnrichmentFailedItem> enrichmentFailed
) {

	public WordBulkSaveResponse {
		saved = List.copyOf(saved);
		skipped = List.copyOf(skipped);
		enrichmentFailed = List.copyOf(enrichmentFailed);
	}

	public static WordBulkSaveResponse from(WordBulkSaveResult result) {
		return new WordBulkSaveResponse(result.saved(), result.skipped(), List.of());
	}
}

package com.english.word;

public record WordBulkEnrichmentFailedItem(
		Long id,
		String word,
		String meaning,
		String reason
) {
}

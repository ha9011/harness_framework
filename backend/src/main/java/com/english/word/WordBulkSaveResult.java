package com.english.word;

import java.util.List;

public record WordBulkSaveResult(
		List<WordResponse> saved,
		List<WordBulkSkippedItem> skipped
) {

	public WordBulkSaveResult {
		saved = List.copyOf(saved);
		skipped = List.copyOf(skipped);
	}
}

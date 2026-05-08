package com.english.study;

import java.time.LocalDate;
import java.util.List;

public record StudyRecordResponse(
		Long id,
		LocalDate studyDate,
		int dayNumber,
		List<StudyRecordItemResponse> items
) {

	public StudyRecordResponse {
		items = List.copyOf(items);
	}
}

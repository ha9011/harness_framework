package com.english.dashboard;

import com.english.study.StudyRecordResponse;
import java.util.List;

public record DashboardResponse(
		long wordCount,
		long patternCount,
		long sentenceCount,
		int streak,
		TodayReviewRemainingResponse todayReviewRemaining,
		List<StudyRecordResponse> recentStudyRecords
) {

	public DashboardResponse {
		recentStudyRecords = List.copyOf(recentStudyRecords);
	}
}

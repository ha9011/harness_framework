package com.english.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardResponse {
    private long wordCount;
    private long patternCount;
    private long sentenceCount;
    private int streak;
    private ReviewRemaining todayReviewRemaining;
    private List<StudyRecordDto> recentStudyRecords;

    @Getter
    @AllArgsConstructor
    public static class ReviewRemaining {
        private long word;
        private long pattern;
        private long sentence;
    }

    @Getter
    @AllArgsConstructor
    public static class StudyRecordDto {
        private Long id;
        private int dayNumber;
        private String createdAt;
        private int wordCount;
        private int patternCount;
    }
}

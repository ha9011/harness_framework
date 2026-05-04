package com.english.study;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyRecordResponse {
    private Long id;
    private int dayNumber;
    private String createdAt;
    private int wordCount;
    private int patternCount;
}

package com.english.review;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResultResponse {
    private Long id;
    private String result;
    private int intervalDays;
    private LocalDate nextReviewDate;
    private double easeFactor;
    private int reviewCount;
}

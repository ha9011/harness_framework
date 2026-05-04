package com.english.review;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_item_id", nullable = false)
    private Long reviewItemId;

    @Column(nullable = false)
    private String result;

    @Column(name = "previous_interval", nullable = false)
    private int previousInterval;

    @Column(name = "new_interval", nullable = false)
    private int newInterval;

    @Column(name = "previous_ease_factor", nullable = false)
    private double previousEaseFactor;

    @Column(name = "new_ease_factor", nullable = false)
    private double newEaseFactor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ReviewLog(Long reviewItemId, String result,
                     int previousInterval, int newInterval,
                     double previousEaseFactor, double newEaseFactor) {
        this.reviewItemId = reviewItemId;
        this.result = result;
        this.previousInterval = previousInterval;
        this.newInterval = newInterval;
        this.previousEaseFactor = previousEaseFactor;
        this.newEaseFactor = newEaseFactor;
        this.createdAt = LocalDateTime.now();
    }
}

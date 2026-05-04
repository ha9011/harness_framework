package com.english.review;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"item_type", "item_id", "direction"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private String direction;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    @Column(name = "ease_factor", nullable = false)
    private double easeFactor = 2.5;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "last_result")
    private String lastResult;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    public ReviewItem(String itemType, Long itemId, String direction) {
        this.itemType = itemType;
        this.itemId = itemId;
        this.direction = direction;
        this.nextReviewDate = LocalDate.now();
    }

    /**
     * SM-2 기반 커스텀 간격 반복 적용
     * @return ReviewLog 기록용 데이터
     */
    public ReviewLog applyResult(String result) {
        int previousInterval = this.intervalDays;
        double previousEaseFactor = this.easeFactor;

        switch (result) {
            case "EASY":
                this.intervalDays = (int) Math.round(this.intervalDays * this.easeFactor * 1.3);
                this.easeFactor += 0.15;
                break;
            case "MEDIUM":
                this.intervalDays = (int) Math.round(this.intervalDays * this.easeFactor);
                break;
            case "HARD":
                this.intervalDays = 1;
                this.easeFactor = Math.max(1.3, this.easeFactor - 0.2);
                break;
            default:
                throw new IllegalArgumentException("잘못된 결과: " + result);
        }

        this.nextReviewDate = LocalDate.now().plusDays(this.intervalDays);
        this.reviewCount++;
        this.lastResult = result;
        this.lastReviewedAt = LocalDateTime.now();

        return new ReviewLog(this.id, result, previousInterval, this.intervalDays,
                previousEaseFactor, this.easeFactor);
    }
}

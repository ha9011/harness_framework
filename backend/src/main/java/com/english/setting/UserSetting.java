package com.english.setting;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "daily_review_count", nullable = false)
    private Integer dailyReviewCount = 10;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserSetting(Integer dailyReviewCount) {
        this.dailyReviewCount = dailyReviewCount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDailyReviewCount(Integer dailyReviewCount) {
        this.dailyReviewCount = dailyReviewCount;
        this.updatedAt = LocalDateTime.now();
    }
}

package com.english.generate;

import com.english.auth.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "generation_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String level;

    @Column(name = "requested_count", nullable = false)
    private int requestedCount;

    @Column(name = "actual_count", nullable = false)
    private int actualCount;

    @Column(name = "word_id")
    private Long wordId;

    @Column(name = "pattern_id")
    private Long patternId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GenerationHistory(User user, String level, int requestedCount, int actualCount, Long wordId, Long patternId) {
        this.user = user;
        this.level = level;
        this.requestedCount = requestedCount;
        this.actualCount = actualCount;
        this.wordId = wordId;
        this.patternId = patternId;
        this.createdAt = LocalDateTime.now();
    }
}

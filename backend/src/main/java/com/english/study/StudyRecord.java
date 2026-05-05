package com.english.study;

import com.english.auth.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "study_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private LocalDate createdAt;

    public StudyRecord(User user, Integer dayNumber, LocalDate createdAt) {
        this.user = user;
        this.dayNumber = dayNumber;
        this.createdAt = createdAt;
    }
}

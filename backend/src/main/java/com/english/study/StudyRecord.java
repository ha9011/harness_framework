package com.english.study;

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

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private LocalDate createdAt;

    public StudyRecord(Integer dayNumber, LocalDate createdAt) {
        this.dayNumber = dayNumber;
        this.createdAt = createdAt;
    }
}

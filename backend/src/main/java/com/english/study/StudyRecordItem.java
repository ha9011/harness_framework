package com.english.study;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "study_record_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"study_record_id", "item_type", "item_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyRecordItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_record_id", nullable = false)
    private Long studyRecordId;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    public StudyRecordItem(Long studyRecordId, String itemType, Long itemId) {
        this.studyRecordId = studyRecordId;
        this.itemType = itemType;
        this.itemId = itemId;
    }
}

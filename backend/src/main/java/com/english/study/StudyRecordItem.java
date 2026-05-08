package com.english.study;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_record_items")
public class StudyRecordItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "study_record_id", nullable = false)
	private StudyRecord studyRecord;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_type", nullable = false, length = 10)
	private StudyItemType itemType;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	protected StudyRecordItem() {
	}

	public StudyRecordItem(StudyRecord studyRecord, StudyItemType itemType, Long itemId) {
		this.studyRecord = studyRecord;
		this.itemType = itemType;
		this.itemId = itemId;
	}

	public Long getId() {
		return id;
	}

	public StudyRecord getStudyRecord() {
		return studyRecord;
	}

	public StudyItemType getItemType() {
		return itemType;
	}

	public Long getItemId() {
		return itemId;
	}
}

package com.english.study;

import com.english.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "study_records")
public class StudyRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "study_date", nullable = false)
	private LocalDate studyDate;

	@Column(name = "day_number", nullable = false)
	private int dayNumber;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected StudyRecord() {
	}

	public StudyRecord(User user, LocalDate studyDate, int dayNumber) {
		this.user = user;
		this.studyDate = studyDate;
		this.dayNumber = dayNumber;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public LocalDate getStudyDate() {
		return studyDate;
	}

	public int getDayNumber() {
		return dayNumber;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

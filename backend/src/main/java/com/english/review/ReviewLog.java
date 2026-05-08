package com.english.review;

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
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "review_logs")
public class ReviewLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_item_id", nullable = false)
	private ReviewItem reviewItem;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ReviewResult result;

	@CreationTimestamp
	@Column(name = "reviewed_at", nullable = false, updatable = false)
	private Instant reviewedAt;

	protected ReviewLog() {
	}

	public ReviewLog(ReviewItem reviewItem, ReviewResult result) {
		this.reviewItem = reviewItem;
		this.result = result;
	}

	public Long getId() {
		return id;
	}

	public ReviewItem getReviewItem() {
		return reviewItem;
	}

	public ReviewResult getResult() {
		return result;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}
}

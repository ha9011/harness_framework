package com.english.review;

import com.english.auth.User;
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
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "review_items")
public class ReviewItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_type", nullable = false, length = 20)
	private ReviewItemType itemType;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private ReviewDirection direction = ReviewDirection.RECOGNITION;

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

	@Enumerated(EnumType.STRING)
	@Column(name = "last_result", length = 10)
	private ReviewResult lastResult;

	@Column(name = "last_reviewed_at")
	private Instant lastReviewedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ReviewItem() {
	}

	public ReviewItem(
			User user,
			ReviewItemType itemType,
			Long itemId,
			ReviewDirection direction,
			LocalDate nextReviewDate
	) {
		this.user = user;
		this.itemType = itemType;
		this.itemId = itemId;
		this.direction = direction;
		this.nextReviewDate = nextReviewDate;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public ReviewItemType getItemType() {
		return itemType;
	}

	public Long getItemId() {
		return itemId;
	}

	public ReviewDirection getDirection() {
		return direction;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public LocalDate getNextReviewDate() {
		return nextReviewDate;
	}

	public int getIntervalDays() {
		return intervalDays;
	}

	public double getEaseFactor() {
		return easeFactor;
	}

	public int getReviewCount() {
		return reviewCount;
	}

	public ReviewResult getLastResult() {
		return lastResult;
	}

	public Instant getLastReviewedAt() {
		return lastReviewedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

package com.english.setting;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_settings")
public class UserSetting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "daily_review_count", nullable = false)
	private int dailyReviewCount = 10;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserSetting() {
	}

	public UserSetting(User user, int dailyReviewCount) {
		this.user = user;
		this.dailyReviewCount = dailyReviewCount;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public int getDailyReviewCount() {
		return dailyReviewCount;
	}

	public void updateDailyReviewCount(int dailyReviewCount) {
		this.dailyReviewCount = dailyReviewCount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}

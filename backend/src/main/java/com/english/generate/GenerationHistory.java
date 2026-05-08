package com.english.generate;

import com.english.auth.User;
import com.english.pattern.Pattern;
import com.english.word.Word;
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

@Entity
@Table(name = "generation_history")
public class GenerationHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 10)
	private String level;

	@Column(name = "requested_count", nullable = false)
	private int requestedCount;

	@Column(name = "actual_count", nullable = false)
	private int actualCount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "word_id")
	private Word word;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pattern_id")
	private Pattern pattern;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected GenerationHistory() {
	}

	public GenerationHistory(
			User user,
			String level,
			int requestedCount,
			int actualCount,
			Word word,
			Pattern pattern
	) {
		this.user = user;
		this.level = level;
		this.requestedCount = requestedCount;
		this.actualCount = actualCount;
		this.word = word;
		this.pattern = pattern;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getLevel() {
		return level;
	}

	public int getRequestedCount() {
		return requestedCount;
	}

	public int getActualCount() {
		return actualCount;
	}

	public Word getWord() {
		return word;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

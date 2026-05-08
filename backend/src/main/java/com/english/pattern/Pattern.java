package com.english.pattern;

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
@Table(name = "patterns")
public class Pattern {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 255)
	private String template;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(nullable = false)
	private boolean deleted = false;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Pattern() {
	}

	public Pattern(User user, String template, String description) {
		this.user = user;
		this.template = template;
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getTemplate() {
		return template;
	}

	public String getDescription() {
		return description;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}

package com.english.generate;

import com.english.auth.User;
import com.english.pattern.Pattern;
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
@Table(name = "generated_sentences")
public class GeneratedSentence {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "generation_id", nullable = false)
	private GenerationHistory generationHistory;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pattern_id")
	private Pattern pattern;

	@Column(nullable = false, columnDefinition = "text")
	private String sentence;

	@Column(nullable = false, columnDefinition = "text")
	private String translation;

	@Column(nullable = false, length = 10)
	private String level;

	@Column(nullable = false)
	private boolean deleted = false;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected GeneratedSentence() {
	}

	public GeneratedSentence(
			User user,
			GenerationHistory generationHistory,
			Pattern pattern,
			String sentence,
			String translation,
			String level
	) {
		this.user = user;
		this.generationHistory = generationHistory;
		this.pattern = pattern;
		this.sentence = sentence;
		this.translation = translation;
		this.level = level;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public GenerationHistory getGenerationHistory() {
		return generationHistory;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public String getSentence() {
		return sentence;
	}

	public String getTranslation() {
		return translation;
	}

	public String getLevel() {
		return level;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

package com.english.word;

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
@Table(name = "words")
public class Word {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 200)
	private String word;

	@Column(nullable = false, length = 500)
	private String meaning;

	@Column(name = "part_of_speech", length = 50)
	private String partOfSpeech;

	@Column(length = 200)
	private String pronunciation;

	@Column(length = 500)
	private String synonyms;

	@Column(length = 500)
	private String tip;

	@Column(name = "is_important", nullable = false)
	private boolean important = false;

	@Column(nullable = false)
	private boolean deleted = false;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Word() {
	}

	public Word(User user, String word, String meaning) {
		this.user = user;
		this.word = word;
		this.meaning = meaning;
	}

	public void update(
			String word,
			String meaning,
			String partOfSpeech,
			String pronunciation,
			String synonyms,
			String tip
	) {
		this.word = word;
		this.meaning = meaning;
		this.partOfSpeech = partOfSpeech;
		this.pronunciation = pronunciation;
		this.synonyms = synonyms;
		this.tip = tip;
	}

	public void toggleImportant() {
		this.important = !this.important;
	}

	public void softDelete() {
		this.deleted = true;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getWord() {
		return word;
	}

	public String getMeaning() {
		return meaning;
	}

	public String getPartOfSpeech() {
		return partOfSpeech;
	}

	public String getPronunciation() {
		return pronunciation;
	}

	public String getSynonyms() {
		return synonyms;
	}

	public String getTip() {
		return tip;
	}

	public boolean isImportant() {
		return important;
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

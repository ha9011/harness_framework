package com.english.pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pattern_examples")
public class PatternExample {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pattern_id", nullable = false)
	private Pattern pattern;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false, length = 500)
	private String sentence;

	@Column(nullable = false, length = 500)
	private String translation;

	protected PatternExample() {
	}

	public PatternExample(Pattern pattern, int sortOrder, String sentence, String translation) {
		this.pattern = pattern;
		this.sortOrder = sortOrder;
		this.sentence = sentence;
		this.translation = translation;
	}

	public Long getId() {
		return id;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public String getSentence() {
		return sentence;
	}

	public String getTranslation() {
		return translation;
	}
}

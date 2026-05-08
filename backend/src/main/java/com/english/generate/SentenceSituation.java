package com.english.generate;

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
@Table(name = "sentence_situations")
public class SentenceSituation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sentence_id", nullable = false)
	private GeneratedSentence sentence;

	@Column(nullable = false, columnDefinition = "text")
	private String situation;

	protected SentenceSituation() {
	}

	public SentenceSituation(GeneratedSentence sentence, String situation) {
		this.sentence = sentence;
		this.situation = situation;
	}

	public Long getId() {
		return id;
	}

	public GeneratedSentence getSentence() {
		return sentence;
	}

	public String getSituation() {
		return situation;
	}
}

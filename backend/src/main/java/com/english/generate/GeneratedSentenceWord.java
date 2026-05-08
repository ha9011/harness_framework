package com.english.generate;

import com.english.word.Word;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "generated_sentence_words")
public class GeneratedSentenceWord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sentence_id", nullable = false)
	private GeneratedSentence sentence;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "word_id", nullable = false)
	private Word word;

	protected GeneratedSentenceWord() {
	}

	public GeneratedSentenceWord(GeneratedSentence sentence, Word word) {
		this.sentence = sentence;
		this.word = word;
	}

	public Long getId() {
		return id;
	}

	public GeneratedSentence getSentence() {
		return sentence;
	}

	public Word getWord() {
		return word;
	}
}

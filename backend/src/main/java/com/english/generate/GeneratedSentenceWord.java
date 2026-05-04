package com.english.generate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "generated_sentence_words")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeneratedSentenceWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id", nullable = false)
    private GeneratedSentence sentence;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    public GeneratedSentenceWord(GeneratedSentence sentence, Long wordId) {
        this.sentence = sentence;
        this.wordId = wordId;
    }
}

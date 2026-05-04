package com.english.generate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sentence_situations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SentenceSituation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id", nullable = false)
    private GeneratedSentence sentence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String situation;

    public SentenceSituation(GeneratedSentence sentence, String situation) {
        this.sentence = sentence;
        this.situation = situation;
    }
}

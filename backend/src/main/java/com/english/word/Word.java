package com.english.word;

import com.english.auth.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "words")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private String meaning;

    @Column(name = "part_of_speech")
    private String partOfSpeech;

    private String pronunciation;

    private String synonyms;

    private String tip;

    @Column(name = "is_important", nullable = false)
    private boolean isImportant = false;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Word(User user, String word, String meaning) {
        this.user = user;
        this.word = word;
        this.meaning = meaning;
        this.createdAt = LocalDateTime.now();
    }

    public void enrich(String partOfSpeech, String pronunciation, String synonyms, String tip) {
        this.partOfSpeech = partOfSpeech;
        this.pronunciation = pronunciation;
        this.synonyms = synonyms;
        this.tip = tip;
    }

    public void toggleImportant() {
        this.isImportant = !this.isImportant;
    }

    public void softDelete() {
        this.deleted = true;
    }
}

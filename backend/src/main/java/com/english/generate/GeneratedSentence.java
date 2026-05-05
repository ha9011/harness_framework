package com.english.generate;

import com.english.auth.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "generated_sentences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeneratedSentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "english_sentence", nullable = false, columnDefinition = "TEXT")
    private String englishSentence;

    @Column(name = "korean_translation", nullable = false, columnDefinition = "TEXT")
    private String koreanTranslation;

    @Column(nullable = false)
    private String level;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "sentence", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SentenceSituation> situations = new ArrayList<>();

    @OneToMany(mappedBy = "sentence", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GeneratedSentenceWord> sentenceWords = new ArrayList<>();

    public GeneratedSentence(User user, String englishSentence, String koreanTranslation, String level) {
        this.user = user;
        this.englishSentence = englishSentence;
        this.koreanTranslation = koreanTranslation;
        this.level = level;
        this.createdAt = LocalDateTime.now();
    }

    public void addSituation(String situation) {
        situations.add(new SentenceSituation(this, situation));
    }

    public void addSentenceWord(Long wordId) {
        sentenceWords.add(new GeneratedSentenceWord(this, wordId));
    }
}

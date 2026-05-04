package com.english.pattern;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pattern_examples")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatternExample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pattern_id", nullable = false)
    private Pattern pattern;

    @Column(nullable = false)
    private String sentence;

    private String translation;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public PatternExample(Pattern pattern, String sentence, String translation, int orderIndex) {
        this.pattern = pattern;
        this.sentence = sentence;
        this.translation = translation;
        this.orderIndex = orderIndex;
    }
}

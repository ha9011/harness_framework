package com.english.pattern;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patterns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String template;

    private String description;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "pattern", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<PatternExample> examples = new ArrayList<>();

    public Pattern(String template, String description) {
        this.template = template;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public void addExample(String sentence, String translation, int orderIndex) {
        examples.add(new PatternExample(this, sentence, translation, orderIndex));
    }

    public void softDelete() {
        this.deleted = true;
    }
}

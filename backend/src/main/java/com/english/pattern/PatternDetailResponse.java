package com.english.pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PatternDetailResponse {
    private Long id;
    private String template;
    private String description;
    private LocalDateTime createdAt;
    private List<ExampleResponse> examples;

    @Getter
    @AllArgsConstructor
    public static class ExampleResponse {
        private String sentence;
        private String translation;
        private int orderIndex;
    }

    public static PatternDetailResponse from(Pattern pattern) {
        List<ExampleResponse> examples = pattern.getExamples().stream()
                .map(e -> new ExampleResponse(e.getSentence(), e.getTranslation(), e.getOrderIndex()))
                .toList();

        return new PatternDetailResponse(
                pattern.getId(),
                pattern.getTemplate(),
                pattern.getDescription(),
                pattern.getCreatedAt(),
                examples
        );
    }
}

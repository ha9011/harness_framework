package com.english.pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PatternListResponse {
    private Long id;
    private String template;
    private String description;
    private int exampleCount;
    private LocalDateTime createdAt;

    public static PatternListResponse from(Pattern pattern) {
        return new PatternListResponse(
                pattern.getId(),
                pattern.getTemplate(),
                pattern.getDescription(),
                pattern.getExamples().size(),
                pattern.getCreatedAt()
        );
    }
}

package com.english.pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PatternResponse {
    private Long id;
    private String template;
    private String description;
    private LocalDateTime createdAt;

    public static PatternResponse from(Pattern pattern) {
        return new PatternResponse(
                pattern.getId(),
                pattern.getTemplate(),
                pattern.getDescription(),
                pattern.getCreatedAt()
        );
    }
}

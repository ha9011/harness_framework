package com.english.generate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GenerationHistoryResponse {

    private Long id;
    private String level;
    private int requestedCount;
    private int actualCount;
    private Long wordId;
    private Long patternId;
    private LocalDateTime createdAt;

    public static GenerationHistoryResponse from(GenerationHistory history) {
        return new GenerationHistoryResponse(
                history.getId(),
                history.getLevel(),
                history.getRequestedCount(),
                history.getActualCount(),
                history.getWordId(),
                history.getPatternId(),
                history.getCreatedAt()
        );
    }
}

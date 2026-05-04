package com.english.word;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class WordListResponse {
    private Long id;
    private String word;
    private String meaning;
    private String partOfSpeech;
    private boolean isImportant;
    private LocalDateTime createdAt;

    public static WordListResponse from(Word word) {
        return new WordListResponse(
                word.getId(),
                word.getWord(),
                word.getMeaning(),
                word.getPartOfSpeech(),
                word.isImportant(),
                word.getCreatedAt()
        );
    }
}

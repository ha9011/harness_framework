package com.english.word;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class WordResponse {
    private Long id;
    private String word;
    private String meaning;
    private String partOfSpeech;
    private String pronunciation;
    private String synonyms;
    private String tip;
    private boolean isImportant;
    private LocalDateTime createdAt;

    public static WordResponse from(Word word) {
        return new WordResponse(
                word.getId(),
                word.getWord(),
                word.getMeaning(),
                word.getPartOfSpeech(),
                word.getPronunciation(),
                word.getSynonyms(),
                word.getTip(),
                word.isImportant(),
                word.getCreatedAt()
        );
    }
}

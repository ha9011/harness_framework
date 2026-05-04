package com.english.word;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class WordDetailResponse {
    private Long id;
    private String word;
    private String meaning;
    private String partOfSpeech;
    private String pronunciation;
    private String synonyms;
    private String tip;
    private boolean isImportant;
    private LocalDateTime createdAt;
    private List<String> examples;

    public static WordDetailResponse from(Word word, List<String> examples) {
        return new WordDetailResponse(
                word.getId(),
                word.getWord(),
                word.getMeaning(),
                word.getPartOfSpeech(),
                word.getPronunciation(),
                word.getSynonyms(),
                word.getTip(),
                word.isImportant(),
                word.getCreatedAt(),
                examples
        );
    }
}

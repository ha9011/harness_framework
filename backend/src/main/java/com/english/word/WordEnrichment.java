package com.english.word;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WordEnrichment {
    private String partOfSpeech;
    private String pronunciation;
    private String synonyms;
    private String tip;
}

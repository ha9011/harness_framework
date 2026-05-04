package com.english.pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WordExtractResponse {
    private List<ExtractedWord> words;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedWord {
        private String word;
        private String meaning;
    }
}

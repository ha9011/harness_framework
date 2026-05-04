package com.english.pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PatternExtractResponse {
    private String template;
    private String description;
    private List<ExtractedExample> examples;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedExample {
        private String sentence;
        private String translation;
    }
}

package com.english.word;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BulkWordEnrichment {
    private List<Item> enrichments;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String word;
        private String partOfSpeech;
        private String pronunciation;
        private String synonyms;
        private String tip;
    }
}

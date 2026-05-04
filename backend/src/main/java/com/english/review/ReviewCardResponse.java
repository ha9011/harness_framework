package com.english.review;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCardResponse {
    private Long id;
    private String itemType;
    private String direction;
    private FrontContent front;
    private BackContent back;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrontContent {
        private String word;
        private String meaning;
        private String template;
        private String description;
        private String englishSentence;
        private String situation;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackContent {
        private String word;
        private String meaning;
        private String partOfSpeech;
        private String template;
        private String description;
        private String koreanTranslation;
        private List<ExampleDto> examples;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExampleDto {
        private String englishSentence;
        private String koreanTranslation;
    }
}

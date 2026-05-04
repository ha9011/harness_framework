package com.english.generate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponse {

    private Long generationId;
    private List<SentenceResponse> sentences;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentenceResponse {
        private Long id;
        private String englishSentence;
        private String koreanTranslation;
        private String level;
        private List<String> situations;

        public static SentenceResponse from(GeneratedSentence sentence) {
            List<String> situations = sentence.getSituations().stream()
                    .map(SentenceSituation::getSituation)
                    .toList();
            return new SentenceResponse(
                    sentence.getId(),
                    sentence.getEnglishSentence(),
                    sentence.getKoreanTranslation(),
                    sentence.getLevel(),
                    situations
            );
        }
    }
}

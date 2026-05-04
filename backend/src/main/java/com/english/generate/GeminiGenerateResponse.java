package com.english.generate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini API 응답을 파싱하기 위한 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerateResponse {

    private List<GeminiSentence> sentences;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiSentence {
        private String englishSentence;
        private String koreanTranslation;
        private List<Long> wordIds;
        private List<String> situations;
    }
}

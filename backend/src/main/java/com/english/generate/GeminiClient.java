package com.english.generate;

import java.util.List;
import java.util.Optional;

public interface GeminiClient {

	List<GeminiWordEnrichment> enrichWords(List<GeminiWordInput> words);

	List<GeminiExtractedWord> extractWordsFromImage(GeminiImage image);

	Optional<GeminiExtractedPattern> extractPatternFromImage(GeminiImage image);

	List<GeminiGeneratedSentence> generateSentences(GeminiSentenceGenerationRequest request);
}

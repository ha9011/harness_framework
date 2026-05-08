package com.english.generate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

class FakeGenerateGeminiClient implements GeminiClient {

	private final Queue<List<GeminiGeneratedSentence>> sentenceResponses = new ArrayDeque<>();
	private final List<GeminiSentenceGenerationRequest> sentenceRequests = new ArrayList<>();
	private RuntimeException sentenceFailure;

	void reset() {
		sentenceResponses.clear();
		sentenceRequests.clear();
		sentenceFailure = null;
	}

	void enqueueSentences(List<GeminiGeneratedSentence> sentences) {
		sentenceResponses.add(List.copyOf(sentences));
	}

	void failSentenceGeneration(RuntimeException exception) {
		sentenceFailure = exception;
	}

	GeminiSentenceGenerationRequest lastSentenceRequest() {
		return sentenceRequests.getLast();
	}

	int sentenceRequestCount() {
		return sentenceRequests.size();
	}

	@Override
	public List<GeminiWordEnrichment> enrichWords(List<GeminiWordInput> words) {
		throw new UnsupportedOperationException("word enrichment is not used by generate service tests");
	}

	@Override
	public List<GeminiExtractedWord> extractWordsFromImage(GeminiImage image) {
		throw new UnsupportedOperationException("word image extraction is not used by generate service tests");
	}

	@Override
	public Optional<GeminiExtractedPattern> extractPatternFromImage(GeminiImage image) {
		throw new UnsupportedOperationException("pattern image extraction is not used by generate service tests");
	}

	@Override
	public List<GeminiGeneratedSentence> generateSentences(GeminiSentenceGenerationRequest request) {
		sentenceRequests.add(request);
		if (sentenceFailure != null) {
			throw sentenceFailure;
		}
		if (sentenceResponses.isEmpty()) {
			throw new IllegalStateException("No fake Gemini sentence response enqueued");
		}
		return sentenceResponses.remove();
	}
}

package com.english.generate;

public class GeminiClientException extends RuntimeException {

	private final GeminiOperation operation;
	private final GeminiFailureType failureType;
	private final boolean retryable;
	private final boolean fallbackRecommended;

	public GeminiClientException(
			GeminiOperation operation,
			GeminiFailureType failureType,
			String message,
			boolean retryable,
			Throwable cause
	) {
		super(message, cause);
		this.operation = operation;
		this.failureType = failureType;
		this.retryable = retryable;
		this.fallbackRecommended = operation == GeminiOperation.WORD_ENRICHMENT;
	}

	public GeminiOperation getOperation() {
		return operation;
	}

	public GeminiFailureType getFailureType() {
		return failureType;
	}

	public boolean isRetryable() {
		return retryable;
	}

	public boolean isFallbackRecommended() {
		return fallbackRecommended;
	}
}

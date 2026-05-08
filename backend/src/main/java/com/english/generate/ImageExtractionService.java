package com.english.generate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageExtractionService {

	private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/webp",
			"image/gif");

	private final GeminiClient geminiClient;

	public ImageExtractionService(GeminiClient geminiClient) {
		this.geminiClient = geminiClient;
	}

	public List<GeminiExtractedWord> extractWords(MultipartFile image) {
		try {
			return geminiClient.extractWordsFromImage(toGeminiImage(image));
		}
		catch (GeminiClientException exception) {
			throw GenerateException.aiServiceError("이미지 추출에 실패했습니다");
		}
	}

	public Optional<GeminiExtractedPattern> extractPattern(MultipartFile image) {
		try {
			return geminiClient.extractPatternFromImage(toGeminiImage(image));
		}
		catch (GeminiClientException exception) {
			throw GenerateException.aiServiceError("이미지 추출에 실패했습니다");
		}
	}

	private static GeminiImage toGeminiImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw GenerateException.invalidImageFormat();
		}
		String contentType = image.getContentType();
		if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
			throw GenerateException.invalidImageFormat();
		}
		try {
			return new GeminiImage(contentType, image.getBytes());
		}
		catch (IOException | IllegalArgumentException exception) {
			throw GenerateException.invalidImageFormat();
		}
	}
}

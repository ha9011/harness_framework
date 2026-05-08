package com.english.generate;

import java.util.Arrays;

public record GeminiImage(String mimeType, byte[] data) {

	public GeminiImage {
		mimeType = GeminiValidation.requireText(mimeType, "mimeType");
		if (data == null || data.length == 0) {
			throw new IllegalArgumentException("image data is required");
		}
		data = Arrays.copyOf(data, data.length);
	}

	@Override
	public byte[] data() {
		return Arrays.copyOf(data, data.length);
	}
}

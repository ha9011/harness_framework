package com.english.config;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
		List<T> content,
		long totalElements,
		int totalPages,
		int page,
		int size
) {

	public PageResponse {
		content = List.copyOf(content);
	}

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.getNumber(),
				page.getSize());
	}
}

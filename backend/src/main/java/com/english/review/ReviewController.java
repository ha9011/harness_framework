package com.english.review;

import com.english.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping("/today")
	public List<ReviewCardResponse> today(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) ReviewItemType type,
			@RequestParam(required = false) String exclude
	) {
		if (type == null) {
			throw ReviewException.badRequest("복습 타입은 필수입니다");
		}
		return reviewService.getTodayReviews(principal.userId(), type, parseExclude(exclude));
	}

	@PostMapping("/{reviewItemId}")
	public ReviewResultResponse record(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long reviewItemId,
			@Valid @RequestBody ReviewRecordRequest request
	) {
		return reviewService.recordResult(principal.userId(), reviewItemId, request.result());
	}

	private static List<Long> parseExclude(String exclude) {
		if (exclude == null || exclude.isBlank()) {
			return List.of();
		}

		List<Long> excludedIds = new ArrayList<>();
		for (String token : exclude.split(",")) {
			String trimmed = token.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			excludedIds.add(parsePositiveId(trimmed));
		}
		return excludedIds;
	}

	private static Long parsePositiveId(String value) {
		try {
			long parsed = Long.parseLong(value);
			if (parsed <= 0) {
				throw ReviewException.badRequest("제외할 복습 항목 ID가 올바르지 않습니다");
			}
			return parsed;
		} catch (NumberFormatException exception) {
			throw ReviewException.badRequest("제외할 복습 항목 ID가 올바르지 않습니다");
		}
	}
}

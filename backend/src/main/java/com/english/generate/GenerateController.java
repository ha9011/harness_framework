package com.english.generate;

import com.english.auth.AuthenticatedUser;
import com.english.config.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/generate")
public class GenerateController {

	private static final int MAX_PAGE_SIZE = 100;

	private final GenerateService generateService;

	public GenerateController(GenerateService generateService) {
		this.generateService = generateService;
	}

	@PostMapping
	public ResponseEntity<GenerateResponse> generate(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody GenerateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(generateService.generate(principal.userId(), request.requiredLevel(), request.fullCount()));
	}

	@PostMapping("/word")
	public ResponseEntity<GenerateResponse> generateForWord(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody WordGenerateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(generateService.generateForWord(
						principal.userId(),
						request.wordId(),
						request.requiredLevel(),
						request.detailCount()));
	}

	@PostMapping("/pattern")
	public ResponseEntity<GenerateResponse> generateForPattern(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody PatternGenerateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(generateService.generateForPattern(
						principal.userId(),
						request.patternId(),
						request.requiredLevel(),
						request.patternCount()));
	}

	@GetMapping("/history")
	public PageResponse<GenerationHistoryResponse> history(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		return PageResponse.from(generateService.getHistory(principal.userId(), PageRequest.of(page, size)));
	}
}

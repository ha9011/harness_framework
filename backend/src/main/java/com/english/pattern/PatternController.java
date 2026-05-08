package com.english.pattern;

import com.english.auth.AuthenticatedUser;
import com.english.config.PageResponse;
import com.english.generate.GeminiExtractedPattern;
import com.english.generate.ImageExtractionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/patterns")
public class PatternController {

	private static final int MAX_PAGE_SIZE = 100;

	private final PatternService patternService;
	private final ImageExtractionService imageExtractionService;

	public PatternController(PatternService patternService, ImageExtractionService imageExtractionService) {
		this.patternService = patternService;
		this.imageExtractionService = imageExtractionService;
	}

	@GetMapping
	public PageResponse<PatternResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		return PageResponse.from(patternService.search(principal.userId(), pageable));
	}

	@GetMapping("/{id}")
	public PatternDetailResponse get(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long id
	) {
		return PatternDetailResponse.from(patternService.get(principal.userId(), id));
	}

	@PostMapping
	public ResponseEntity<PatternResponse> create(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody PatternCreateRequest request
	) {
		PatternResponse response = patternService.create(principal.userId(), request);
		return ResponseEntity.created(URI.create("/api/patterns/" + response.id()))
				.body(response);
	}

	@PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> extract(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestPart("image") MultipartFile image
	) {
		return imageExtractionService.extractPattern(image)
				.<ResponseEntity<?>>map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.ok(Map.of()));
	}

	@PutMapping("/{id}")
	public PatternResponse update(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long id,
			@Valid @RequestBody PatternUpdateRequest request
	) {
		return patternService.update(principal.userId(), id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long id
	) {
		patternService.delete(principal.userId(), id);
		return ResponseEntity.noContent().build();
	}
}

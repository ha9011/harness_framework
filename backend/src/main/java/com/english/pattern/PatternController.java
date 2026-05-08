package com.english.pattern;

import com.english.auth.AuthenticatedUser;
import com.english.config.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/patterns")
public class PatternController {

	private static final int MAX_PAGE_SIZE = 100;

	private final PatternService patternService;

	public PatternController(PatternService patternService) {
		this.patternService = patternService;
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

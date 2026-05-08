package com.english.word;

import com.english.auth.AuthenticatedUser;
import com.english.config.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/words")
public class WordController {

	private static final int MAX_PAGE_SIZE = 100;

	private final WordService wordService;

	public WordController(WordService wordService) {
		this.wordService = wordService;
	}

	@GetMapping
	public PageResponse<WordResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String partOfSpeech,
			@RequestParam(defaultValue = "false") Boolean importantOnly,
			@RequestParam(defaultValue = "createdAt") String sort
	) {
		Pageable pageable = PageRequest.of(page, size, wordSort(sort));
		return PageResponse.from(wordService.search(
				principal.userId(),
				new WordSearchCondition(search, partOfSpeech, importantOnly),
				pageable));
	}

	@GetMapping("/{id}")
	public WordDetailResponse get(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long id
	) {
		return WordDetailResponse.from(wordService.get(principal.userId(), id));
	}

	@PostMapping
	public ResponseEntity<WordResponse> create(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody WordCreateRequest request
	) {
		WordResponse response = wordService.create(principal.userId(), request);
		return ResponseEntity.created(URI.create("/api/words/" + response.id()))
				.body(response);
	}

	@PostMapping("/bulk")
	public ResponseEntity<WordBulkSaveResponse> createBulk(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody List<@Valid WordCreateRequest> requests
	) {
		if (requests == null || requests.isEmpty()) {
			throw WordException.badRequest("저장할 단어가 없습니다");
		}
		WordBulkSaveResponse response = WordBulkSaveResponse.from(wordService.saveBulk(principal.userId(), requests));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(response);
	}

	@PutMapping("/{id}")
	public WordResponse update(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long id,
			@Valid @RequestBody WordUpdateRequest request
	) {
		return wordService.update(principal.userId(), id, request);
	}

	@PatchMapping("/{id}/important")
	public WordResponse toggleImportant(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long id
	) {
		return wordService.toggleImportant(principal.userId(), id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long id
	) {
		wordService.delete(principal.userId(), id);
		return ResponseEntity.noContent().build();
	}

	private static Sort wordSort(String sort) {
		if (sort == null || sort.isBlank() || "createdAt".equals(sort)) {
			return Sort.by(Sort.Direction.DESC, "createdAt");
		}
		if ("word".equals(sort)) {
			return Sort.by(Sort.Direction.ASC, "word");
		}
		throw WordException.badRequest("지원하지 않는 단어 정렬입니다");
	}
}

package com.english.word;

import com.english.auth.User;
import com.english.pattern.PatternService;
import com.english.pattern.WordExtractResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;
    private final PatternService patternService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WordResponse create(@AuthenticationPrincipal User user,
                               @RequestBody @Valid WordCreateRequest request) {
        return wordService.create(user, request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkCreateResponse bulkCreate(@AuthenticationPrincipal User user,
                                         @RequestBody List<WordCreateRequest> requests) {
        return wordService.bulkCreate(user, requests);
    }

    @GetMapping
    public Page<WordListResponse> getList(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String partOfSpeech,
            @RequestParam(defaultValue = "false") boolean importantOnly,
            @RequestParam(defaultValue = "latest") String sort,
            Pageable pageable) {
        return wordService.getList(user, search, partOfSpeech, importantOnly, sort, pageable);
    }

    @GetMapping("/{id}")
    public WordDetailResponse getDetail(@AuthenticationPrincipal User user,
                                        @PathVariable Long id) {
        return wordService.getDetail(user, id);
    }

    @PatchMapping("/{id}/important")
    public WordResponse toggleImportant(@AuthenticationPrincipal User user,
                                        @PathVariable Long id) {
        return wordService.toggleImportant(user, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user,
                       @PathVariable Long id) {
        wordService.delete(user, id);
    }

    @PostMapping("/extract")
    public WordExtractResponse extractFromImage(@RequestParam("image") MultipartFile image) {
        return patternService.extractWordsFromImage(image);
    }
}

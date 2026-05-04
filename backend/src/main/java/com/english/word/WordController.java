package com.english.word;

import com.english.pattern.PatternService;
import com.english.pattern.WordExtractResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    public WordResponse create(@RequestBody @Valid WordCreateRequest request) {
        return wordService.create(request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkCreateResponse bulkCreate(@RequestBody List<WordCreateRequest> requests) {
        return wordService.bulkCreate(requests);
    }

    @GetMapping
    public Page<WordListResponse> getList(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String partOfSpeech,
            @RequestParam(defaultValue = "false") boolean importantOnly,
            @RequestParam(defaultValue = "latest") String sort,
            Pageable pageable) {
        return wordService.getList(search, partOfSpeech, importantOnly, sort, pageable);
    }

    @GetMapping("/{id}")
    public WordDetailResponse getDetail(@PathVariable Long id) {
        return wordService.getDetail(id);
    }

    @PatchMapping("/{id}/important")
    public WordResponse toggleImportant(@PathVariable Long id) {
        return wordService.toggleImportant(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        wordService.delete(id);
    }

    @PostMapping("/extract")
    public WordExtractResponse extractFromImage(@RequestParam("image") MultipartFile image) {
        return patternService.extractWordsFromImage(image);
    }
}

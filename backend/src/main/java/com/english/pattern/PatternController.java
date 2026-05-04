package com.english.pattern;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/patterns")
@RequiredArgsConstructor
public class PatternController {

    private final PatternService patternService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatternResponse create(@RequestBody @Valid PatternCreateRequest request) {
        return patternService.create(request);
    }

    @GetMapping
    public Page<PatternListResponse> getList(Pageable pageable) {
        return patternService.getList(pageable);
    }

    @GetMapping("/{id}")
    public PatternDetailResponse getDetail(@PathVariable Long id) {
        return patternService.getDetail(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        patternService.delete(id);
    }

    @PostMapping("/extract")
    public PatternExtractResponse extract(@RequestParam("image") MultipartFile image) {
        return patternService.extractFromImage(image);
    }
}

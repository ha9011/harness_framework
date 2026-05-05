package com.english.pattern;

import com.english.auth.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/patterns")
@RequiredArgsConstructor
public class PatternController {

    private final PatternService patternService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatternResponse create(@AuthenticationPrincipal User user,
                                  @RequestBody @Valid PatternCreateRequest request) {
        return patternService.create(user, request);
    }

    @GetMapping
    public Page<PatternListResponse> getList(@AuthenticationPrincipal User user,
                                             Pageable pageable) {
        return patternService.getList(user, pageable);
    }

    @GetMapping("/{id}")
    public PatternDetailResponse getDetail(@AuthenticationPrincipal User user,
                                           @PathVariable Long id) {
        return patternService.getDetail(user, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user,
                       @PathVariable Long id) {
        patternService.delete(user, id);
    }

    @PostMapping("/extract")
    public PatternExtractResponse extract(@RequestParam("image") MultipartFile image) {
        return patternService.extractFromImage(image);
    }
}

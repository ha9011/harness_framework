package com.english.generate;

import com.english.auth.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
public class GenerateController {

    private final GenerateService generateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateResponse generate(@AuthenticationPrincipal User user,
                                     @Valid @RequestBody GenerateRequest request) {
        return generateService.generate(user, request);
    }

    @PostMapping("/word")
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateResponse generateByWord(@AuthenticationPrincipal User user,
                                           @Valid @RequestBody GenerateRequest request) {
        return generateService.generateByWord(user, request.getWordId(), request);
    }

    @PostMapping("/pattern")
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateResponse generateByPattern(@AuthenticationPrincipal User user,
                                              @Valid @RequestBody GenerateRequest request) {
        return generateService.generateByPattern(user, request.getPatternId(), request);
    }

    @GetMapping("/history")
    public Page<GenerationHistoryResponse> getHistory(@AuthenticationPrincipal User user,
                                                      Pageable pageable) {
        return generateService.getHistory(user, pageable);
    }
}

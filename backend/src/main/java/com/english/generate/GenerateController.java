package com.english.generate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
public class GenerateController {

    private final GenerateService generateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateResponse generate(@Valid @RequestBody GenerateRequest request) {
        return generateService.generate(request);
    }

    @PostMapping("/word")
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateResponse generateByWord(@Valid @RequestBody GenerateRequest request) {
        return generateService.generateByWord(request.getWordId(), request);
    }

    @PostMapping("/pattern")
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateResponse generateByPattern(@Valid @RequestBody GenerateRequest request) {
        return generateService.generateByPattern(request.getPatternId(), request);
    }

    @GetMapping("/history")
    public Page<GenerationHistoryResponse> getHistory(Pageable pageable) {
        return generateService.getHistory(pageable);
    }
}

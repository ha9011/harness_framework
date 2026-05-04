package com.english.review;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/today")
    public ResponseEntity<List<ReviewCardResponse>> getTodayCards(
            @RequestParam String type,
            @RequestParam(required = false) List<Long> exclude) {

        List<Long> excludeIds = (exclude != null) ? exclude : Collections.emptyList();
        List<ReviewCardResponse> cards = reviewService.getTodayCards(type, excludeIds);
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/{id}")
    public ResponseEntity<ReviewResultResponse> submitResult(
            @PathVariable Long id,
            @Valid @RequestBody ReviewResultRequest request) {

        ReviewResultResponse response = reviewService.submitResult(id, request.getResult());
        return ResponseEntity.ok(response);
    }
}

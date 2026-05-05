package com.english.review;

import com.english.auth.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal User user,
            @RequestParam String type,
            @RequestParam(required = false) List<Long> exclude) {

        List<Long> excludeIds = (exclude != null) ? exclude : Collections.emptyList();
        List<ReviewCardResponse> cards = reviewService.getTodayCards(user, type, excludeIds);
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/{id}")
    public ResponseEntity<ReviewResultResponse> submitResult(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ReviewResultRequest request) {

        ReviewResultResponse response = reviewService.submitResult(user, id, request.getResult());
        return ResponseEntity.ok(response);
    }
}

package com.english.review;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResultRequest {
    @NotNull
    @Pattern(regexp = "EASY|MEDIUM|HARD")
    private String result;
}

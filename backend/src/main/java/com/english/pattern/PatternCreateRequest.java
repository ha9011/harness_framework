package com.english.pattern;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PatternCreateRequest {

    @NotBlank(message = "패턴을 입력해주세요")
    private String template;

    private String description;

    private List<ExampleRequest> examples;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExampleRequest {
        private String sentence;
        private String translation;
    }
}

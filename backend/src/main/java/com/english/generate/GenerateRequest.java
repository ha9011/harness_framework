package com.english.generate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRequest {

    @NotBlank
    private String level;

    @NotNull
    private Integer count;

    private Long wordId;

    private Long patternId;
}

package com.english.word;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WordCreateRequest {

    @NotBlank(message = "단어를 입력해주세요")
    private String word;

    @NotBlank(message = "뜻을 입력해주세요")
    private String meaning;
}

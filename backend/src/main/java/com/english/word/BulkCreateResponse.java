package com.english.word;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BulkCreateResponse {
    private int saved;
    private int skipped;
    private int enrichmentFailed;
    private List<WordResponse> words;
}

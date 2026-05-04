package com.english.study;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study-records")
@RequiredArgsConstructor
public class StudyRecordController {

    private final StudyRecordService studyRecordService;

    @GetMapping
    public Page<StudyRecordResponse> getRecords(Pageable pageable) {
        return studyRecordService.getRecords(pageable);
    }
}

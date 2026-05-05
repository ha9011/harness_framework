package com.english.study;

import com.english.auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study-records")
@RequiredArgsConstructor
public class StudyRecordController {

    private final StudyRecordService studyRecordService;

    @GetMapping
    public Page<StudyRecordResponse> getRecords(@AuthenticationPrincipal User user,
                                                 Pageable pageable) {
        return studyRecordService.getRecords(user, pageable);
    }
}

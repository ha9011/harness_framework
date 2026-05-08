package com.english.study;

import com.english.auth.AuthenticatedUser;
import com.english.config.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/study-records")
public class StudyRecordController {

	private static final int MAX_PAGE_SIZE = 100;

	private final StudyRecordService studyRecordService;

	public StudyRecordController(StudyRecordService studyRecordService) {
		this.studyRecordService = studyRecordService;
	}

	@GetMapping
	public PageResponse<StudyRecordResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		return PageResponse.from(studyRecordService.getStudyRecords(principal.userId(), page, size));
	}
}

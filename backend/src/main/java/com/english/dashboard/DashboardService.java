package com.english.dashboard;

import com.english.auth.User;
import com.english.generate.GeneratedSentenceRepository;
import com.english.pattern.PatternRepository;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewLogRepository;
import com.english.study.StudyRecord;
import com.english.study.StudyRecordItemRepository;
import com.english.study.StudyRecordRepository;
import com.english.word.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WordRepository wordRepository;
    private final PatternRepository patternRepository;
    private final GeneratedSentenceRepository generatedSentenceRepository;
    private final ReviewItemRepository reviewItemRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final StudyRecordItemRepository studyRecordItemRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(User user) {
        long wordCount = wordRepository.countByUserAndDeletedFalse(user);
        long patternCount = patternRepository.countByUserAndDeletedFalse(user);
        long sentenceCount = generatedSentenceRepository.countByUser(user);

        int streak = calculateStreak(user);

        LocalDate today = LocalDate.now();
        long wordRemaining = reviewItemRepository.countTodayRemaining(user, "WORD", today);
        long patternRemaining = reviewItemRepository.countTodayRemaining(user, "PATTERN", today);
        long sentenceRemaining = reviewItemRepository.countTodayRemaining(user, "SENTENCE", today);
        DashboardResponse.ReviewRemaining remaining =
                new DashboardResponse.ReviewRemaining(wordRemaining, patternRemaining, sentenceRemaining);

        List<StudyRecord> recentRecords = studyRecordRepository.findTop5ByUserOrderByCreatedAtDesc(user);
        List<DashboardResponse.StudyRecordDto> recentDtos = recentRecords.stream()
                .map(r -> new DashboardResponse.StudyRecordDto(
                        r.getId(),
                        r.getDayNumber(),
                        r.getCreatedAt().toString(),
                        (int) studyRecordItemRepository.countByStudyRecordIdAndItemType(r.getId(), "WORD"),
                        (int) studyRecordItemRepository.countByStudyRecordIdAndItemType(r.getId(), "PATTERN")
                ))
                .collect(Collectors.toList());

        return new DashboardResponse(wordCount, patternCount, sentenceCount, streak, remaining, recentDtos);
    }

    private int calculateStreak(User user) {
        List<LocalDate> reviewDates = reviewLogRepository.findDistinctReviewDates(user);
        if (reviewDates.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 오늘 기록 있으면 오늘부터, 없으면 어제부터 시작
        LocalDate start;
        if (reviewDates.contains(today)) {
            start = today;
        } else if (reviewDates.contains(yesterday)) {
            start = yesterday;
        } else {
            return 0;
        }

        int streak = 0;
        LocalDate check = start;
        while (reviewDates.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }
        return streak;
    }
}

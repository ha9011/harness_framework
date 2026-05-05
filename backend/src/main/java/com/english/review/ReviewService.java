package com.english.review;

import com.english.auth.User;
import com.english.config.ForbiddenException;
import com.english.config.NotFoundException;
import com.english.generate.GeneratedSentence;
import com.english.generate.GeneratedSentenceRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.setting.SettingService;
import com.english.word.Word;
import com.english.word.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewItemRepository reviewItemRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final WordRepository wordRepository;
    private final PatternRepository patternRepository;
    private final GeneratedSentenceRepository generatedSentenceRepository;
    private final SettingService settingService;

    /**
     * 오늘 복습할 카드 선정
     */
    @Transactional(readOnly = true)
    public List<ReviewCardResponse> getTodayCards(User user, String type, List<Long> exclude) {
        List<Long> excludeIds = (exclude == null || exclude.isEmpty())
                ? Collections.singletonList(-1L) : exclude;

        List<ReviewItem> items = reviewItemRepository.findTodayCards(user, type, LocalDate.now(), excludeIds);

        // 카드 응답 빌드 (원본 삭제된 항목은 null → 제외)
        List<ReviewCardResponse> cards = items.stream()
                .map(this::buildCardResponse)
                .filter(card -> card != null)
                .collect(Collectors.toList());

        // LIMIT N (설정에서 dailyReviewCount 조회)
        int dailyReviewCount = settingService.getSetting(user).getDailyReviewCount();
        int limit = Math.min(cards.size(), dailyReviewCount);
        List<ReviewCardResponse> selected = new ArrayList<>(cards.subList(0, limit));

        // 랜덤 셔플
        Collections.shuffle(selected);

        return selected;
    }

    /**
     * SM-2 결과 제출
     */
    @Transactional
    public ReviewResultResponse submitResult(User user, Long reviewItemId, String result) {
        ReviewItem item = reviewItemRepository.findById(reviewItemId)
                .orElseThrow(() -> new NotFoundException("복습 아이템을 찾을 수 없습니다."));

        // IDOR 방어: 소유권 검증
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }

        ReviewLog log = item.applyResult(result);
        reviewLogRepository.save(log);

        return new ReviewResultResponse(
                item.getId(),
                result,
                item.getIntervalDays(),
                item.getNextReviewDate(),
                item.getEaseFactor(),
                item.getReviewCount()
        );
    }

    private ReviewCardResponse buildCardResponse(ReviewItem item) {
        switch (item.getItemType()) {
            case "WORD":
                return buildWordCard(item);
            case "PATTERN":
                return buildPatternCard(item);
            case "SENTENCE":
                return buildSentenceCard(item);
            default:
                return null;
        }
    }

    private ReviewCardResponse buildWordCard(ReviewItem item) {
        Word word = wordRepository.findByIdAndUserAndDeletedFalse(item.getItemId(), item.getUser()).orElse(null);
        if (word == null) return null;

        ReviewCardResponse.FrontContent front;
        ReviewCardResponse.BackContent back;

        if ("RECOGNITION".equals(item.getDirection())) {
            front = new ReviewCardResponse.FrontContent(word.getWord(), null, null, null, null, null);

            List<ReviewCardResponse.ExampleDto> examples = getWordExamples(item.getItemId());
            back = new ReviewCardResponse.BackContent(null, word.getMeaning(), word.getPartOfSpeech(),
                    null, null, null, examples);
        } else {
            front = new ReviewCardResponse.FrontContent(null, word.getMeaning(), null, null, null, null);
            back = new ReviewCardResponse.BackContent(word.getWord(), null, word.getPartOfSpeech(),
                    null, null, null, null);
        }

        return new ReviewCardResponse(item.getId(), item.getItemType(), item.getDirection(), front, back);
    }

    private ReviewCardResponse buildPatternCard(ReviewItem item) {
        Pattern pattern = patternRepository.findByIdAndUserAndDeletedFalse(item.getItemId(), item.getUser()).orElse(null);
        if (pattern == null) return null;

        ReviewCardResponse.FrontContent front;
        ReviewCardResponse.BackContent back;

        if ("RECOGNITION".equals(item.getDirection())) {
            front = new ReviewCardResponse.FrontContent(null, null, pattern.getTemplate(), null, null, null);

            List<ReviewCardResponse.ExampleDto> examples = pattern.getExamples().stream()
                    .map(ex -> new ReviewCardResponse.ExampleDto(ex.getSentence(), ex.getTranslation()))
                    .collect(Collectors.toList());
            back = new ReviewCardResponse.BackContent(null, null, null,
                    null, pattern.getDescription(), null, examples);
        } else {
            front = new ReviewCardResponse.FrontContent(null, null, null, pattern.getDescription(), null, null);
            back = new ReviewCardResponse.BackContent(null, null, null,
                    pattern.getTemplate(), null, null, null);
        }

        return new ReviewCardResponse(item.getId(), item.getItemType(), item.getDirection(), front, back);
    }

    private ReviewCardResponse buildSentenceCard(ReviewItem item) {
        GeneratedSentence sentence = generatedSentenceRepository.findById(item.getItemId()).orElse(null);
        if (sentence == null) return null;

        // 상황 5개 중 랜덤 1개 선택
        String situation = null;
        if (sentence.getSituations() != null && !sentence.getSituations().isEmpty()) {
            int randomIndex = (int) (Math.random() * sentence.getSituations().size());
            situation = sentence.getSituations().get(randomIndex).getSituation();
        }

        ReviewCardResponse.FrontContent front = new ReviewCardResponse.FrontContent(
                null, null, null, null, sentence.getEnglishSentence(), situation);
        ReviewCardResponse.BackContent back = new ReviewCardResponse.BackContent(
                null, null, null, null, null, sentence.getKoreanTranslation(), null);

        return new ReviewCardResponse(item.getId(), item.getItemType(), item.getDirection(), front, back);
    }

    private List<ReviewCardResponse.ExampleDto> getWordExamples(Long wordId) {
        List<GeneratedSentence> sentences = generatedSentenceRepository.findByWordId(wordId);
        if (sentences.isEmpty()) return Collections.emptyList();

        if (sentences.size() <= 3) {
            return sentences.stream()
                    .map(s -> new ReviewCardResponse.ExampleDto(s.getEnglishSentence(), s.getKoreanTranslation()))
                    .collect(Collectors.toList());
        }

        // 4개 이상이면 랜덤 3개 선택
        List<GeneratedSentence> shuffled = new ArrayList<>(sentences);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, 3).stream()
                .map(s -> new ReviewCardResponse.ExampleDto(s.getEnglishSentence(), s.getKoreanTranslation()))
                .collect(Collectors.toList());
    }
}

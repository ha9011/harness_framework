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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
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
    @Transactional
    public List<ReviewCardResponse> getTodayCards(User user, String type, List<Long> exclude) {
        // dailyReviewCount 먼저 조회
        int dailyReviewCount = settingService.getSetting(user).getDailyReviewCount();
        if (dailyReviewCount <= 0) {
            return Collections.emptyList();
        }

        List<Long> excludeIds = (exclude == null || exclude.isEmpty())
                ? Collections.singletonList(-1L) : exclude;

        // LIMIT 적용하여 필요한 만큼만 조회
        List<ReviewItem> items = reviewItemRepository.findTodayCards(
                user, type, LocalDate.now(), excludeIds, PageRequest.of(0, dailyReviewCount));

        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        // itemId 수집
        List<Long> itemIds = items.stream()
                .map(ReviewItem::getItemId)
                .collect(Collectors.toList());

        // 타입별 배치 조회 + 카드 빌드
        List<ReviewCardResponse> cards;
        switch (type) {
            case "WORD":
                cards = buildWordCards(items, itemIds, user);
                break;
            case "PATTERN":
                cards = buildPatternCards(items, itemIds, user);
                break;
            case "SENTENCE":
                cards = buildSentenceCards(items, itemIds);
                break;
            default:
                cards = Collections.emptyList();
                break;
        }

        // 랜덤 셔플
        List<ReviewCardResponse> result = new ArrayList<>(cards);
        Collections.shuffle(result);

        return result;
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

    private List<ReviewCardResponse> buildWordCards(List<ReviewItem> items, List<Long> itemIds, User user) {
        // 단어 배치 조회
        Map<Long, Word> wordMap = wordRepository.findByIdInAndUserAndDeletedFalse(itemIds, user)
                .stream()
                .collect(Collectors.toMap(Word::getId, w -> w));

        // RECOGNITION 방향만 예문 배치 조회
        List<Long> recognitionWordIds = items.stream()
                .filter(i -> "RECOGNITION".equals(i.getDirection()))
                .map(ReviewItem::getItemId)
                .filter(wordMap::containsKey)
                .collect(Collectors.toList());

        Map<Long, List<GeneratedSentence>> wordExamplesMap = Collections.emptyMap();
        if (!recognitionWordIds.isEmpty()) {
            wordExamplesMap = generatedSentenceRepository.findByWordIdInWithMapping(recognitionWordIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            row -> (Long) row[0],
                            Collectors.mapping(row -> (GeneratedSentence) row[1], Collectors.toList())
                    ));
        }

        Map<Long, List<GeneratedSentence>> finalWordExamplesMap = wordExamplesMap;
        return items.stream()
                .map(item -> {
                    Word word = wordMap.get(item.getItemId());
                    if (word == null) return null;
                    return buildWordCard(item, word, finalWordExamplesMap);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ReviewCardResponse buildWordCard(ReviewItem item, Word word, Map<Long, List<GeneratedSentence>> wordExamplesMap) {
        ReviewCardResponse.FrontContent front;
        ReviewCardResponse.BackContent back;

        if ("RECOGNITION".equals(item.getDirection())) {
            front = new ReviewCardResponse.FrontContent(word.getWord(), null, null, null, null, null);

            List<ReviewCardResponse.ExampleDto> examples = getWordExamplesFromMap(item.getItemId(), wordExamplesMap);
            back = new ReviewCardResponse.BackContent(null, word.getMeaning(), word.getPartOfSpeech(),
                    null, null, null, examples);
        } else {
            front = new ReviewCardResponse.FrontContent(null, word.getMeaning(), null, null, null, null);
            back = new ReviewCardResponse.BackContent(word.getWord(), null, word.getPartOfSpeech(),
                    null, null, null, null);
        }

        return new ReviewCardResponse(item.getId(), item.getItemType(), item.getDirection(), front, back);
    }

    private List<ReviewCardResponse.ExampleDto> getWordExamplesFromMap(Long wordId, Map<Long, List<GeneratedSentence>> wordExamplesMap) {
        List<GeneratedSentence> sentences = wordExamplesMap.getOrDefault(wordId, Collections.emptyList());
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

    private List<ReviewCardResponse> buildPatternCards(List<ReviewItem> items, List<Long> itemIds, User user) {
        // 패턴 배치 조회 (examples JOIN FETCH 포함)
        Map<Long, Pattern> patternMap = patternRepository.findByIdInWithExamples(itemIds, user)
                .stream()
                .collect(Collectors.toMap(Pattern::getId, p -> p));

        return items.stream()
                .map(item -> {
                    Pattern pattern = patternMap.get(item.getItemId());
                    if (pattern == null) return null;
                    return buildPatternCard(item, pattern);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ReviewCardResponse buildPatternCard(ReviewItem item, Pattern pattern) {
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

    private List<ReviewCardResponse> buildSentenceCards(List<ReviewItem> items, List<Long> itemIds) {
        // 문장 배치 조회 (situations JOIN FETCH 포함)
        Map<Long, GeneratedSentence> sentenceMap = generatedSentenceRepository.findByIdInWithSituations(itemIds)
                .stream()
                .collect(Collectors.toMap(GeneratedSentence::getId, gs -> gs));

        return items.stream()
                .map(item -> {
                    GeneratedSentence sentence = sentenceMap.get(item.getItemId());
                    if (sentence == null) return null;
                    return buildSentenceCard(item, sentence);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ReviewCardResponse buildSentenceCard(ReviewItem item, GeneratedSentence sentence) {
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
}

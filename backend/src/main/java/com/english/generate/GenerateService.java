package com.english.generate;

import com.english.config.GeminiClient;
import com.english.config.NoPatternsException;
import com.english.config.NoWordsException;
import com.english.config.NotFoundException;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.review.ReviewItemService;
import com.english.word.Word;
import com.english.word.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateService {

    private final WordRepository wordRepository;
    private final PatternRepository patternRepository;
    private final GeneratedSentenceRepository generatedSentenceRepository;
    private final GenerationHistoryRepository generationHistoryRepository;
    private final GeminiClient geminiClient;
    private final ReviewItemService reviewItemService;

    private static final int MAX_WORDS = 50;

    @Transactional
    public GenerateResponse generate(GenerateRequest request) {
        List<Word> words = selectWords();
        List<Pattern> patterns = selectPatterns();

        String prompt = buildPrompt(words, patterns, request.getLevel(), request.getCount(), null, null);
        List<GenerateResponse.SentenceResponse> sentences = callGeminiAndSave(prompt, request.getLevel());

        saveHistory(request.getLevel(), request.getCount(), sentences.size(), null, null);

        return new GenerateResponse(null, sentences);
    }

    @Transactional
    public GenerateResponse generateByWord(Long wordId, GenerateRequest request) {
        Word targetWord = wordRepository.findByIdAndDeletedFalse(wordId)
                .orElseThrow(() -> new NotFoundException("단어를 찾을 수 없습니다: " + wordId));

        List<Word> words = selectWords();
        List<Pattern> patterns = selectPatterns();

        String prompt = buildPrompt(words, patterns, request.getLevel(), request.getCount(), targetWord, null);
        List<GenerateResponse.SentenceResponse> sentences = callGeminiAndSave(prompt, request.getLevel());

        saveHistory(request.getLevel(), request.getCount(), sentences.size(), wordId, null);

        return new GenerateResponse(null, sentences);
    }

    @Transactional
    public GenerateResponse generateByPattern(Long patternId, GenerateRequest request) {
        Pattern targetPattern = patternRepository.findByIdAndDeletedFalse(patternId)
                .orElseThrow(() -> new NotFoundException("패턴을 찾을 수 없습니다: " + patternId));

        List<Word> words = selectWords();

        String prompt = buildPrompt(words, List.of(targetPattern), request.getLevel(), request.getCount(), null, targetPattern);
        List<GenerateResponse.SentenceResponse> sentences = callGeminiAndSave(prompt, request.getLevel());

        saveHistory(request.getLevel(), request.getCount(), sentences.size(), null, patternId);

        return new GenerateResponse(null, sentences);
    }

    @Transactional(readOnly = true)
    public Page<GenerationHistoryResponse> getHistory(Pageable pageable) {
        return generationHistoryRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(GenerationHistoryResponse::from);
    }

    // 단어 선택: ⭐중요 > 복습 적은 것 > 랜덤, 최대 50개
    private List<Word> selectWords() {
        List<Word> allWords = wordRepository.findByDeletedFalse();
        if (allWords.isEmpty()) {
            throw new NoWordsException("등록된 단어가 없습니다");
        }

        // 중요 단어 우선 정렬
        List<Word> sorted = new ArrayList<>(allWords);
        sorted.sort((a, b) -> {
            if (a.isImportant() != b.isImportant()) {
                return a.isImportant() ? -1 : 1;
            }
            return 0;
        });

        // 최대 50개
        if (sorted.size() > MAX_WORDS) {
            sorted = sorted.subList(0, MAX_WORDS);
        }

        return sorted;
    }

    private List<Pattern> selectPatterns() {
        Page<Pattern> patternPage = patternRepository.findByDeletedFalse(PageRequest.of(0, 100));
        if (patternPage.isEmpty()) {
            throw new NoPatternsException("등록된 패턴이 없습니다");
        }
        return patternPage.getContent();
    }

    private List<GenerateResponse.SentenceResponse> callGeminiAndSave(String prompt, String level) {
        GeminiGenerateResponse geminiResponse = geminiClient.generateContent(prompt, GeminiGenerateResponse.class);

        List<GenerateResponse.SentenceResponse> result = new ArrayList<>();

        for (GeminiGenerateResponse.GeminiSentence gs : geminiResponse.getSentences()) {
            GeneratedSentence sentence = new GeneratedSentence(
                    gs.getEnglishSentence(), gs.getKoreanTranslation(), level);

            // situation 추가
            if (gs.getSituations() != null) {
                for (String situation : gs.getSituations()) {
                    sentence.addSituation(situation);
                }
            }

            // word 매핑 (존재하는 ID만)
            if (gs.getWordIds() != null) {
                for (Long wordId : gs.getWordIds()) {
                    if (wordRepository.existsById(wordId)) {
                        sentence.addSentenceWord(wordId);
                    } else {
                        log.warn("Gemini가 반환한 wordId {}는 존재하지 않아 매핑 무시", wordId);
                    }
                }
            }

            GeneratedSentence saved = generatedSentenceRepository.save(sentence);

            // SENTENCE RECOGNITION review_item 생성
            reviewItemService.createSentenceReviewItem(saved.getId());

            result.add(GenerateResponse.SentenceResponse.from(saved));
        }

        return result;
    }

    private void saveHistory(String level, int requestedCount, int actualCount, Long wordId, Long patternId) {
        GenerationHistory history = new GenerationHistory(level, requestedCount, actualCount, wordId, patternId);
        GenerationHistory saved = generationHistoryRepository.save(history);
        // GenerateResponse에 generationId 설정은 Controller에서 처리
    }

    private String buildPrompt(List<Word> words, List<Pattern> patterns, String level, int count,
                                Word targetWord, Pattern targetPattern) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("영어 예문을 생성해주세요.\n\n");

        // 난이도 설명
        prompt.append("난이도: ").append(getLevelDescription(level)).append("\n\n");

        // 단어 목록
        prompt.append("사용할 단어 목록:\n");
        String wordJson = words.stream()
                .map(w -> "{\"id\":" + w.getId() + ", \"word\":\"" + w.getWord() + "\", \"meaning\":\"" + w.getMeaning() + "\"}")
                .collect(Collectors.joining(", ", "[", "]"));
        prompt.append(wordJson).append("\n\n");

        // 패턴 목록
        prompt.append("사용할 패턴 목록:\n");
        String patternJson = patterns.stream()
                .map(p -> "{\"template\":\"" + p.getTemplate() + "\", \"description\":\"" + (p.getDescription() != null ? p.getDescription() : "") + "\"}")
                .collect(Collectors.joining(", ", "[", "]"));
        prompt.append(patternJson).append("\n\n");

        // 특정 단어/패턴 지정
        if (targetWord != null) {
            prompt.append("⚠️ 반드시 단어 \"").append(targetWord.getWord()).append("\"(id:").append(targetWord.getId()).append(")를 포함하는 예문을 생성하세요.\n\n");
        }
        if (targetPattern != null) {
            prompt.append("⚠️ 반드시 패턴 \"").append(targetPattern.getTemplate()).append("\"을 사용하는 예문을 생성하세요.\n\n");
        }

        prompt.append("생성 개수: ").append(count).append("개\n\n");

        prompt.append("각 예문마다 감정이입할 수 있는 상황(situation) 5개를 함께 생성하세요.\n");
        prompt.append("각 예문에 사용된 단어의 id를 wordIds 배열에 포함하세요.\n\n");

        prompt.append("아래 JSON 형식으로 응답해주세요:\n");
        prompt.append("{\n");
        prompt.append("  \"sentences\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"englishSentence\": \"영어 예문\",\n");
        prompt.append("      \"koreanTranslation\": \"한국어 해석\",\n");
        prompt.append("      \"wordIds\": [사용된 단어 ID 배열],\n");
        prompt.append("      \"situations\": [\"상황1\", \"상황2\", \"상황3\", \"상황4\", \"상황5\"]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}");

        return prompt.toString();
    }

    private String getLevelDescription(String level) {
        return switch (level) {
            case "TODDLER" -> "유아 (3~5단어, 아주 쉬운 문장)";
            case "ELEMENTARY" -> "초등 (카페 주문 수준)";
            case "INTERMEDIATE" -> "중등 (카카오톡 대화 수준)";
            case "ADVANCED" -> "고등 (회사 대화 수준)";
            default -> level;
        };
    }
}

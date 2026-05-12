package com.english.word;

import com.english.auth.User;
import com.english.config.DuplicateException;
import com.english.config.EmptyRequestException;
import com.english.config.GeminiClient;
import com.english.config.NotFoundException;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemService;
import com.english.study.StudyRecord;
import com.english.study.StudyRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordService {

    static final int BULK_ENRICHMENT_BATCH_SIZE = 25;

    private final WordRepository wordRepository;
    private final GeminiClient geminiClient;
    private final StudyRecordService studyRecordService;
    private final ReviewItemService reviewItemService;
    private final ReviewItemRepository reviewItemRepository;

    @Transactional
    public WordResponse create(User user, WordCreateRequest request) {
        if (wordRepository.existsByWordAndUserAndDeletedFalse(request.getWord(), user)) {
            throw new DuplicateException("이미 등록된 단어입니다: " + request.getWord());
        }

        Word word = new Word(user, request.getWord(), request.getMeaning());

        // AI 보강 시도
        try {
            String prompt = buildEnrichmentPrompt(request.getWord(), request.getMeaning());
            WordEnrichment enrichment = geminiClient.generateContent(prompt, WordEnrichment.class);
            word.enrich(enrichment.getPartOfSpeech(), enrichment.getPronunciation(),
                    enrichment.getSynonyms(), enrichment.getTip());
        } catch (Exception e) {
            log.warn("단어 보강 실패 (보강 없이 저장): {}", e.getMessage());
        }

        Word saved = wordRepository.save(word);

        // 학습 기록 연동
        StudyRecord record = studyRecordService.getOrCreateTodayRecord(user);
        studyRecordService.addItem(record, "WORD", saved.getId());

        // 복습 아이템 생성
        reviewItemService.createWordReviewItems(user, saved.getId());

        return WordResponse.from(saved);
    }

    @Transactional
    public BulkCreateResponse bulkCreate(User user, List<WordCreateRequest> requests) {
        if (requests.isEmpty()) {
            throw new EmptyRequestException("등록할 단어가 없습니다");
        }

        int skipped = 0;
        int enrichmentFailed = 0;

        // 1. 중복 검사 — 유효한 단어만 분리
        List<WordCreateRequest> validRequests = new ArrayList<>();
        for (WordCreateRequest request : requests) {
            if (wordRepository.existsByWordAndUserAndDeletedFalse(request.getWord(), user)) {
                skipped++;
            } else {
                validRequests.add(request);
            }
        }

        // 2. 유효한 단어들을 Word 엔티티로 생성 (아직 보강 없이)
        Map<String, Word> wordMap = new LinkedHashMap<>();
        for (WordCreateRequest request : validRequests) {
            wordMap.put(request.getWord(), new Word(user, request.getWord(), request.getMeaning()));
        }

        // 3. 배치 분할 (BULK_ENRICHMENT_BATCH_SIZE개씩) + Gemini API 호출
        List<List<WordCreateRequest>> batches = partition(validRequests, BULK_ENRICHMENT_BATCH_SIZE);
        for (List<WordCreateRequest> batch : batches) {
            try {
                String prompt = buildBulkEnrichmentPrompt(batch);
                BulkWordEnrichment result = geminiClient.generateContent(prompt, BulkWordEnrichment.class);

                // 응답의 word 필드로 매핑
                int enrichedCount = 0;
                for (BulkWordEnrichment.Item item : result.getEnrichments()) {
                    Word word = wordMap.get(item.getWord());
                    if (word != null) {
                        word.enrich(item.getPartOfSpeech(), item.getPronunciation(),
                                item.getSynonyms(), item.getTip());
                        enrichedCount++;
                    }
                }
                // 매핑 실패한 단어 수 = 배치 크기 - 매핑 성공 수
                enrichmentFailed += batch.size() - enrichedCount;
            } catch (Exception e) {
                log.warn("벌크 보강 배치 실패 ({})건: {}", batch.size(), e.getMessage());
                enrichmentFailed += batch.size();
            }
        }

        // 4. 저장 + 학습기록 + 복습아이템
        StudyRecord record = studyRecordService.getOrCreateTodayRecord(user);
        List<WordResponse> words = new ArrayList<>();
        for (Word word : wordMap.values()) {
            Word savedWord = wordRepository.save(word);
            studyRecordService.addItem(record, "WORD", savedWord.getId());
            reviewItemService.createWordReviewItems(user, savedWord.getId());
            words.add(WordResponse.from(savedWord));
        }

        return new BulkCreateResponse(words.size(), skipped, enrichmentFailed, words);
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            batches.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return batches;
    }

    @Transactional(readOnly = true)
    public Page<WordListResponse> getList(User user, String search, String partOfSpeech, boolean importantOnly,
                                           String sort, Pageable pageable) {
        Sort sorting = "name".equals(sort)
                ? Sort.by(Sort.Direction.ASC, "word")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sorting);

        return wordRepository.findAllWithFilters(user, search, partOfSpeech, importantOnly, sortedPageable)
                .map(WordListResponse::from);
    }

    @Transactional(readOnly = true)
    public WordDetailResponse getDetail(User user, Long id) {
        Word word = wordRepository.findByIdAndUserAndDeletedFalse(id, user)
                .orElseThrow(() -> new NotFoundException("단어를 찾을 수 없습니다: " + id));

        List<String> examples = List.of();

        return WordDetailResponse.from(word, examples);
    }

    @Transactional
    public WordResponse toggleImportant(User user, Long id) {
        Word word = wordRepository.findByIdAndUserAndDeletedFalse(id, user)
                .orElseThrow(() -> new NotFoundException("단어를 찾을 수 없습니다: " + id));

        word.toggleImportant();
        return WordResponse.from(word);
    }

    @Transactional
    public void delete(User user, Long id) {
        Word word = wordRepository.findByIdAndUserAndDeletedFalse(id, user)
                .orElseThrow(() -> new NotFoundException("단어를 찾을 수 없습니다: " + id));

        word.softDelete();
        reviewItemRepository.softDeleteByUserAndItemTypeAndItemId(user, "WORD", id);
    }

    // 벌크 보강용 배열 프롬프트 빌더 (package-private: 테스트 접근 가능)
    String buildBulkEnrichmentPrompt(List<WordCreateRequest> requests) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 영어 단어들에 대해 JSON 형식으로 보강 정보를 제공해주세요.\n\n");

        // 단어 배열을 JSON으로 포함
        prompt.append("단어 목록:\n");
        String wordJson = requests.stream()
                .map(r -> "{\"word\":\"" + r.getWord() + "\", \"meaning\":\"" + r.getMeaning() + "\"}")
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
        prompt.append(wordJson).append("\n\n");

        prompt.append("아래 JSON 형식으로 응답해주세요:\n");
        prompt.append("{\n");
        prompt.append("  \"enrichments\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"word\": \"원본 단어 (위 목록의 word 값 그대로)\",\n");
        prompt.append("      \"partOfSpeech\": \"품사 (예: 명사, 동사, 형용사)\",\n");
        prompt.append("      \"pronunciation\": \"IPA 발음 기호\",\n");
        prompt.append("      \"synonyms\": \"유의어 (쉼표로 구분)\",\n");
        prompt.append("      \"tip\": \"학습 팁 또는 암기 도움말\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("⚠️ 입력된 모든 단어에 대해 빠짐없이 응답하세요. enrichments 배열의 각 항목에 word 필드를 반드시 포함하세요.");

        return prompt.toString();
    }

    private String buildEnrichmentPrompt(String word, String meaning) {
        return "다음 영어 단어에 대해 JSON 형식으로 보강 정보를 제공해주세요.\n" +
                "단어: " + word + "\n" +
                "뜻: " + meaning + "\n\n" +
                "아래 JSON 형식으로 응답해주세요:\n" +
                "{\n" +
                "  \"partOfSpeech\": \"품사 (예: 명사, 동사, 형용사)\",\n" +
                "  \"pronunciation\": \"IPA 발음 기호\",\n" +
                "  \"synonyms\": \"유의어 (쉼표로 구분)\",\n" +
                "  \"tip\": \"학습 팁 또는 암기 도움말\"\n" +
                "}";
    }
}

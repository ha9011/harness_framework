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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordService {

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

        int saved = 0;
        int skipped = 0;
        int enrichmentFailed = 0;
        List<WordResponse> words = new ArrayList<>();

        StudyRecord record = studyRecordService.getOrCreateTodayRecord(user);

        for (WordCreateRequest request : requests) {
            if (wordRepository.existsByWordAndUserAndDeletedFalse(request.getWord(), user)) {
                skipped++;
                continue;
            }

            Word word = new Word(user, request.getWord(), request.getMeaning());

            // AI 보강 시도
            boolean enriched = false;
            try {
                String prompt = buildEnrichmentPrompt(request.getWord(), request.getMeaning());
                WordEnrichment enrichment = geminiClient.generateContent(prompt, WordEnrichment.class);
                word.enrich(enrichment.getPartOfSpeech(), enrichment.getPronunciation(),
                        enrichment.getSynonyms(), enrichment.getTip());
                enriched = true;
            } catch (Exception e) {
                log.warn("벌크 단어 보강 실패: {} - {}", request.getWord(), e.getMessage());
                enrichmentFailed++;
            }

            Word savedWord = wordRepository.save(word);

            studyRecordService.addItem(record, "WORD", savedWord.getId());
            reviewItemService.createWordReviewItems(user, savedWord.getId());

            words.add(WordResponse.from(savedWord));
            saved++;
        }

        return new BulkCreateResponse(saved, skipped, enrichmentFailed, words);
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

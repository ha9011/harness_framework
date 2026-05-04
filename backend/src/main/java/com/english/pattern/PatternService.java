package com.english.pattern;

import com.english.config.DuplicateException;
import com.english.config.GeminiClient;
import com.english.config.InvalidImageException;
import com.english.config.NotFoundException;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemService;
import com.english.study.StudyRecord;
import com.english.study.StudyRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatternService {

    private final PatternRepository patternRepository;
    private final GeminiClient geminiClient;
    private final StudyRecordService studyRecordService;
    private final ReviewItemService reviewItemService;
    private final ReviewItemRepository reviewItemRepository;

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Transactional
    public PatternResponse create(PatternCreateRequest request) {
        if (patternRepository.existsByTemplateAndDeletedFalse(request.getTemplate())) {
            throw new DuplicateException("이미 등록된 패턴입니다: " + request.getTemplate());
        }

        Pattern pattern = new Pattern(request.getTemplate(), request.getDescription());

        if (request.getExamples() != null) {
            for (int i = 0; i < request.getExamples().size(); i++) {
                PatternCreateRequest.ExampleRequest ex = request.getExamples().get(i);
                pattern.addExample(ex.getSentence(), ex.getTranslation(), i);
            }
        }

        Pattern saved = patternRepository.save(pattern);

        // 학습 기록 연동
        StudyRecord record = studyRecordService.getOrCreateTodayRecord();
        studyRecordService.addItem(record, "PATTERN", saved.getId());

        // 복습 아이템 생성
        reviewItemService.createPatternReviewItems(saved.getId());

        return PatternResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<PatternListResponse> getList(Pageable pageable) {
        return patternRepository.findByDeletedFalse(pageable)
                .map(PatternListResponse::from);
    }

    @Transactional(readOnly = true)
    public PatternDetailResponse getDetail(Long id) {
        Pattern pattern = patternRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("패턴을 찾을 수 없습니다: " + id));

        return PatternDetailResponse.from(pattern);
    }

    @Transactional
    public void delete(Long id) {
        Pattern pattern = patternRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("패턴을 찾을 수 없습니다: " + id));

        pattern.softDelete();
        reviewItemRepository.softDeleteByItemTypeAndItemId("PATTERN", id);
    }

    public PatternExtractResponse extractFromImage(MultipartFile image) {
        validateImageType(image);

        try {
            byte[] imageData = image.getBytes();
            String mimeType = image.getContentType();

            String prompt = "이 이미지에서 영어 패턴을 추출해주세요.\n" +
                    "아래 JSON 형식으로 응답해주세요:\n" +
                    "{\n" +
                    "  \"template\": \"패턴 템플릿 (예: I want to ~)\",\n" +
                    "  \"description\": \"패턴 설명 (한국어)\",\n" +
                    "  \"examples\": [\n" +
                    "    {\"sentence\": \"예문 영어\", \"translation\": \"예문 한국어 해석\"}\n" +
                    "  ]\n" +
                    "}";

            return geminiClient.generateContentWithImage(imageData, mimeType, prompt, PatternExtractResponse.class);
        } catch (IOException e) {
            throw new InvalidImageException("이미지 파일을 읽을 수 없습니다");
        }
    }

    public WordExtractResponse extractWordsFromImage(MultipartFile image) {
        validateImageType(image);

        try {
            byte[] imageData = image.getBytes();
            String mimeType = image.getContentType();

            String prompt = "이 이미지에서 영어 단어와 뜻을 추출해주세요.\n" +
                    "아래 JSON 형식으로 응답해주세요:\n" +
                    "{\n" +
                    "  \"words\": [\n" +
                    "    {\"word\": \"영어 단어\", \"meaning\": \"한국어 뜻\"}\n" +
                    "  ]\n" +
                    "}";

            return geminiClient.generateContentWithImage(imageData, mimeType, prompt, WordExtractResponse.class);
        } catch (IOException e) {
            throw new InvalidImageException("이미지 파일을 읽을 수 없습니다");
        }
    }

    private void validateImageType(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidImageException("지원하지 않는 이미지 형식입니다. JPEG, PNG, WebP, GIF만 지원합니다.");
        }
    }
}

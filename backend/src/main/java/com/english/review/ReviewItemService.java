package com.english.review;

import com.english.auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewItemService {

    private final ReviewItemRepository reviewItemRepository;

    /**
     * 단어/패턴 등록 시 RECOGNITION + RECALL 2개 생성
     */
    @Transactional
    public void createWordReviewItems(User user, Long wordId) {
        reviewItemRepository.save(new ReviewItem(user, "WORD", wordId, "RECOGNITION"));
        reviewItemRepository.save(new ReviewItem(user, "WORD", wordId, "RECALL"));
    }

    @Transactional
    public void createPatternReviewItems(User user, Long patternId) {
        reviewItemRepository.save(new ReviewItem(user, "PATTERN", patternId, "RECOGNITION"));
        reviewItemRepository.save(new ReviewItem(user, "PATTERN", patternId, "RECALL"));
    }

    /**
     * 예문 등록 시 RECOGNITION만 1개 생성
     */
    @Transactional
    public void createSentenceReviewItem(User user, Long sentenceId) {
        reviewItemRepository.save(new ReviewItem(user, "SENTENCE", sentenceId, "RECOGNITION"));
    }
}

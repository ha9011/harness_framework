package com.english.word;

import com.english.auth.AuthException;
import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.generate.GeminiClient;
import com.english.generate.GeminiClientException;
import com.english.generate.GeminiWordEnrichment;
import com.english.generate.GeminiWordInput;
import com.english.review.ReviewItemCreationService;
import com.english.review.ReviewItemType;
import com.english.study.StudyItemType;
import com.english.study.StudyRecordService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WordService {

	private static final String DUPLICATE_WORD_REASON = "이미 등록된 단어입니다";

	private final WordRepository wordRepository;
	private final UserRepository userRepository;
	private final StudyRecordService studyRecordService;
	private final ReviewItemCreationService reviewItemCreationService;
	private final GeminiClient geminiClient;

	public WordService(
            WordRepository wordRepository,
            UserRepository userRepository,
            StudyRecordService studyRecordService,
            ReviewItemCreationService reviewItemCreationService, GeminiClient geminiClient
    ) {
		this.wordRepository = wordRepository;
		this.userRepository = userRepository;
		this.studyRecordService = studyRecordService;
		this.reviewItemCreationService = reviewItemCreationService;
        this.geminiClient = geminiClient;
    }

	@Transactional
	public WordResponse create(Long userId, WordCreateRequest request) {
		User user = findUser(userId);
		String wordText = requiredText(request.word(), "단어");
		assertNoDuplicate(userId, wordText);

		Word word = new Word(user, wordText, requiredText(request.meaning(), "뜻"));
		word.update(
				wordText,
				requiredText(request.meaning(), "뜻"),
				optionalText(request.partOfSpeech()),
				optionalText(request.pronunciation()),
				optionalText(request.synonyms()),
				optionalText(request.tip()));
		Word saved = wordRepository.save(word);
		LocalDate today = LocalDate.now();
		studyRecordService.recordLearning(user, StudyItemType.WORD, saved.getId(), today);
		reviewItemCreationService.createWordReviewItems(user, saved.getId(), today);

		// --- 여기부터 추가 ---
		try {
			var input = new GeminiWordInput(saved.getId(), saved.getWord(), saved.getMeaning());
			List<GeminiWordEnrichment> enrichments = geminiClient.enrichWords(List.of(input));
			if (!enrichments.isEmpty()) {
				GeminiWordEnrichment e = enrichments.get(0);
				saved.update(
						saved.getWord(),
						saved.getMeaning(),
						e.partOfSpeech(),
						e.pronunciation(),
						e.synonyms(),
						e.tip());
			}
		} catch (GeminiClientException ignored) {
			// 보강 실패해도 단어 저장은 유지
		}
		// --- 여기까지 ---


		return WordResponse.from(saved);
	}

	@Transactional
	public WordBulkSaveResult saveBulk(Long userId, List<WordCreateRequest> requests) {
		List<WordResponse> saved = new ArrayList<>();
		List<WordBulkSkippedItem> skipped = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		for (WordCreateRequest request : requests) {
			String wordText = requiredText(request.word(), "단어");
			String normalizedWord = duplicateKey(wordText);

			if (wordRepository.existsByUserIdAndWordIgnoreCaseAndDeletedFalse(userId, wordText)
					|| !seen.add(normalizedWord)) {
				skipped.add(new WordBulkSkippedItem(wordText, DUPLICATE_WORD_REASON));
				continue;
			}

			saved.add(create(userId, new WordCreateRequest(
					wordText,
					request.meaning(),
					request.partOfSpeech(),
					request.pronunciation(),
					request.synonyms(),
					request.tip())));
		}

		return new WordBulkSaveResult(saved, skipped);
	}

	@Transactional(readOnly = true)
	public WordResponse get(Long userId, Long wordId) {
		return WordResponse.from(findOwnedActiveWord(userId, wordId));
	}

	@Transactional
	public WordResponse update(Long userId, Long wordId, WordUpdateRequest request) {
		Word word = findOwnedActiveWord(userId, wordId);
		String wordText = requiredText(request.word(), "단어");
		wordRepository.findByUserIdAndWordIgnoreCaseAndDeletedFalse(userId, wordText)
				.filter(existing -> !existing.getId().equals(word.getId()))
				.ifPresent(existing -> {
					throw WordException.duplicate();
				});

		word.update(
				wordText,
				requiredText(request.meaning(), "뜻"),
				optionalText(request.partOfSpeech()),
				optionalText(request.pronunciation()),
				optionalText(request.synonyms()),
				optionalText(request.tip()));
		return WordResponse.from(word);
	}

	@Transactional
	public WordResponse toggleImportant(Long userId, Long wordId) {
		Word word = findOwnedActiveWord(userId, wordId);
		word.toggleImportant();
		return WordResponse.from(word);
	}

	@Transactional
	public void delete(Long userId, Long wordId) {
		Word word = findOwnedActiveWord(userId, wordId);
		word.softDelete();
		reviewItemCreationService.softDeleteReviewItems(word.getUser(), ReviewItemType.WORD, word.getId());
	}

	@Transactional(readOnly = true)
	public Page<WordResponse> search(Long userId, WordSearchCondition condition, Pageable pageable) {
		return wordRepository.findAll(searchSpec(userId, condition), pageable)
				.map(WordResponse::from);
	}

	private Word findOwnedActiveWord(Long userId, Long wordId) {
		Word word = wordRepository.findById(wordId)
				.orElseThrow(WordException::notFound);
		if (!word.getUser().getId().equals(userId)) {
			throw WordException.forbidden();
		}
		if (word.isDeleted()) {
			throw WordException.notFound();
		}
		return word;
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(AuthException::invalidToken);
	}

	private void assertNoDuplicate(Long userId, String wordText) {
		if (wordRepository.existsByUserIdAndWordIgnoreCaseAndDeletedFalse(userId, wordText)) {
			throw WordException.duplicate();
		}
	}

	private static Specification<Word> searchSpec(Long userId, WordSearchCondition condition) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
			predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

			if (condition != null && hasText(condition.search())) {
				String keyword = "%" + condition.search().trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("word")), keyword),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("meaning")), keyword)));
			}
			if (condition != null && hasText(condition.partOfSpeech())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("partOfSpeech")),
						condition.partOfSpeech().trim().toLowerCase(Locale.ROOT)));
			}
			if (condition != null && Boolean.TRUE.equals(condition.importantOnly())) {
				predicates.add(criteriaBuilder.isTrue(root.get("important")));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String requiredText(String value, String label) {
		if (!hasText(value)) {
			throw WordException.badRequest(label + "은 필수입니다");
		}
		return value.trim();
	}

	private static String optionalText(String value) {
		return hasText(value) ? value.trim() : null;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static String duplicateKey(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}
}

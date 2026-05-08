package com.english.pattern;

import com.english.auth.AuthException;
import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.review.ReviewItemCreationService;
import com.english.review.ReviewItemType;
import com.english.study.StudyItemType;
import com.english.study.StudyRecordService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatternService {

	private final PatternRepository patternRepository;
	private final PatternExampleRepository patternExampleRepository;
	private final UserRepository userRepository;
	private final StudyRecordService studyRecordService;
	private final ReviewItemCreationService reviewItemCreationService;

	public PatternService(
			PatternRepository patternRepository,
			PatternExampleRepository patternExampleRepository,
			UserRepository userRepository,
			StudyRecordService studyRecordService,
			ReviewItemCreationService reviewItemCreationService
	) {
		this.patternRepository = patternRepository;
		this.patternExampleRepository = patternExampleRepository;
		this.userRepository = userRepository;
		this.studyRecordService = studyRecordService;
		this.reviewItemCreationService = reviewItemCreationService;
	}

	@Transactional
	public PatternResponse create(Long userId, PatternCreateRequest request) {
		User user = findUser(userId);
		String template = requiredText(request.template(), "패턴");
		assertNoDuplicate(userId, template);

		Pattern pattern = patternRepository.save(new Pattern(
				user,
				template,
				requiredText(request.description(), "설명")));
		saveExamples(pattern, request.examples());

		LocalDate today = LocalDate.now();
		studyRecordService.recordLearning(user, StudyItemType.PATTERN, pattern.getId(), today);
		reviewItemCreationService.createPatternReviewItems(user, pattern.getId(), today);

		return response(pattern);
	}

	@Transactional(readOnly = true)
	public PatternResponse get(Long userId, Long patternId) {
		return response(findOwnedActivePattern(userId, patternId));
	}

	@Transactional
	public PatternResponse update(Long userId, Long patternId, PatternUpdateRequest request) {
		Pattern pattern = findOwnedActivePattern(userId, patternId);
		String template = requiredText(request.template(), "패턴");
		patternRepository.findByUserIdAndTemplateIgnoreCaseAndDeletedFalse(userId, template)
				.filter(existing -> !existing.getId().equals(pattern.getId()))
				.ifPresent(existing -> {
					throw PatternException.duplicate();
				});

		pattern.update(template, requiredText(request.description(), "설명"));
		patternExampleRepository.deleteByPatternId(pattern.getId());
		saveExamples(pattern, request.examples());

		return response(pattern);
	}

	@Transactional
	public void delete(Long userId, Long patternId) {
		Pattern pattern = findOwnedActivePattern(userId, patternId);
		pattern.softDelete();
		reviewItemCreationService.softDeleteReviewItems(pattern.getUser(), ReviewItemType.PATTERN, pattern.getId());
	}

	@Transactional(readOnly = true)
	public Page<PatternResponse> search(Long userId, Pageable pageable) {
		return patternRepository.findByUserIdAndDeletedFalse(userId, pageable)
				.map(this::response);
	}

	private Pattern findOwnedActivePattern(Long userId, Long patternId) {
		Pattern pattern = patternRepository.findById(patternId)
				.orElseThrow(PatternException::notFound);
		if (!pattern.getUser().getId().equals(userId)) {
			throw PatternException.forbidden();
		}
		if (pattern.isDeleted()) {
			throw PatternException.notFound();
		}
		return pattern;
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(AuthException::invalidToken);
	}

	private void assertNoDuplicate(Long userId, String template) {
		if (patternRepository.existsByUserIdAndTemplateIgnoreCaseAndDeletedFalse(userId, template)) {
			throw PatternException.duplicate();
		}
	}

	private void saveExamples(Pattern pattern, List<PatternExampleRequest> examples) {
		for (int index = 0; index < examples.size(); index++) {
			PatternExampleRequest example = examples.get(index);
			patternExampleRepository.save(new PatternExample(
					pattern,
					index + 1,
					requiredText(example.sentence(), "예문"),
					requiredText(example.translation(), "해석")));
		}
	}

	private PatternResponse response(Pattern pattern) {
		return PatternResponse.from(
				pattern,
				patternExampleRepository.findByPatternIdOrderBySortOrderAsc(pattern.getId()));
	}

	private static String requiredText(String value, String label) {
		if (!hasText(value)) {
			throw PatternException.badRequest(label + "은 필수입니다");
		}
		return value.trim();
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}

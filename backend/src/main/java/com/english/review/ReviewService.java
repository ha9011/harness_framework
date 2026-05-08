package com.english.review;

import com.english.generate.GeneratedSentence;
import com.english.generate.GeneratedSentenceRepository;
import com.english.generate.GeneratedSentenceWordRepository;
import com.english.generate.SentenceSituationRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternExampleRepository;
import com.english.pattern.PatternRepository;
import com.english.setting.UserSetting;
import com.english.setting.UserSettingRepository;
import com.english.word.Word;
import com.english.word.WordRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

	private static final int DEFAULT_DAILY_REVIEW_COUNT = 10;
	private static final int WORD_EXAMPLE_LIMIT = 3;

	private final ReviewItemRepository reviewItemRepository;
	private final ReviewLogRepository reviewLogRepository;
	private final UserSettingRepository userSettingRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;
	private final PatternExampleRepository patternExampleRepository;
	private final GeneratedSentenceRepository generatedSentenceRepository;
	private final GeneratedSentenceWordRepository generatedSentenceWordRepository;
	private final SentenceSituationRepository sentenceSituationRepository;
	private final Sm2Scheduler sm2Scheduler;

	public ReviewService(
			ReviewItemRepository reviewItemRepository,
			ReviewLogRepository reviewLogRepository,
			UserSettingRepository userSettingRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository,
			PatternExampleRepository patternExampleRepository,
			GeneratedSentenceRepository generatedSentenceRepository,
			GeneratedSentenceWordRepository generatedSentenceWordRepository,
			SentenceSituationRepository sentenceSituationRepository,
			Sm2Scheduler sm2Scheduler
	) {
		this.reviewItemRepository = reviewItemRepository;
		this.reviewLogRepository = reviewLogRepository;
		this.userSettingRepository = userSettingRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
		this.patternExampleRepository = patternExampleRepository;
		this.generatedSentenceRepository = generatedSentenceRepository;
		this.generatedSentenceWordRepository = generatedSentenceWordRepository;
		this.sentenceSituationRepository = sentenceSituationRepository;
		this.sm2Scheduler = sm2Scheduler;
	}

	@Transactional(readOnly = true)
	public List<ReviewCardResponse> getTodayReviews(
			Long userId,
			ReviewItemType type,
			Collection<Long> excludeIds
	) {
		Long requestedUserId = requireId(userId, "사용자 ID");
		if (type == null) {
			throw ReviewException.badRequest("복습 타입은 필수입니다");
		}

		Set<Long> excluded = excludeIds(excludeIds);
		int dailyReviewCount = dailyReviewCount(requestedUserId);
		List<ReviewCardResponse> cards = reviewItemRepository.findDueReviewItems(
						requestedUserId,
						type,
						LocalDate.now(),
						ReviewResult.HARD)
				.stream()
				.filter(item -> !excluded.contains(item.getId()))
				.map(item -> assembleCard(requestedUserId, item))
				.flatMap(Optional::stream)
				.limit(dailyReviewCount)
				.toList();
		List<ReviewCardResponse> shuffled = new ArrayList<>(cards);
		Collections.shuffle(shuffled);
		return shuffled;
	}

	@Transactional
	public ReviewResultResponse recordResult(Long userId, Long reviewItemId, ReviewResult result) {
		Long requestedUserId = requireId(userId, "사용자 ID");
		if (result == null) {
			throw ReviewException.badRequest("복습 결과는 필수입니다");
		}
		ReviewItem reviewItem = reviewItemRepository.findById(requireId(reviewItemId, "복습 항목 ID"))
				.orElseThrow(ReviewException::notFound);
		if (!reviewItem.getUser().getId().equals(requestedUserId)) {
			throw ReviewException.forbidden();
		}
		if (reviewItem.isDeleted()) {
			throw ReviewException.notFound();
		}

		Sm2Schedule schedule = sm2Scheduler.schedule(
				reviewItem.getIntervalDays(),
				reviewItem.getEaseFactor(),
				result,
				LocalDate.now());
		reviewItem.recordResult(result, schedule, Instant.now());
		reviewLogRepository.save(new ReviewLog(reviewItem, result));

		return new ReviewResultResponse(schedule.nextReviewDate(), schedule.intervalDays());
	}

	private Optional<ReviewCardResponse> assembleCard(Long userId, ReviewItem reviewItem) {
		return switch (reviewItem.getItemType()) {
			case WORD -> assembleWordCard(userId, reviewItem);
			case PATTERN -> assemblePatternCard(userId, reviewItem);
			case SENTENCE -> assembleSentenceCard(userId, reviewItem);
		};
	}

	private Optional<ReviewCardResponse> assembleWordCard(Long userId, ReviewItem reviewItem) {
		return wordRepository.findById(reviewItem.getItemId())
				.filter(word -> isOwnedBy(word, userId))
				.filter(word -> !word.isDeleted())
				.map(word -> switch (reviewItem.getDirection()) {
					case RECOGNITION -> new ReviewCardResponse(
							reviewItem.getId(),
							reviewItem.getItemType(),
							reviewItem.getDirection(),
							new ReviewCardFront(word.getWord(), null),
							new WordRecognitionReviewBack(
									word.getMeaning(),
									word.getPronunciation(),
									word.getTip(),
									wordExamples(userId, word.getId())));
					case RECALL -> new ReviewCardResponse(
							reviewItem.getId(),
							reviewItem.getItemType(),
							reviewItem.getDirection(),
							new ReviewCardFront(word.getMeaning(), null),
							new WordRecallReviewBack(
									word.getWord(),
									word.getPronunciation(),
									word.getTip()));
				});
	}

	private Optional<ReviewCardResponse> assemblePatternCard(Long userId, ReviewItem reviewItem) {
		return patternRepository.findById(reviewItem.getItemId())
				.filter(pattern -> isOwnedBy(pattern, userId))
				.filter(pattern -> !pattern.isDeleted())
				.map(pattern -> {
					List<ReviewPatternExampleResponse> examples = patternExamples(pattern.getId());
					return switch (reviewItem.getDirection()) {
						case RECOGNITION -> new ReviewCardResponse(
								reviewItem.getId(),
								reviewItem.getItemType(),
								reviewItem.getDirection(),
								new ReviewCardFront(pattern.getTemplate(), null),
								new PatternRecognitionReviewBack(pattern.getDescription(), examples));
						case RECALL -> new ReviewCardResponse(
								reviewItem.getId(),
								reviewItem.getItemType(),
								reviewItem.getDirection(),
								new ReviewCardFront(pattern.getDescription(), null),
								new PatternRecallReviewBack(pattern.getTemplate(), examples));
					};
				});
	}

	private Optional<ReviewCardResponse> assembleSentenceCard(Long userId, ReviewItem reviewItem) {
		if (reviewItem.getDirection() != ReviewDirection.RECOGNITION) {
			return Optional.empty();
		}
		return generatedSentenceRepository.findById(reviewItem.getItemId())
				.filter(sentence -> isOwnedBy(sentence, userId))
				.filter(sentence -> !sentence.isDeleted())
				.map(sentence -> new ReviewCardResponse(
						reviewItem.getId(),
						reviewItem.getItemType(),
						reviewItem.getDirection(),
						new ReviewCardFront(sentence.getSentence(), randomSituation(sentence.getId())),
						new SentenceRecognitionReviewBack(
								sentence.getTranslation(),
								sentence.getPattern() == null ? null : sentence.getPattern().getTemplate(),
								sentenceWords(sentence.getId()))));
	}

	private List<String> wordExamples(Long userId, Long wordId) {
		return generatedSentenceWordRepository.findAll()
				.stream()
				.filter(sentenceWord -> sentenceWord.getWord().getId().equals(wordId))
				.map(sentenceWord -> sentenceWord.getSentence())
				.filter(sentence -> isOwnedBy(sentence, userId))
				.filter(sentence -> !sentence.isDeleted())
				.map(GeneratedSentence::getSentence)
				.distinct()
				.limit(WORD_EXAMPLE_LIMIT)
				.toList();
	}

	private List<ReviewPatternExampleResponse> patternExamples(Long patternId) {
		return patternExampleRepository.findByPatternIdOrderBySortOrderAsc(patternId)
				.stream()
				.map(example -> new ReviewPatternExampleResponse(example.getSentence(), example.getTranslation()))
				.toList();
	}

	private String randomSituation(Long sentenceId) {
		List<String> situations = sentenceSituationRepository.findAll()
				.stream()
				.filter(situation -> situation.getSentence().getId().equals(sentenceId))
				.map(situation -> situation.getSituation())
				.toList();
		if (situations.isEmpty()) {
			return null;
		}
		return situations.get(ThreadLocalRandom.current().nextInt(situations.size()));
	}

	private List<String> sentenceWords(Long sentenceId) {
		return generatedSentenceWordRepository.findAll()
				.stream()
				.filter(sentenceWord -> sentenceWord.getSentence().getId().equals(sentenceId))
				.map(sentenceWord -> sentenceWord.getWord().getWord())
				.distinct()
				.toList();
	}

	private int dailyReviewCount(Long userId) {
		return userSettingRepository.findAll()
				.stream()
				.filter(setting -> setting.getUser().getId().equals(userId))
				.findFirst()
				.map(UserSetting::getDailyReviewCount)
				.orElse(DEFAULT_DAILY_REVIEW_COUNT);
	}

	private static Set<Long> excludeIds(Collection<Long> excludeIds) {
		if (excludeIds == null || excludeIds.isEmpty()) {
			return Set.of();
		}
		Set<Long> result = new HashSet<>();
		for (Long excludeId : excludeIds) {
			if (excludeId != null) {
				result.add(excludeId);
			}
		}
		return result;
	}

	private static boolean isOwnedBy(Word word, Long userId) {
		return word.getUser().getId().equals(userId);
	}

	private static boolean isOwnedBy(Pattern pattern, Long userId) {
		return pattern.getUser().getId().equals(userId);
	}

	private static boolean isOwnedBy(GeneratedSentence sentence, Long userId) {
		return sentence.getUser().getId().equals(userId);
	}

	private static Long requireId(Long value, String label) {
		if (value == null || value <= 0) {
			throw ReviewException.badRequest(label + "는 필수입니다");
		}
		return value;
	}
}

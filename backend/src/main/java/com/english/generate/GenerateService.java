package com.english.generate;

import com.english.auth.AuthException;
import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.pattern.Pattern;
import com.english.pattern.PatternRepository;
import com.english.review.ReviewDirection;
import com.english.review.ReviewItem;
import com.english.review.ReviewItemCreationService;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemType;
import com.english.word.Word;
import com.english.word.WordRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerateService {

	private static final int MAX_CANDIDATES = 50;

	private final GeminiClient geminiClient;
	private final UserRepository userRepository;
	private final WordRepository wordRepository;
	private final PatternRepository patternRepository;
	private final GenerationHistoryRepository generationHistoryRepository;
	private final GeneratedSentenceRepository generatedSentenceRepository;
	private final GeneratedSentenceWordRepository generatedSentenceWordRepository;
	private final SentenceSituationRepository sentenceSituationRepository;
	private final ReviewItemCreationService reviewItemCreationService;
	private final ReviewItemRepository reviewItemRepository;

	public GenerateService(
			GeminiClient geminiClient,
			UserRepository userRepository,
			WordRepository wordRepository,
			PatternRepository patternRepository,
			GenerationHistoryRepository generationHistoryRepository,
			GeneratedSentenceRepository generatedSentenceRepository,
			GeneratedSentenceWordRepository generatedSentenceWordRepository,
			SentenceSituationRepository sentenceSituationRepository,
			ReviewItemCreationService reviewItemCreationService,
			ReviewItemRepository reviewItemRepository
	) {
		this.geminiClient = geminiClient;
		this.userRepository = userRepository;
		this.wordRepository = wordRepository;
		this.patternRepository = patternRepository;
		this.generationHistoryRepository = generationHistoryRepository;
		this.generatedSentenceRepository = generatedSentenceRepository;
		this.generatedSentenceWordRepository = generatedSentenceWordRepository;
		this.sentenceSituationRepository = sentenceSituationRepository;
		this.reviewItemCreationService = reviewItemCreationService;
		this.reviewItemRepository = reviewItemRepository;
	}

	@Transactional
	public GenerateResponse generate(Long userId, String level, int count) {
		User user = findUser(userId);
		GenerationOptions options = validateOptions(level, count);
		List<Word> words = selectWordCandidates(userId);
		if (words.isEmpty()) {
			throw GenerateException.noWords();
		}
		List<Pattern> patterns = selectPatternCandidates(userId);
		if (patterns.isEmpty()) {
			throw GenerateException.noPatterns();
		}

		return generateAndPersist(user, options, null, null, words, patterns);
	}

	@Transactional
	public GenerateResponse generateForWord(Long userId, Long wordId, String level, int count) {
		User user = findUser(userId);
		GenerationOptions options = validateOptions(level, count);
		Word word = findOwnedActiveWord(userId, wordId);

		return generateAndPersist(user, options, word, null, List.of(word), List.of());
	}

	@Transactional
	public GenerateResponse generateForPattern(Long userId, Long patternId, String level, int count) {
		User user = findUser(userId);
		GenerationOptions options = validateOptions(level, count);
		Pattern pattern = findOwnedActivePattern(userId, patternId);
		List<Word> words = selectWordCandidates(userId);
		if (words.isEmpty()) {
			throw GenerateException.noWords();
		}

		return generateAndPersist(user, options, null, pattern, words, List.of(pattern));
	}

	@Transactional(readOnly = true)
	public Page<GenerationHistoryResponse> getHistory(Long userId, Pageable pageable) {
		findUser(userId);
		return generationHistoryRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
				.map(GenerationHistoryResponse::from);
	}

	private GenerateResponse generateAndPersist(
			User user,
			GenerationOptions options,
			Word historyWord,
			Pattern historyPattern,
			List<Word> wordCandidates,
			List<Pattern> patternCandidates
	) {
		List<GeminiGeneratedSentence> generatedSentences = requestSentences(options, wordCandidates, patternCandidates);
		GenerationHistory history = generationHistoryRepository.save(new GenerationHistory(
				user,
				options.level(),
				options.count(),
				generatedSentences.size(),
				historyWord,
				historyPattern));
		Map<Long, Word> validWords = wordCandidates.stream()
				.collect(Collectors.toMap(
						Word::getId,
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));
		Map<Long, Pattern> validPatterns = patternCandidates.stream()
				.collect(Collectors.toMap(
						Pattern::getId,
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));
		List<GeneratedSentenceResponse> responses = generatedSentences.stream()
				.map(generatedSentence -> persistSentence(user, history, options.level(), generatedSentence, validWords, validPatterns))
				.toList();

		return new GenerateResponse(history.getId(), responses);
	}

	private List<GeminiGeneratedSentence> requestSentences(
			GenerationOptions options,
			List<Word> wordCandidates,
			List<Pattern> patternCandidates
	) {
		GeminiSentenceGenerationRequest request = new GeminiSentenceGenerationRequest(
				options.level(),
				options.count(),
				wordCandidates.stream()
						.map(word -> new GeminiSentenceWordCandidate(word.getId(), word.getWord(), word.getMeaning()))
						.toList(),
				patternCandidates.stream()
						.map(pattern -> new GeminiSentencePatternCandidate(
								pattern.getId(),
								pattern.getTemplate(),
								pattern.getDescription()))
						.toList());
		try {
			List<GeminiGeneratedSentence> generatedSentences = geminiClient.generateSentences(request);
			if (generatedSentences == null) {
				throw GenerateException.aiServiceError();
			}
			return generatedSentences;
		}
		catch (GeminiClientException exception) {
			throw GenerateException.aiServiceError();
		}
	}

	private GeneratedSentenceResponse persistSentence(
			User user,
			GenerationHistory history,
			String level,
			GeminiGeneratedSentence generatedSentence,
			Map<Long, Word> validWords,
			Map<Long, Pattern> validPatterns
	) {
		Pattern pattern = generatedSentence.patternId() == null ? null : validPatterns.get(generatedSentence.patternId());
		GeneratedSentence sentence = generatedSentenceRepository.save(new GeneratedSentence(
				user,
				history,
				pattern,
				generatedSentence.sentence(),
				generatedSentence.translation(),
				level));
		List<GeneratedSentenceWordResponse> words = saveSentenceWords(sentence, generatedSentence.wordIds(), validWords);
		List<String> situations = saveSituations(sentence, generatedSentence.situations());
		reviewItemCreationService.createSentenceReviewItem(user, sentence.getId(), LocalDate.now());

		return new GeneratedSentenceResponse(
				sentence.getId(),
				sentence.getSentence(),
				sentence.getTranslation(),
				situations,
				sentence.getLevel(),
				pattern == null ? null : GeneratedSentencePatternResponse.from(pattern),
				words);
	}

	private List<GeneratedSentenceWordResponse> saveSentenceWords(
			GeneratedSentence sentence,
			List<Long> responseWordIds,
			Map<Long, Word> validWords
	) {
		Set<Long> seen = new LinkedHashSet<>();
		List<GeneratedSentenceWordResponse> responses = new ArrayList<>();
		for (Long wordId : responseWordIds) {
			if (!seen.add(wordId)) {
				continue;
			}
			Word word = validWords.get(wordId);
			if (word == null) {
				continue;
			}
			generatedSentenceWordRepository.save(new GeneratedSentenceWord(sentence, word));
			responses.add(GeneratedSentenceWordResponse.from(word));
		}
		return responses;
	}

	private List<String> saveSituations(GeneratedSentence sentence, List<String> responseSituations) {
		List<String> situations = new ArrayList<>();
		for (String situation : responseSituations) {
			sentenceSituationRepository.save(new SentenceSituation(sentence, situation));
			situations.add(situation);
		}
		return situations;
	}

	private List<Word> selectWordCandidates(Long userId) {
		Map<Long, Integer> reviewCounts = recognitionReviewCounts(userId);
		return wordRepository.findByUserIdAndDeletedFalse(userId).stream()
				.map(word -> new WordCandidate(
						word,
						reviewCounts.getOrDefault(word.getId(), 0),
						ThreadLocalRandom.current().nextLong()))
				.sorted(Comparator
						.comparing((WordCandidate candidate) -> !candidate.word().isImportant())
						.thenComparingInt(WordCandidate::reviewCount)
						.thenComparingLong(WordCandidate::randomOrder))
				.limit(MAX_CANDIDATES)
				.map(WordCandidate::word)
				.toList();
	}

	private Map<Long, Integer> recognitionReviewCounts(Long userId) {
		return reviewItemRepository.findByUserIdAndItemTypeAndDirectionAndDeletedFalse(
						userId,
						ReviewItemType.WORD,
						ReviewDirection.RECOGNITION)
				.stream()
				.collect(Collectors.toMap(
						ReviewItem::getItemId,
						ReviewItem::getReviewCount,
						Math::min));
	}

	private List<Pattern> selectPatternCandidates(Long userId) {
		List<Pattern> patterns = new ArrayList<>(patternRepository.findByUserIdAndDeletedFalse(
						userId,
						Pageable.unpaged())
				.getContent());
		Collections.shuffle(patterns);
		return patterns.stream()
				.limit(MAX_CANDIDATES)
				.toList();
	}

	private Word findOwnedActiveWord(Long userId, Long wordId) {
		Long requestedWordId = requireId(wordId, "단어 ID");
		Word word = wordRepository.findById(requestedWordId)
				.orElseThrow(() -> GenerateException.notFound("단어를 찾을 수 없습니다"));
		if (!word.getUser().getId().equals(userId)) {
			throw GenerateException.forbidden("단어에 접근할 권한이 없습니다");
		}
		if (word.isDeleted()) {
			throw GenerateException.notFound("단어를 찾을 수 없습니다");
		}
		return word;
	}

	private Pattern findOwnedActivePattern(Long userId, Long patternId) {
		Long requestedPatternId = requireId(patternId, "패턴 ID");
		Pattern pattern = patternRepository.findById(requestedPatternId)
				.orElseThrow(() -> GenerateException.notFound("패턴을 찾을 수 없습니다"));
		if (!pattern.getUser().getId().equals(userId)) {
			throw GenerateException.forbidden("패턴에 접근할 권한이 없습니다");
		}
		if (pattern.isDeleted()) {
			throw GenerateException.notFound("패턴을 찾을 수 없습니다");
		}
		return pattern;
	}

	private User findUser(Long userId) {
		return userRepository.findById(requireId(userId, "사용자 ID"))
				.orElseThrow(AuthException::invalidToken);
	}

	private GenerationOptions validateOptions(String level, int count) {
		if (count <= 0) {
			throw GenerateException.badRequest("생성 개수는 양수여야 합니다");
		}
		return new GenerationOptions(requiredText(level, "난이도"), count);
	}

	private static String requiredText(String value, String label) {
		if (value == null || value.isBlank()) {
			throw GenerateException.badRequest(label + "은 필수입니다");
		}
		return value.trim();
	}

	private static Long requireId(Long value, String label) {
		if (value == null || value <= 0) {
			throw GenerateException.badRequest(label + "는 필수입니다");
		}
		return value;
	}

	private record GenerationOptions(String level, int count) {
	}

	private record WordCandidate(Word word, int reviewCount, long randomOrder) {
	}
}

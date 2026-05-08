package com.english.word;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.english.auth.User;
import com.english.auth.UserRepository;
import com.english.review.ReviewDirection;
import com.english.review.ReviewItem;
import com.english.review.ReviewItemRepository;
import com.english.review.ReviewItemType;
import com.english.study.StudyItemType;
import com.english.study.StudyRecord;
import com.english.study.StudyRecordItem;
import com.english.study.StudyRecordItemRepository;
import com.english.study.StudyRecordRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class WordServiceTest {

	private final WordService wordService;
	private final UserRepository userRepository;
	private final WordRepository wordRepository;
	private final StudyRecordRepository studyRecordRepository;
	private final StudyRecordItemRepository studyRecordItemRepository;
	private final ReviewItemRepository reviewItemRepository;

	@Autowired
	WordServiceTest(
			WordService wordService,
			UserRepository userRepository,
			WordRepository wordRepository,
			StudyRecordRepository studyRecordRepository,
			StudyRecordItemRepository studyRecordItemRepository,
			ReviewItemRepository reviewItemRepository
	) {
		this.wordService = wordService;
		this.userRepository = userRepository;
		this.wordRepository = wordRepository;
		this.studyRecordRepository = studyRecordRepository;
		this.studyRecordItemRepository = studyRecordItemRepository;
		this.reviewItemRepository = reviewItemRepository;
	}

	@Test
	void createWordStoresWordAndCreatesStudyRecordAndBidirectionalReviewItems() {
		User user = saveUser("word-create@example.com");

		WordResponse response = wordService.create(user.getId(), new WordCreateRequest(
				"make a bed",
				"침대를 정리하다",
				"phrase",
				"/meik a bed/",
				"tidy the bed",
				"make the bed도 같은 의미"));

		assertThat(response.id()).isNotNull();
		assertThat(response.word()).isEqualTo("make a bed");
		assertThat(response.meaning()).isEqualTo("침대를 정리하다");
		assertThat(response.important()).isFalse();

		Word saved = wordRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getUser().getId()).isEqualTo(user.getId());
		assertThat(saved.getPartOfSpeech()).isEqualTo("phrase");
		assertThat(saved.isDeleted()).isFalse();

		StudyRecord record = studyRecordRepository.findByUserIdAndStudyDate(user.getId(), LocalDate.now())
				.orElseThrow();
		assertThat(record.getDayNumber()).isEqualTo(1);
		List<StudyRecordItem> studyItems = studyRecordItemRepository.findByStudyRecordId(record.getId());
		assertThat(studyItems)
				.extracting(StudyRecordItem::getItemType, StudyRecordItem::getItemId)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(StudyItemType.WORD, response.id()));

		List<ReviewItem> reviewItems = reviewItemRepository.findByUserIdAndItemTypeAndItemId(
				user.getId(),
				ReviewItemType.WORD,
				response.id());
		assertThat(reviewItems).hasSize(2);
		assertThat(reviewItems)
				.extracting(ReviewItem::getDirection)
				.containsExactlyInAnyOrder(ReviewDirection.RECOGNITION, ReviewDirection.RECALL);
		assertThat(reviewItems)
				.allSatisfy(reviewItem -> {
					assertThat(reviewItem.isDeleted()).isFalse();
					assertThat(reviewItem.getNextReviewDate()).isEqualTo(LocalDate.now());
					assertThat(reviewItem.getIntervalDays()).isEqualTo(1);
					assertThat(reviewItem.getEaseFactor()).isEqualTo(2.5);
				});
	}

	@Test
	void createWordRejectsDuplicateForSameUserButAllowsSameWordForDifferentUsers() {
		User firstUser = saveUser("word-duplicate-first@example.com");
		User secondUser = saveUser("word-duplicate-second@example.com");
		wordService.create(firstUser.getId(), new WordCreateRequest("coffee", "커피"));

		Throwable duplicate = catchThrowable(() -> wordService.create(
				firstUser.getId(),
				new WordCreateRequest("  COFFEE  ", "커피")));
		WordResponse otherUsersWord = wordService.create(secondUser.getId(), new WordCreateRequest("coffee", "커피"));

		assertThat(duplicate)
				.isInstanceOf(WordException.class)
				.hasMessage("이미 등록된 단어입니다");
		assertThat(((WordException) duplicate).getErrorCode()).isEqualTo(WordErrorCode.DUPLICATE);
		assertThat(otherUsersWord.id()).isNotNull();
	}

	@Test
	void saveBulkSkipsExistingAndPayloadDuplicates() {
		User user = saveUser("word-bulk@example.com");
		wordService.create(user.getId(), new WordCreateRequest("coffee", "커피"));

		WordBulkSaveResult result = wordService.saveBulk(user.getId(), List.of(
				new WordCreateRequest("coffee", "커피"),
				new WordCreateRequest("study", "공부하다", "verb", null, null, null),
				new WordCreateRequest(" Study ", "학습하다")));

		assertThat(result.saved()).hasSize(1);
		assertThat(result.saved().getFirst().word()).isEqualTo("study");
		assertThat(result.skipped())
				.extracting(WordBulkSkippedItem::word, WordBulkSkippedItem::reason)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("coffee", "이미 등록된 단어입니다"),
						org.assertj.core.groups.Tuple.tuple("Study", "이미 등록된 단어입니다"));
		assertThat(wordRepository.findByUserIdAndDeletedFalse(user.getId())).hasSize(2);
	}

	@Test
	void updateToggleImportantAndSearchOnlyCurrentUsersActiveWords() {
		User owner = saveUser("word-search-owner@example.com");
		User otherUser = saveUser("word-search-other@example.com");
		WordResponse coffee = wordService.create(owner.getId(), new WordCreateRequest(
				"brew coffee",
				"커피를 내리다",
				"phrase",
				null,
				null,
				null));
		wordService.create(owner.getId(), new WordCreateRequest("coffee bean", "커피콩", "noun", null, null, null));
		WordResponse deleted = wordService.create(owner.getId(), new WordCreateRequest("coffee shop", "커피숍", "noun", null, null, null));
		wordService.create(otherUser.getId(), new WordCreateRequest("brew coffee", "커피를 내리다", "phrase", null, null, null));
		wordService.delete(owner.getId(), deleted.id());

		WordResponse updated = wordService.update(owner.getId(), coffee.id(), new WordUpdateRequest(
				"brew fresh coffee",
				"신선한 커피를 내리다",
				"phrase",
				"/bru/",
				"make coffee",
				"brew는 천천히 우려내는 느낌"));
		WordResponse toggled = wordService.toggleImportant(owner.getId(), coffee.id());
		Page<WordResponse> page = wordService.search(
				owner.getId(),
				new WordSearchCondition("coffee", "phrase", true),
				PageRequest.of(0, 10, Sort.by("word").ascending()));

		assertThat(updated.word()).isEqualTo("brew fresh coffee");
		assertThat(toggled.important()).isTrue();
		assertThat(page.getContent())
				.extracting(WordResponse::id)
				.containsExactly(coffee.id());
	}

	@Test
	void deleteSoftDeletesWordAndOnlyItsWordReviewItems() {
		User user = saveUser("word-delete@example.com");
		WordResponse word = wordService.create(user.getId(), new WordCreateRequest("latte", "라떼"));
		ReviewItem sentenceReview = reviewItemRepository.save(new ReviewItem(
				user,
				ReviewItemType.SENTENCE,
				word.id(),
				ReviewDirection.RECOGNITION,
				LocalDate.now()));

		wordService.delete(user.getId(), word.id());

		Word deletedWord = wordRepository.findById(word.id()).orElseThrow();
		assertThat(deletedWord.isDeleted()).isTrue();
		assertThat(reviewItemRepository.findByUserIdAndItemTypeAndItemId(user.getId(), ReviewItemType.WORD, word.id()))
				.allSatisfy(reviewItem -> assertThat(reviewItem.isDeleted()).isTrue());
		assertThat(reviewItemRepository.findById(sentenceReview.getId()).orElseThrow().isDeleted()).isFalse();
	}

	@Test
	void getUpdateAndDeleteRejectOtherUsersWordAsForbidden() {
		User owner = saveUser("word-forbidden-owner@example.com");
		User otherUser = saveUser("word-forbidden-other@example.com");
		WordResponse word = wordService.create(owner.getId(), new WordCreateRequest("owner word", "소유자 단어"));

		Throwable read = catchThrowable(() -> wordService.get(otherUser.getId(), word.id()));
		Throwable update = catchThrowable(() -> wordService.update(
				otherUser.getId(),
				word.id(),
				new WordUpdateRequest("changed", "변경됨")));
		Throwable delete = catchThrowable(() -> wordService.delete(otherUser.getId(), word.id()));

		assertThat(read).isInstanceOf(WordException.class);
		assertThat(((WordException) read).getErrorCode()).isEqualTo(WordErrorCode.FORBIDDEN);
		assertThat(update).isInstanceOf(WordException.class);
		assertThat(((WordException) update).getErrorCode()).isEqualTo(WordErrorCode.FORBIDDEN);
		assertThat(delete).isInstanceOf(WordException.class);
		assertThat(((WordException) delete).getErrorCode()).isEqualTo(WordErrorCode.FORBIDDEN);
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}
}

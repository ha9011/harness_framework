package com.english.pattern;

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
class PatternServiceTest {

	private final PatternService patternService;
	private final UserRepository userRepository;
	private final PatternRepository patternRepository;
	private final PatternExampleRepository patternExampleRepository;
	private final StudyRecordRepository studyRecordRepository;
	private final StudyRecordItemRepository studyRecordItemRepository;
	private final ReviewItemRepository reviewItemRepository;

	@Autowired
	PatternServiceTest(
			PatternService patternService,
			UserRepository userRepository,
			PatternRepository patternRepository,
			PatternExampleRepository patternExampleRepository,
			StudyRecordRepository studyRecordRepository,
			StudyRecordItemRepository studyRecordItemRepository,
			ReviewItemRepository reviewItemRepository
	) {
		this.patternService = patternService;
		this.userRepository = userRepository;
		this.patternRepository = patternRepository;
		this.patternExampleRepository = patternExampleRepository;
		this.studyRecordRepository = studyRecordRepository;
		this.studyRecordItemRepository = studyRecordItemRepository;
		this.reviewItemRepository = reviewItemRepository;
	}

	@Test
	void createPatternStoresExamplesInInputOrderAndCreatesStudyRecordAndBidirectionalReviewItems() {
		User user = saveUser("pattern-create@example.com");

		PatternResponse response = patternService.create(user.getId(), new PatternCreateRequest(
				"I'm afraid that...",
				"나쁜 소식을 부드럽게 말할 때 쓴다",
				List.of(
						new PatternExampleRequest("I'm afraid that we are late.", "우리가 늦은 것 같아요."),
						new PatternExampleRequest("I'm afraid that it is closed.", "문을 닫은 것 같아요."))));

		assertThat(response.id()).isNotNull();
		assertThat(response.template()).isEqualTo("I'm afraid that...");
		assertThat(response.examples())
				.extracting(PatternExampleResponse::sortOrder, PatternExampleResponse::sentence)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(1, "I'm afraid that we are late."),
						org.assertj.core.groups.Tuple.tuple(2, "I'm afraid that it is closed."));

		Pattern saved = patternRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getUser().getId()).isEqualTo(user.getId());
		assertThat(saved.isDeleted()).isFalse();

		StudyRecord record = studyRecordRepository.findByUserIdAndStudyDate(user.getId(), LocalDate.now())
				.orElseThrow();
		assertThat(record.getDayNumber()).isEqualTo(1);
		List<StudyRecordItem> studyItems = studyRecordItemRepository.findByStudyRecordId(record.getId());
		assertThat(studyItems)
				.extracting(StudyRecordItem::getItemType, StudyRecordItem::getItemId)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(StudyItemType.PATTERN, response.id()));

		List<ReviewItem> reviewItems = reviewItemRepository.findByUserIdAndItemTypeAndItemId(
				user.getId(),
				ReviewItemType.PATTERN,
				response.id());
		assertThat(reviewItems).hasSize(2);
		assertThat(reviewItems)
				.extracting(ReviewItem::getDirection)
				.containsExactlyInAnyOrder(ReviewDirection.RECOGNITION, ReviewDirection.RECALL);
	}

	@Test
	void createPatternRejectsDuplicateForSameUserButAllowsSameTemplateForDifferentUsers() {
		User firstUser = saveUser("pattern-duplicate-first@example.com");
		User secondUser = saveUser("pattern-duplicate-second@example.com");
		patternService.create(firstUser.getId(), new PatternCreateRequest("I tend to...", "자주 하는 일을 말한다"));

		Throwable duplicate = catchThrowable(() -> patternService.create(
				firstUser.getId(),
				new PatternCreateRequest(" i tend to... ", "자주 하는 일을 말한다")));
		PatternResponse otherUsersPattern = patternService.create(
				secondUser.getId(),
				new PatternCreateRequest("I tend to...", "자주 하는 일을 말한다"));

		assertThat(duplicate)
				.isInstanceOf(PatternException.class)
				.hasMessage("이미 등록된 패턴입니다");
		assertThat(((PatternException) duplicate).getErrorCode()).isEqualTo(PatternErrorCode.DUPLICATE);
		assertThat(otherUsersPattern.id()).isNotNull();
	}

	@Test
	void updateReplacesExamplesAndListOnlyReturnsCurrentUsersActivePatterns() {
		User owner = saveUser("pattern-list-owner@example.com");
		User otherUser = saveUser("pattern-list-other@example.com");
		PatternResponse target = patternService.create(owner.getId(), new PatternCreateRequest(
				"I used to...",
				"과거 습관",
				List.of(new PatternExampleRequest("I used to drink tea.", "나는 차를 마시곤 했다."))));
		PatternResponse deleted = patternService.create(owner.getId(), new PatternCreateRequest("Could you...?", "부탁"));
		patternService.create(otherUser.getId(), new PatternCreateRequest("I used to...", "과거 습관"));
		patternService.delete(owner.getId(), deleted.id());

		PatternResponse updated = patternService.update(owner.getId(), target.id(), new PatternUpdateRequest(
				"I usually...",
				"평소 습관",
				List.of(new PatternExampleRequest("I usually brew coffee.", "나는 보통 커피를 내린다."))));
		Page<PatternResponse> page = patternService.search(
				owner.getId(),
				PageRequest.of(0, 10, Sort.by("template").ascending()));

		assertThat(updated.template()).isEqualTo("I usually...");
		assertThat(updated.examples())
				.extracting(PatternExampleResponse::sortOrder, PatternExampleResponse::sentence)
				.containsExactly(org.assertj.core.groups.Tuple.tuple(1, "I usually brew coffee."));
		assertThat(patternExampleRepository.findByPatternIdOrderBySortOrderAsc(target.id())).hasSize(1);
		assertThat(page.getContent())
				.extracting(PatternResponse::id)
				.containsExactly(target.id());
	}

	@Test
	void getReturnsPatternDetailWithExamplesInOrder() {
		User user = saveUser("pattern-detail@example.com");
		PatternResponse created = patternService.create(user.getId(), new PatternCreateRequest(
				"Would you mind if...",
				"허락을 구한다",
				List.of(
						new PatternExampleRequest("Would you mind if I sit here?", "제가 여기 앉아도 될까요?"),
						new PatternExampleRequest("Would you mind if I open it?", "제가 그것을 열어도 될까요?"))));

		PatternResponse detail = patternService.get(user.getId(), created.id());

		assertThat(detail.template()).isEqualTo("Would you mind if...");
		assertThat(detail.examples())
				.extracting(PatternExampleResponse::sortOrder, PatternExampleResponse::translation)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(1, "제가 여기 앉아도 될까요?"),
						org.assertj.core.groups.Tuple.tuple(2, "제가 그것을 열어도 될까요?"));
	}

	@Test
	void deleteSoftDeletesPatternAndOnlyItsPatternReviewItems() {
		User user = saveUser("pattern-delete@example.com");
		PatternResponse pattern = patternService.create(user.getId(), new PatternCreateRequest("Let's...", "제안"));
		ReviewItem sentenceReview = reviewItemRepository.save(new ReviewItem(
				user,
				ReviewItemType.SENTENCE,
				pattern.id(),
				ReviewDirection.RECOGNITION,
				LocalDate.now()));

		patternService.delete(user.getId(), pattern.id());

		Pattern deletedPattern = patternRepository.findById(pattern.id()).orElseThrow();
		assertThat(deletedPattern.isDeleted()).isTrue();
		assertThat(reviewItemRepository.findByUserIdAndItemTypeAndItemId(user.getId(), ReviewItemType.PATTERN, pattern.id()))
				.allSatisfy(reviewItem -> assertThat(reviewItem.isDeleted()).isTrue());
		assertThat(reviewItemRepository.findById(sentenceReview.getId()).orElseThrow().isDeleted()).isFalse();
	}

	@Test
	void getUpdateAndDeleteRejectOtherUsersPatternAsForbidden() {
		User owner = saveUser("pattern-forbidden-owner@example.com");
		User otherUser = saveUser("pattern-forbidden-other@example.com");
		PatternResponse pattern = patternService.create(owner.getId(), new PatternCreateRequest("owner pattern", "소유자 패턴"));

		Throwable read = catchThrowable(() -> patternService.get(otherUser.getId(), pattern.id()));
		Throwable update = catchThrowable(() -> patternService.update(
				otherUser.getId(),
				pattern.id(),
				new PatternUpdateRequest("changed", "변경됨")));
		Throwable delete = catchThrowable(() -> patternService.delete(otherUser.getId(), pattern.id()));

		assertThat(read).isInstanceOf(PatternException.class);
		assertThat(((PatternException) read).getErrorCode()).isEqualTo(PatternErrorCode.FORBIDDEN);
		assertThat(update).isInstanceOf(PatternException.class);
		assertThat(((PatternException) update).getErrorCode()).isEqualTo(PatternErrorCode.FORBIDDEN);
		assertThat(delete).isInstanceOf(PatternException.class);
		assertThat(((PatternException) delete).getErrorCode()).isEqualTo(PatternErrorCode.FORBIDDEN);
	}

	private User saveUser(String email) {
		return userRepository.save(new User(
				email,
				"$2a$10$123456789012345678901u123456789012345678901234567890123456",
				"tester"));
	}
}

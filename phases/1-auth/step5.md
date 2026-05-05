# Step 5: 전체 Entity userId 적용 — 사용자별 데이터 격리 (TDD)

## 읽어야 할 파일

먼저 아래 파일들을 **전부** 읽고 프로젝트의 현재 상태와 설계 의도를 완전히 파악하라. 이 Step은 변경 범위가 매우 크므로 꼼꼼히 읽어야 한다:

- `/docs/PLAN.md` — 변경 범위 상세, 보안 체크리스트, IDOR 방어, softDelete 정책
- `/docs/ARCHITECTURE.md` — 데이터 모델, UNIQUE 제약 변경, 에러 처리 흐름
- `/docs/ADR.md` — ADR-008 (전체 Entity user_id FK)
- `/backend/src/main/java/com/english/auth/User.java` — Step 1에서 생성
- `/backend/src/main/java/com/english/config/SecurityConfig.java` — Step 4에서 생성
- 그리고 `backend/src/main/java/com/english/` 하위의 **모든 Entity, Repository, Service, Controller** 파일
- `backend/src/test/java/com/english/` 하위의 **모든 테스트** 파일

## 작업 개요

7개 Entity에 `@ManyToOne User user` FK를 추가하고, 모든 Repository/Service/Controller/테스트를 수정하여 사용자별 데이터 격리를 구현한다. **중간 substep에서는 빌드가 불가능할 수 있다 (callee 시그니처 변경 → caller 컴파일 에러). 모든 변경을 완료한 후 최종 빌드/테스트를 수행하라.**

## 작업 상세

### Phase A: Entity 변경 (7개)

아래 7개 Entity에 `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;` 필드를 추가하라. 생성자에도 `User user` 파라미터를 추가하라.

1. **Word** — `@Column(unique = true)` on `word` 필드 제거. 중복 검증은 앱 레벨에서만 처리 (soft delete 재등록 호환)
2. **Pattern** — `@Column(unique = true)` on `template` 필드 제거. 동일 이유
3. **GeneratedSentence** — user 추가
4. **GenerationHistory** — user 추가
5. **ReviewItem** — `@UniqueConstraint(columnNames = {"item_type", "item_id", "direction"})` 제거. 앱 레벨 검증으로 전환
6. **StudyRecord** — user 추가. `study_date`의 UNIQUE 제거 (사용자+날짜별 하나이므로)
7. **UserSetting** — user 추가

추가로 **ReviewLog**:
- `@Column(name = "review_item_id") private Long reviewItemId` → `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_item_id", nullable = false) private ReviewItem reviewItem`으로 변경
- ReviewLog 생성자와 ReviewItem.applyResult()에서 ReviewLog를 생성하는 코드도 함께 수정 (Long → ReviewItem 참조)

### Phase B: Repository 변경 (8개 + ReviewLog)

모든 Repository 메서드에 User 파라미터를 추가하라. 기존 메서드를 수정하고, @Query 어노테이션의 WHERE 절에 user 조건을 추가하라.

**핵심 변경 목록:**

- **WordRepository**: `findByDeletedFalse()` → `findByUserAndDeletedFalse(User user)`, `findByIdAndDeletedFalse()` → `findByIdAndUserAndDeletedFalse(Long id, User user)`, `existsByWordAndDeletedFalse()` → `existsByWordAndUserAndDeletedFalse(String word, User user)`, `countByDeletedFalse()` → `countByUserAndDeletedFalse(User user)`, `findAllWithFilters` @Query에 `w.user = :user` 추가
- **PatternRepository**: 동일 패턴. `existsByTemplateAndDeletedFalse` → `existsByTemplateAndUserAndDeletedFalse`
- **ReviewItemRepository**: @Query 4개에 `r.user = :user` 추가. `softDeleteByItemTypeAndItemId` → user 조건 추가
- **StudyRecordRepository**: `findByCreatedAt` → `findByUserAndCreatedAt(User user, LocalDate date)`, `findMaxDayNumber` @Query에 user 조건 추가, `findTop5ByOrderByCreatedAtDesc` → `findTop5ByUserOrderByCreatedAtDesc(User user)`, `findAllByOrderByCreatedAtDesc` → `findByUserOrderByCreatedAtDesc`
- **GeneratedSentenceRepository**: @Query에 user 조건 추가. `countByUser(User user)` 메서드 추가
- **GenerationHistoryRepository**: `findAllByOrderByCreatedAtDesc` → `findByUserOrderByCreatedAtDesc`
- **UserSettingRepository**: `findByUser(User user)` 추가
- **ReviewLogRepository**: `reviewItemId` → `reviewItem` 참조로 변경 후, @Query에 `r.reviewItem.user = :user` 조건 추가

### Phase C: Service 변경 (8개)

모든 Service의 public 메서드에 `User user` 파라미터를 추가하라 (또는 내부에서 SecurityContext를 사용). 권장: Controller에서 `@AuthenticationPrincipal User user`로 받아서 Service에 전달.

**핵심 변경:**

1. **StudyRecordService**: `getOrCreateTodayRecord(User user)` — findMaxDayNumber에 user 필터
2. **SettingService**: `getOrCreateSetting(User user)` — `findAll().get(0)` → `findByUser(user)`
3. **ReviewItemService**: `createWordReviewItems(Long wordId, User user)` — ReviewItem 생성 시 user 설정
4. **WordService**: 6개 메서드에 User 추가. IDOR 방어: `findByIdAndUserAndDeletedFalse`로 소유권 검증. softDelete: user 조건 포함
5. **PatternService**: 동일 패턴. IDOR 방어 + softDelete user 조건
6. **GenerateService**: `selectWords(User user)`, `selectPatterns(User user)`. `generateByWord/generateByPattern`에서 target 소유권 검증. `callGeminiAndSave` 내부 `existsById` → `existsByIdAndUser`
7. **ReviewService**: `getTodayCards`, `submitResult`에 User 추가. submitResult에서 ReviewItem 소유권 검증
8. **DashboardService**: 모든 count/조회 메서드에 user 필터. `findTop5ByUserOrderByCreatedAtDesc`. `calculateStreak`에서 ReviewLog user 필터

### Phase D: Controller 변경 (7개)

모든 Controller 메서드에 `@AuthenticationPrincipal User user` 파라미터를 추가하고, Service 호출 시 user를 전달하라.

```java
@GetMapping
public ResponseEntity<Page<WordListResponse>> getList(
    @AuthenticationPrincipal User user,
    // ... 기존 파라미터
) {
    return ResponseEntity.ok(wordService.getList(user, ...));
}
```

### Phase E: 테스트 수정 (22개 파일)

**단위 테스트 (Service/Controller):**
- 테스트에서 `User testUser = new User("test@test.com", "password", "테스터");`를 만들고 리플렉션으로 id를 설정하라
- Entity 생성자에 User 파라미터 추가 반영 (50곳)
- Service mock의 메서드 시그니처에 User 파라미터 추가 반영
- given(...).willReturn(...) 호출에서 User 파라미터 추가

**Controller 테스트 (standaloneSetup):**
- Service mock 시그니처만 변경. Security 필터는 적용되지 않으므로 @AuthenticationPrincipal은 `HandlerMethodArgumentResolver`를 mock으로 등록하여 User 객체를 주입하라

**기존 테스트의 assert/expect 값 자체는 변경하지 마라.** 변경하는 것은 메서드 시그니처와 Entity 생성자뿐이다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

- 전체 단위 테스트 통과 (기존 수정 + 신규 없음)
- 컴파일 에러 없음

## 검증 절차

1. `cd backend && ./gradlew test` 실행
2. 7개 Entity에 user 필드가 추가되었는지 확인
3. Word.word, Pattern.template의 DB UNIQUE가 제거되었는지 확인
4. ReviewItem의 DB UniqueConstraint가 제거되었는지 확인
5. ReviewLog가 @ManyToOne ReviewItem으로 변경되었는지 확인
6. 모든 Service에서 IDOR 방어 (findByIdAndUser)가 적용되었는지 확인
7. softDelete 쿼리에 user 조건이 포함되었는지 확인
8. 기존 테스트가 메서드 시그니처 변경만 반영하고 assert 값은 유지하는지 확인

## 금지사항

- 통합 테스트(IntegrationTestBase 상속)를 이 단계에서 수정하지 마라. 이유: Step 6에서 인증 헬퍼와 함께 일괄 수정한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 프론트엔드 코드를 수정하지 마라. 이유: Step 7에서 처리한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

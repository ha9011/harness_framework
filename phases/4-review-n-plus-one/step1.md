# Step 1: service-refactor

## Step Contract

- Capability: ReviewService.getTodayCards()를 배치 조회 방식으로 리팩터링하여 N+1 쿼리를 3~4 고정 쿼리로 줄임
- Layer: service
- Write Scope: `backend/src/main/java/com/english/review/ReviewService.java`, `backend/src/test/java/com/english/review/ReviewServiceTest.java`
- Out of Scope: Repository 메서드 추가/수정 (Step 0에서 완료), 프론트엔드, Controller, 통합 테스트 수정
- Critical Gates: `cd backend && ./gradlew test --tests "com.english.review.ReviewServiceTest"` — 단위 테스트 통과. `cd backend && ./gradlew test --tests "com.english.integration.ReviewIntegrationTest"` — 통합 테스트 통과 (API 응답 형식 불변 확인)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 전체 작업 계획, 이슈 7건, 엣지케이스 체크리스트
- `/docs/ARCHITECTURE.md` — 데이터 모델
- `/docs/ADR.md` — ADR-009
- `backend/src/main/java/com/english/review/ReviewService.java` — 현재 구현 (리팩터링 대상)
- `backend/src/main/java/com/english/review/ReviewItemRepository.java` — Step 0에서 추가된 Pageable 오버로드
- `backend/src/main/java/com/english/word/WordRepository.java` — Step 0에서 추가된 findByIdInAndUserAndDeletedFalse
- `backend/src/main/java/com/english/pattern/PatternRepository.java` — Step 0에서 추가된 findByIdInWithExamples
- `backend/src/main/java/com/english/generate/GeneratedSentenceRepository.java` — Step 0에서 추가된 findByIdInWithSituations, findByWordIdInWithMapping
- `backend/src/main/java/com/english/review/ReviewItem.java` — itemType, itemId, direction 필드
- `backend/src/main/java/com/english/review/ReviewCardResponse.java` — 응답 DTO
- `backend/src/test/java/com/english/review/ReviewServiceTest.java` — 기존 단위 테스트 (mock 변경 필요)
- `backend/src/test/java/com/english/integration/ReviewIntegrationTest.java` — 통합 테스트 (변경 불필요, 검증용)

Step 0에서 만들어진 배치 조회 메서드를 꼼꼼히 읽고, 시그니처를 정확히 파악한 뒤 작업하라.

## 작업

### 1. getTodayCards() 리팩터링

현재 흐름 (건바이건 개별 조회):
```
findTodayCards(전체) → stream.map(buildCardResponse) → 각 아이템마다 Repository 개별 조회 → settingService.getSetting → LIMIT → 셔플
```

변경 후 흐름 (배치 조회):
```
settingService.getSetting → dailyReviewCount 가드 → findTodayCards(LIMIT) → type별 itemId 수집 → IN 배치 조회 → Map 구성 → Map 기반 카드 빌드 → null 필터 → 셔플
```

핵심 규칙:

1. **dailyReviewCount ≤ 0 가드**: `PageRequest.of(0, 0)`은 `IllegalArgumentException`을 발생시킨다. 현재 코드는 `Math.min(cards.size(), 0)` → `subList(0, 0)` → 빈 리스트로 정상 동작한다. 리팩터링 후에도 동일한 동작을 보장하기 위해 `if (dailyReviewCount <= 0) return Collections.emptyList()` 가드를 Pageable 호출 전에 추가하라.

2. **빈 IN 리스트 가드**: itemId 리스트가 비어있으면 배치 조회를 호출하지 마라. `WHERE id IN ()` → SQL 문법 오류. 모든 배치 조회 호출 전 `if (ids.isEmpty())` 가드 추가.

3. **type별 배치 조회**: API는 `?type=WORD` 처럼 단일 타입으로 호출된다. switch/if로 타입별 배치 조회를 분기하라.
   - `"WORD"`: `wordRepository.findByIdInAndUserAndDeletedFalse(itemIds, user)` → `Map<Long, Word>`
   - `"PATTERN"`: `patternRepository.findByIdInWithExamples(itemIds, user)` → `Map<Long, Pattern>` (examples 이미 JOIN FETCH됨)
   - `"SENTENCE"`: `generatedSentenceRepository.findByIdInWithSituations(itemIds)` → `Map<Long, GeneratedSentence>` (situations 이미 JOIN FETCH됨)

4. **WORD 예문 배치 (RECOGNITION 방향만)**: RECALL 방향 단어는 예문을 사용하지 않는다. 예문 배치 조회용 wordId는 RECOGNITION 방향 아이템에서만 수집하라. wordMap에 존재하는(soft-deleted 아닌) 단어만 필터링하라.
   ```
   recognitionWordIds = items에서 direction=="RECOGNITION"인 것만 → itemId 수집 → wordMap에 존재하는 것만
   if (!recognitionWordIds.isEmpty()) → findByWordIdInWithMapping(recognitionWordIds)
   → Object[0]=wordId, Object[1]=GeneratedSentence → Map<Long, List<GeneratedSentence>> 구성
   ```

5. **카드 빌드**: 기존 `buildCardResponse(ReviewItem)` → `buildCardFromMap(ReviewItem, entityMap, wordExamplesMap)` 방식으로 변경. 개별 Repository 호출을 Map 조회로 대체.

6. **기존 private 메서드 정리**: `buildCardResponse`, `buildWordCard`, `buildPatternCard`, `buildSentenceCard`, `getWordExamples` — 이 메서드들은 개별 Repository 호출에 의존한다. Map 기반으로 시그니처를 변경하거나, 새 메서드로 교체하고 기존 것을 삭제하라.

7. **예문 최대 3개 + 셔플 로직 유지**: 기존 `getWordExamples`의 로직 — 예문 4개 이상이면 랜덤 3개 선택, 3개 이하면 전부 반환. 이 로직을 Map 기반 버전에서도 동일하게 유지하라.

### 2. ReviewServiceTest mock 수정

기존 테스트의 **검증 의도(assert)는 변경하지 마라**. mock 설정만 배치 메서드 시그니처에 맞게 변경한다.

변경이 필요한 mock 패턴:

**findTodayCards** — 모든 given/verify에 `Pageable` 파라미터 추가:
```
// Before: given(reviewItemRepository.findTodayCards(eq(testUser), eq("WORD"), any(LocalDate.class), anyList()))
// After:  given(reviewItemRepository.findTodayCards(eq(testUser), eq("WORD"), any(LocalDate.class), anyList(), any(Pageable.class)))
```

**WORD 테스트** — 개별 조회 mock → 배치 조회 mock:
```
// Before: given(wordRepository.findByIdAndUserAndDeletedFalse(1L, testUser)).willReturn(Optional.of(word))
// After:  given(wordRepository.findByIdInAndUserAndDeletedFalse(anyList(), eq(testUser))).willReturn(List.of(word))

// Before: given(generatedSentenceRepository.findByWordId(1L)).willReturn(List.of(s1, s2))
// After:  given(generatedSentenceRepository.findByWordIdInWithMapping(anyList())).willReturn(List.of(new Object[]{1L, s1}, new Object[]{1L, s2}))
```

**PATTERN 테스트** — 배치 mock:
```
// Before: given(patternRepository.findByIdAndUserAndDeletedFalse(1L, testUser)).willReturn(Optional.of(pattern))
// After:  given(patternRepository.findByIdInWithExamples(anyList(), eq(testUser))).willReturn(List.of(pattern))
```

**SENTENCE 테스트** — 배치 mock:
```
// Before: given(generatedSentenceRepository.findById(1L)).willReturn(Optional.of(sentence))
// After:  given(generatedSentenceRepository.findByIdInWithSituations(anyList())).willReturn(List.of(sentence))
```

**excludeCards 테스트** — verify에 Pageable 추가:
```
// Before: verify(reviewItemRepository).findTodayCards(testUser, "WORD", LocalDate.now(), exclude)
// After:  verify(reviewItemRepository).findTodayCards(eq(testUser), eq("WORD"), eq(LocalDate.now()), eq(exclude), any(Pageable.class))
```

**WORD 예문 테스트(wordRecognitionBackExamples, wordRecognitionBackExamplesLessThan3)**: 기존 테스트는 예문 개수(hasSize(3), hasSize(2))를 검증한다. 배치 mock에서 Object[] 형태로 예문을 넘길 때 wordId 매핑이 올바른지 확인하라. 테스트용 GeneratedSentence에 `addSentenceWord(wordId)`가 필요하진 않다 — Object[] 프로젝션은 이미 wordId를 포함하므로.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "com.english.review.ReviewServiceTest"         # 단위 테스트 통과
cd backend && ./gradlew test --tests "com.english.integration.ReviewIntegrationTest" # 통합 테스트 통과
cd backend && ./gradlew test                                                         # 전체 테스트 회귀 없음
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 핵심 capability를 검증했는지 확인한다:
   - ReviewServiceTest의 모든 getTodayCards 테스트가 통과하는가?
   - ReviewIntegrationTest가 통과하는가? (API 응답 형식 불변)
3. 아키텍처 체크리스트를 확인한다:
   - Step Contract의 Write Scope 밖(Repository, Controller, 프론트엔드)을 수정하지 않았는가?
   - 기존 테스트의 assert/expect 값을 변경하지 않았는가?
4. 결과에 따라 `phases/4-review-n-plus-one/index.json`의 step 1을 업데이트한다.

## 금지사항

- Repository 메서드를 추가/수정하지 마라. 이유: Step 0에서 완료됨
- Controller를 수정하지 마라. 이유: API 시그니처 불변
- 기존 테스트의 기대값(assert)을 변경하지 마라. 이유: 응답 형식이 동일해야 함. mock 설정만 변경 허용. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, PRD/ADR에 근거해야 한다
- `TODO`, `not implemented`, `stub`으로 핵심 기능을 대체하고 completed 처리하지 마라. 이유: 미구현을 완료로 오판하는 것을 방지
- 프론트엔드 코드를 수정하지 마라. 이유: 백엔드 전용 step

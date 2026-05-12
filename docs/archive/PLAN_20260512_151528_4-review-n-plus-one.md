# PLAN: 복습 카드 조회 N+1 성능 최적화

## 작업 목표
`/api/reviews/today` API의 N+1 쿼리 문제를 LIMIT + IN 배치 조회로 해소하여 쿼리 수를 O(N)에서 O(1)로 줄인다.

## 구현할 기능
1. ReviewItemRepository에 Pageable 파라미터 추가 (LIMIT 적용)
2. WordRepository, PatternRepository, GeneratedSentenceRepository에 IN 배치 조회 메서드 추가
3. ReviewService.getTodayCards()를 배치 조회 방식으로 리팩터링 (WORD, PATTERN, SENTENCE 전체 적용)

## 기술적 제약사항
- Polymorphic Association (item_type + item_id) 구조 유지 — JOIN FETCH 불가, IN 쿼리로 해결
- 기존 API 응답 형식(ReviewCardResponse) 변경 없음
- dailyReviewCount를 먼저 조회하여 Repository 레벨에서 LIMIT 적용

## 검토에서 발견된 이슈 (1차)

### 이슈 1: Pattern.examples Lazy Loading (숨은 N+1)
- `Pattern.examples`는 `@OneToMany(fetch = LAZY)` (JPA 기본값)
- `buildPatternCard()`에서 `pattern.getExamples()` 호출 시 **패턴마다 예문 조회 쿼리 추가 발생**
- 해결: PatternRepository 배치 조회 시 `LEFT JOIN FETCH p.examples` 사용

### 이슈 2: GeneratedSentence.situations Lazy Loading (숨은 N+1)
- `GeneratedSentence.situations`도 `@OneToMany(fetch = LAZY)`
- `buildSentenceCard()`에서 `sentence.getSituations()` 호출 시 **문장마다 상황 조회 쿼리 추가 발생**
- 해결: GeneratedSentence 배치 조회 시 `LEFT JOIN FETCH gs.situations` 사용

### 이슈 3: WORD 예문 배치 조회의 wordId 매핑
- 현재 `findByWordId(wordId)` → 단어별 개별 쿼리
- 배치로 바꾸면 `findByWordIdIn(List<Long> wordIds)` → 결과에서 wordId별로 그룹핑 필요
- 해결: `SELECT sw.wordId, gs FROM GeneratedSentence gs JOIN gs.sentenceWords sw WHERE sw.wordId IN :wordIds` → Object[]로 받아 Map<Long, List<GeneratedSentence>> 구성

### LIMIT 후 null 필터링 주의
- LIMIT으로 N개 가져온 후 원본이 삭제된 항목이 있으면 실제 반환 수 < dailyReviewCount
- 하지만 PRD 규칙상 단어/패턴 삭제 시 review_items도 soft delete되므로 실제로는 거의 발생하지 않음
- 허용 가능한 동작으로 판단 (over-fetch 같은 복잡한 처리 불필요)

## 검토에서 발견된 이슈 (2차 — 엣지케이스/사이드이펙트)

### 이슈 4: MultipleBagFetchException 위험 ⚠️
- `GeneratedSentence`는 `@OneToMany List` 컬렉션이 **2개** (`situations`, `sentenceWords`)
- 두 컬렉션을 동시에 `LEFT JOIN FETCH`하면 Hibernate가 `MultipleBagFetchException` 발생
- **SENTENCE 배치 조회**: `situations`만 JOIN FETCH (sentenceWords 불필요) → 안전
- **WORD 예문 배치 조회**: sentenceWords는 WHERE 조건의 JOIN으로만 사용, 컬렉션 FETCH 아님. situations도 불필요 (예문은 englishSentence/koreanTranslation만 사용) → 안전
- 구현 시 **절대 두 컬렉션을 동시에 JOIN FETCH하지 말 것**

### 이슈 5: 빈 IN 리스트 가드 필수 ⚠️
- `WHERE id IN :ids`에 빈 리스트가 전달되면 Hibernate가 `WHERE id IN ()` → **SQL 문법 오류**
- 발생 시나리오: 오늘 복습 대상이 전부 PATTERN인데 type=WORD로 조회 → WORD itemId 리스트가 빈 상태
- 해결: 배치 조회 호출 전 `if (ids.isEmpty()) return emptyMap()` 가드 추가
- 모든 IN 쿼리 호출 지점 (Word, Pattern, GeneratedSentence, 예문)에 동일 적용

### 이슈 6: RECALL 방향은 예문 불필요 — 불필요 쿼리 방지
- `buildWordCard()`에서 RECOGNITION 방향만 `getWordExamples()` 호출 (Line 110)
- RECALL 방향 단어는 예문 미사용 (Line 114-116)
- 예문 배치 조회용 wordId 수집 시 **RECOGNITION 방향 아이템만** 필터링해야 불필요한 데이터 로딩 방지
- `items.stream().filter(i -> "WORD".equals(i.getItemType()) && "RECOGNITION".equals(i.getDirection())).map(ReviewItem::getItemId)`

### 기존 동작 유지 확인 (사이드이펙트 없음)
- **settingService.getSetting()**: `@Transactional`으로 설정 없으면 자동 생성. 호출 시점이 앞으로 이동하지만 같은 트랜잭션 내이므로 동작 동일
- **셔플**: LIMIT 후 카드 빌드 후 셔플 — 순서 변경 없음
- **exclude 파라미터**: `-1L` sentinel 처리 로직 불변, Pageable 추가와 무관
- **Pageable + @Query ORDER BY**: `PageRequest.of(0, limit)` (Sort 미지정) 사용 시 @Query의 ORDER BY 그대로 유지

## 최종 쿼리 수 비교

### 변경 전 (WORD 기준, N개)
| 쿼리 | 횟수 |
|------|------|
| findTodayCards (전체) | 1 |
| wordRepository.findById × N | N |
| generatedSentenceRepository.findByWordId × N | N |
| settingService.getSetting | 1 |
| **Pattern: patternRepository.findById × N** | N |
| **Pattern: pattern.getExamples() lazy load × N** | N |
| **Sentence: generatedSentenceRepository.findById × N** | N |
| **Sentence: sentence.getSituations() lazy load × N** | N |
| **합계** | **2N+2 ~ 4N+2** |

### 변경 후
| 쿼리 | 횟수 |
|------|------|
| settingService.getSetting | 1 |
| findTodayCards (LIMIT) | 1 |
| 타입별 배치 조회 (JOIN FETCH 포함) | 1 |
| WORD 예문 배치 조회 (RECOGNITION만) | 0~1 |
| **합계** | **3~4 (고정)** |

## 테스트 전략
- 기존 테스트 영향: ReviewServiceTest의 mock 호출 시그니처 변경 필요 (findTodayCards에 Pageable 추가, 개별 조회 → 배치 조회)
- 신규 테스트: 배치 조회 메서드의 Repository 테스트는 기존 JPA 메서드 네이밍 컨벤션 + JPQL이므로 통합 테스트에서 검증
- 통합 테스트: ReviewIntegrationTest에서 응답 형식 불변 확인
- 테스트 수정이 필요한 Step: Step 1에서 ServiceTest mock 변경 허용

## Phase/Step 초안

### Step 0: Repository 배치 조회 메서드 추가
- 작업:
  - ReviewItemRepository.findTodayCards()에 Pageable 파라미터 추가
  - WordRepository에 `findByIdInAndUserAndDeletedFalse(List<Long> ids, User user)` 추가
  - PatternRepository에 `SELECT DISTINCT p FROM Pattern p LEFT JOIN FETCH p.examples WHERE p.id IN :ids AND p.user = :user AND p.deleted = false` 추가
  - GeneratedSentenceRepository에 `SELECT DISTINCT gs FROM GeneratedSentence gs LEFT JOIN FETCH gs.situations WHERE gs.id IN :ids` 추가 (sentenceWords JOIN FETCH 금지 — MultipleBagFetchException)
  - GeneratedSentenceRepository에 `SELECT sw.wordId, gs FROM GeneratedSentence gs JOIN gs.sentenceWords sw WHERE sw.wordId IN :wordIds` 추가
- 산출물: 4개 Repository에 배치 조회 메서드 추가

### Step 1: ReviewService 리팩터링 + 테스트 수정
- 작업:
  - settingService.getSetting()을 먼저 호출하여 dailyReviewCount 확보
  - findTodayCards()에 PageRequest.of(0, dailyReviewCount) 전달
  - itemType별로 itemId를 수집 → **빈 리스트 가드** → IN 쿼리로 한 번에 조회 → Map<Long, Entity>으로 변환
  - WORD 예문: **RECOGNITION 방향만** wordId 수집 → findByWordIdIn()으로 조회 → Map<Long, List<GeneratedSentence>>
  - buildCardResponse()를 Map 기반으로 변경 (개별 Repository 호출 제거)
  - ReviewServiceTest: mock 호출 시그니처 변경 반영
  - 전체 테스트 스위트 통과 확인 (`./gradlew test`)
- 산출물: getTodayCards() 쿼리 수 O(N) → O(1), 전체 테스트 통과

## 미결 사항
- 없음

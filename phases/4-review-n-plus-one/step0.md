# Step 0: batch-repositories

## Step Contract

- Capability: 복습 카드 조회에 필요한 배치 조회 Repository 메서드 추가
- Layer: domain
- Write Scope: `backend/src/main/java/com/english/review/ReviewItemRepository.java`, `backend/src/main/java/com/english/word/WordRepository.java`, `backend/src/main/java/com/english/pattern/PatternRepository.java`, `backend/src/main/java/com/english/generate/GeneratedSentenceRepository.java`
- Out of Scope: ReviewService 로직 변경, 테스트 수정, 프론트엔드, 기존 메서드 삭제
- Critical Gates: `cd backend && ./gradlew compileJava` — 컴파일 성공 확인. 새 메서드가 JPQL 문법 오류 없이 추가됨

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 전체 작업 계획 및 이슈 7건
- `/docs/ARCHITECTURE.md` — 데이터 모델, 패턴
- `/docs/ADR.md` — ADR-009: N+1 해소 결정
- `backend/src/main/java/com/english/review/ReviewItemRepository.java` — 기존 findTodayCards 쿼리
- `backend/src/main/java/com/english/word/WordRepository.java` — 기존 단건 조회 메서드
- `backend/src/main/java/com/english/pattern/PatternRepository.java` — 기존 단건 조회 메서드
- `backend/src/main/java/com/english/generate/GeneratedSentenceRepository.java` — 기존 findByWordId 메서드
- `backend/src/main/java/com/english/pattern/Pattern.java` — examples @OneToMany 구조
- `backend/src/main/java/com/english/generate/GeneratedSentence.java` — situations, sentenceWords @OneToMany 구조
- `backend/src/main/java/com/english/generate/GeneratedSentenceWord.java` — wordId 필드

## 작업

기존 메서드는 다른 서비스(WordService, PatternService, GenerateService)에서 사용 중이므로 **삭제하지 않고 유지**한다. 아래 메서드를 **추가**만 한다.

### 1. ReviewItemRepository — Pageable 오버로드 추가

기존 `findTodayCards` 메서드를 유지하고, `Pageable` 파라미터를 받는 오버로드를 추가한다.

```java
// 시그니처만 제시 — @Query는 기존 findTodayCards와 동일한 JPQL 사용
List<ReviewItem> findTodayCards(User user, String itemType, LocalDate today, List<Long> excludeIds, Pageable pageable);
```

- 반환 타입은 `List<ReviewItem>` (Page가 아님 — count 쿼리 불필요)
- `import org.springframework.data.domain.Pageable` 추가

### 2. WordRepository — IN 배치 조회 추가

```java
@Query("SELECT w FROM Word w WHERE w.id IN :ids AND w.user = :user AND w.deleted = false")
List<Word> findByIdInAndUserAndDeletedFalse(@Param("ids") List<Long> ids, @Param("user") User user);
```

### 3. PatternRepository — IN + JOIN FETCH examples 추가

```java
@Query("SELECT DISTINCT p FROM Pattern p LEFT JOIN FETCH p.examples WHERE p.id IN :ids AND p.user = :user AND p.deleted = false")
List<Pattern> findByIdInWithExamples(@Param("ids") List<Long> ids, @Param("user") User user);
```

- `DISTINCT` 필수 — LEFT JOIN FETCH가 row를 뻥튀기하므로 중복 제거
- Pattern은 `@OneToMany` 1개(examples)만 있으므로 MultipleBagFetchException 위험 없음

### 4. GeneratedSentenceRepository — 2개 메서드 추가

**4-A. SENTENCE 카드용 배치 조회** (situations JOIN FETCH):

```java
@Query("SELECT DISTINCT gs FROM GeneratedSentence gs LEFT JOIN FETCH gs.situations WHERE gs.id IN :ids")
List<GeneratedSentence> findByIdInWithSituations(@Param("ids") List<Long> ids);
```

- `sentenceWords`는 절대 JOIN FETCH하지 마라. 이유: GeneratedSentence에 @OneToMany List가 2개(situations, sentenceWords)이므로 동시 FETCH 시 MultipleBagFetchException 발생

**4-B. WORD 예문용 배치 조회** (Object[] 프로젝션으로 wordId 매핑):

```java
@Query("SELECT sw.wordId, gs FROM GeneratedSentence gs JOIN gs.sentenceWords sw WHERE sw.wordId IN :wordIds")
List<Object[]> findByWordIdInWithMapping(@Param("wordIds") List<Long> wordIds);
```

- Object[0] = wordId(Long), Object[1] = GeneratedSentence
- JOIN FETCH가 아닌 단순 JOIN + 프로젝션. Hibernate collection filtering 동작에 의존하지 않는 안전한 방식

## Acceptance Criteria

```bash
cd backend && ./gradlew compileJava    # 컴파일 성공 — JPQL 문법 오류 없음
cd backend && ./gradlew test           # 기존 테스트 회귀 없음 (새 메서드 추가만이므로 기존 테스트 영향 없음)
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 컴파일 성공과 기존 테스트 통과를 확인한다.
3. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가? → Repository는 각 도메인 패키지 내 위치
   - ADR 기술 스택을 벗어나지 않았는가? → Spring Data JPA, JPQL 사용
   - CLAUDE.md CRITICAL 규칙을 위반하지 않았는가? → 백엔드 코드는 backend/ 폴더
   - Step Contract의 Write Scope 밖을 수정하지 않았는가?
4. 결과에 따라 `phases/4-review-n-plus-one/index.json`의 step 0을 업데이트한다.

## 금지사항

- 기존 Repository 메서드를 삭제하거나 수정하지 마라. 이유: WordService, PatternService, GenerateService 등 다른 서비스에서 사용 중
- GeneratedSentence의 situations와 sentenceWords를 동시에 LEFT JOIN FETCH하지 마라. 이유: MultipleBagFetchException 발생
- ReviewService 로직을 변경하지 마라. 이유: Step 1의 scope
- 기존 테스트를 수정하지 마라. 이유: 메서드 추가만이므로 기존 테스트에 영향 없어야 함

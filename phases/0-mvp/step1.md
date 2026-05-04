# Step 1: 단어 CRUD (TDD)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 특히 "Step 1: 단어 CRUD" 섹션과 "핵심 알고리즘" 중 "예문 생성 시 단어 선택" 부분
- `/docs/ARCHITECTURE.md` — 디렉토리 구조, 데이터 모델 관계도
- `/docs/ADR.md` — ADR-004 Polymorphic Association
- `/docs/UI_GUIDE.md` — 색상, 컴포넌트 스타일, 레이아웃 규칙
- `/backend/src/main/java/com/english/config/GeminiClient.java` — AI 보강에 사용
- `/backend/src/main/java/com/english/config/GlobalExceptionHandler.java` — 예외 처리
- `/backend/src/main/java/com/english/study/StudyRecordService.java` — 학습 기록 연동
- `/backend/src/main/java/com/english/review/ReviewItemService.java` — 복습 아이템 자동 생성

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 백엔드: TDD 순서로 진행

#### 1. Word Entity

`backend/src/main/java/com/english/word/Word.java`:

```java
// id, word(UNIQUE), meaning, partOfSpeech, pronunciation, synonyms, tip, isImportant, deleted, createdAt
```

#### 2. WordRepository

`backend/src/main/java/com/english/word/WordRepository.java`:

```java
// JpaRepository<Word, Long>
// 검색, 필터링, 정렬을 위한 쿼리 메서드
// deleted=false 조건 기본 적용
```

#### 3. WordService 테스트 먼저

`backend/src/test/java/com/english/word/WordServiceTest.java`:

테스트 케이스:
- 단건 등록 성공 + AI 보강 호출 확인
- 단건 등록 시 AI 보강 실패 → 보강 없이 저장
- 벌크 등록 성공 (saved/skipped/enrichmentFailed 카운트)
- 중복 단어 등록 → DuplicateException
- 빈 배열 벌크 → EmptyRequestException
- 목록 조회 (페이지네이션)
- 검색/필터 (search, partOfSpeech, importantOnly, sort)
- 상세 조회 + 예문 목록
- 존재하지 않는 ID → NotFoundException
- 중요 토글
- soft delete → WORD review_items만 삭제
- 등록 시 study_records 자동 생성 확인
- 등록 시 study_record_items 추가 확인
- 등록 시 review_items 2개(RECOGNITION+RECALL) 생성 확인

#### 4. WordService 구현

`backend/src/main/java/com/english/word/WordService.java`:

```java
public WordResponse create(WordCreateRequest request)
public BulkCreateResponse bulkCreate(List<WordCreateRequest> requests)
public Page<WordListResponse> getList(String search, String partOfSpeech, boolean importantOnly, String sort, Pageable pageable)
public WordDetailResponse getDetail(Long id)
public WordResponse toggleImportant(Long id)
public void delete(Long id)
```

핵심 규칙:
- 등록 시 GeminiClient로 AI 보강 요청. 실패 시 보강 없이 저장 (word+meaning만)
- 등록 시 StudyRecordService.getOrCreateTodayRecord() + addItem(WORD, wordId)
- 등록 시 ReviewItemService.createWordReviewItems(wordId)
- soft delete 시 ReviewItemRepository에서 item_type=WORD, item_id=wordId인 항목 deleted=true
- 예문(generated_sentences)은 삭제하지 않음

#### 5. WordController 테스트

`backend/src/test/java/com/english/word/WordControllerTest.java`:

- MockMvc 사용
- 각 엔드포인트의 HTTP 상태코드, 응답 형식 검증
- 에러 케이스의 에러코드 검증

#### 6. WordController 구현

`backend/src/main/java/com/english/word/WordController.java`:

```java
@RestController @RequestMapping("/api/words")
POST   /api/words          → 201
POST   /api/words/bulk      → 201
GET    /api/words           → 200 (페이지네이션)
GET    /api/words/{id}      → 200
PATCH  /api/words/{id}/important → 200
DELETE /api/words/{id}      → 204
```

#### 7. DTO 클래스

- `WordCreateRequest`: word, meaning
- `WordResponse`: id, word, meaning, partOfSpeech, pronunciation, synonyms, tip, isImportant, createdAt
- `WordListResponse`: id, word, meaning, partOfSpeech, isImportant, createdAt
- `WordDetailResponse`: WordResponse + examples (예문 목록)
- `BulkCreateResponse`: saved, skipped, enrichmentFailed, words

### 프론트엔드: 단어 페이지

#### 8. 단어 목록 페이지

`frontend/app/words/page.tsx`:

- 단어 목록 카드 UI (UI_GUIDE.md 스타일 적용)
- 검색바 + 필터 (품사, 중요만)
- 정렬 (최신순, 이름순)
- 페이지네이션
- 단어 추가 버튼 → 등록 모달/페이지

#### 9. 단어 상세 페이지

`frontend/app/words/[id]/page.tsx`:

- 단어 정보 카드
- AI 보강 정보 (품사, 발음, 유의어, 팁)
- 관련 예문 목록
- 중요 토글 버튼 (⭐)
- 삭제 버튼

#### 10. 단어 등록 컴포넌트

- 단건 등록 폼
- 벌크 등록 (JSON 붙여넣기)
- 등록 성공/실패 피드백 토스트

#### 11. 하단 네비게이션

`frontend/app/components/BottomNav.tsx`:

- [🏠 홈] [📖 단어] [🔤 패턴] [✨ 생성] [🃏 복습]
- 현재 페이지 활성 표시
- 모든 페이지에서 공통 사용

## Acceptance Criteria

```bash
cd backend && ./gradlew test      # 단어 관련 테스트 전체 통과
cd frontend && npm run build      # 빌드 성공
cd frontend && npm run lint       # ESLint 통과
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가?
   - ADR 기술 스택을 벗어나지 않았는가?
   - CLAUDE.md CRITICAL 규칙을 위반하지 않았는가?
3. 결과에 따라 `phases/0-mvp/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 패턴, 예문 생성, 복습 로직을 구현하지 마라. 이유: Step 2~4에서 TDD로 진행한다.
- Word Entity에 패턴/예문 관련 연관관계를 추가하지 마라. 이유: 해당 Entity가 아직 없다.
- GeminiClient의 로직을 수정하지 마라. 이유: Step 0에서 완성된 인프라다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

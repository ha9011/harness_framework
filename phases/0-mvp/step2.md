# Step 2: 패턴 등록 + 이미지 추출 (TDD)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 특히 "Step 2: 패턴 등록 + 이미지 추출" 섹션
- `/docs/ARCHITECTURE.md` — 디렉토리 구조, 데이터 모델 (patterns, pattern_examples)
- `/docs/ADR.md` — ADR-003 Gemini API, ADR-004 Polymorphic Association
- `/docs/UI_GUIDE.md` — 색상, 컴포넌트 스타일
- `/backend/src/main/java/com/english/config/GeminiClient.java` — generateContentWithImage 사용
- `/backend/src/main/java/com/english/word/` — Word 도메인 구조 참고 (동일 패턴 적용)
- `/backend/src/main/java/com/english/study/StudyRecordService.java`
- `/backend/src/main/java/com/english/review/ReviewItemService.java`

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 백엔드: TDD 순서로 진행

#### 1. Pattern + PatternExample Entity

`backend/src/main/java/com/english/pattern/`:

```java
// Pattern: id, template(UNIQUE), description, deleted, createdAt
// PatternExample: id, patternId, sentence, translation, orderIndex
//   orderIndex: 교재 예문 순서 보존용 (0부터)
```

#### 2. PatternService 테스트 먼저

`backend/src/test/java/com/english/pattern/PatternServiceTest.java`:

테스트 케이스:
- 직접 등록 성공 + 교재 예문 순서 보존
- 중복 패턴 → DuplicateException
- soft delete → PATTERN review_items만 삭제
- 등록 시 study_records 자동 생성
- 등록 시 review_items 2개(RECOGNITION+RECALL) 생성

#### 3. PatternService 구현

```java
public PatternResponse create(PatternCreateRequest request)
public Page<PatternListResponse> getList(Pageable pageable)
public PatternDetailResponse getDetail(Long id)
public void delete(Long id)
```

핵심 규칙:
- 등록 시 StudyRecordService + ReviewItemService 연동
- soft delete 시 PATTERN review_items만 삭제

#### 4. 이미지 추출 서비스 테스트

테스트 케이스:
- 패턴 이미지 추출 성공 (GeminiClient.generateContentWithImage mock)
- 단어 이미지 추출 성공
- 이미지 형식 오류 → InvalidImageException
- 추출 결과 없음 → 빈 객체
- Gemini 장애 (3회 실패) → GeminiException

#### 5. 이미지 추출 서비스 구현

```java
// PatternService 내부 또는 별도 ExtractService
public PatternExtractResponse extractFromImage(MultipartFile image)
// WordService에 추가
public WordExtractResponse extractWordsFromImage(MultipartFile image)
```

핵심 규칙:
- 지원 형식: JPEG, PNG, WebP, GIF (MIME 타입 체크)
- Gemini Vision API로 이미지 전달 + JSON 응답 파싱
- 추출만 하고 저장하지 않음 (사용자가 확인 후 등록)

#### 6. PatternController + 테스트

```java
@RestController @RequestMapping("/api/patterns")
POST   /api/patterns           → 201
POST   /api/patterns/extract   → 200
GET    /api/patterns            → 200 (페이지네이션)
GET    /api/patterns/{id}       → 200
DELETE /api/patterns/{id}       → 204

// WordController에 추가
POST   /api/words/extract       → 200
```

### 프론트엔드: 패턴 페이지

#### 7. 패턴 목록 페이지

`frontend/app/patterns/page.tsx`:

- 패턴 목록 카드 UI
- 패턴 추가 버튼

#### 8. 패턴 상세 페이지

`frontend/app/patterns/[id]/page.tsx`:

- 패턴 정보 + 교재 예문 목록
- 삭제 버튼

#### 9. 패턴 등록 + 이미지 추출

- 직접 입력 폼 (패턴 + 설명 + 예문 10개)
- 이미지 업로드 → 추출 결과 미리보기 → 확인 후 등록
- 단어 이미지 추출도 단어 등록 페이지에 추가

## Acceptance Criteria

```bash
cd backend && ./gradlew test      # 패턴 + 이미지 추출 테스트 통과
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

- 예문 생성 로직을 구현하지 마라. 이유: Step 3에서 TDD로 진행한다.
- generated_sentences 테이블이나 관련 Entity를 만들지 마라. 이유: Step 3에서 생성한다.
- 복습 로직(SM-2)을 구현하지 마라. 이유: Step 4에서 진행한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

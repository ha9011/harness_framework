# Step 3: 예문 생성 (TDD)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 특히 "Step 3: 예문 생성" 섹션, "예문 생성 시 단어 선택" 알고리즘, "구현 시 자주 헷갈리는 규칙"
- `/docs/ARCHITECTURE.md` — 데이터 모델 (generated_sentences, generation_history, sentence_situations, generated_sentence_words)
- `/docs/ADR.md` — ADR-003 Gemini API
- `/docs/UI_GUIDE.md`
- `/backend/src/main/java/com/english/config/GeminiClient.java`
- `/backend/src/main/java/com/english/word/` — Word Entity, WordRepository
- `/backend/src/main/java/com/english/pattern/` — Pattern Entity
- `/backend/src/main/java/com/english/review/ReviewItemService.java` — createSentenceReviewItem

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 백엔드: TDD 순서로 진행

#### 1. Entity 생성

`backend/src/main/java/com/english/generate/`:

```java
// GeneratedSentence: id, englishSentence, koreanTranslation, level, createdAt
// SentenceSituation: id, sentenceId, situation (5개/예문)
// GeneratedSentenceWord: id, sentenceId, wordId (다대다 매핑)
// GenerationHistory: id, level, requestedCount, actualCount, wordId(nullable), patternId(nullable), createdAt
```

핵심:
- GeneratedSentenceWord는 Gemini가 반환한 wordIds 중 DB에 실제 존재하는 것만 매핑
- GenerationHistory.wordId: POST /api/generate/word 시 지정 단어 ID
- GenerationHistory.patternId: POST /api/generate/pattern 시 지정 패턴 ID
- 일반 생성: 둘 다 null

#### 2. GenerateService 테스트 먼저

`backend/src/test/java/com/english/generate/GenerateServiceTest.java`:

테스트 케이스:
- 일반 생성 성공: level + count → Gemini 호출 → 예문 저장 + situation 5개 + word 매핑
- 단어 지정 생성: wordId로 해당 단어 포함 예문 생성
- 패턴 지정 생성: patternId로 해당 패턴 사용 예문 생성
- 단어 선택 우선순위: ⭐중요 > 복습 적은 것 > 랜덤
- 최대 50개 단어 선택 제한
- Gemini가 잘못된 wordId 반환 → 예문 저장, 매핑만 무시
- 요청 30개인데 25개만 생성 → actualCount=25
- 단어 0개 → NoWordsException
- 패턴 0개 → NoPatternsException
- 예문마다 review_items (SENTENCE RECOGNITION) 자동 생성 확인
- generation_history 기록 확인

#### 3. GenerateService 구현

```java
public GenerateResponse generate(GenerateRequest request)           // 일반 생성
public GenerateResponse generateByWord(Long wordId, GenerateRequest request)     // 단어 지정
public GenerateResponse generateByPattern(Long patternId, GenerateRequest request) // 패턴 지정
public Page<GenerationHistoryResponse> getHistory(Pageable pageable)
```

단어 선택 알고리즘 (PLAN.md 참조):
1. deleted=false인 단어 전체 조회
2. 정렬: is_important=true 먼저 → review_count 낮은 순(WORD RECOGNITION) → 랜덤
3. 상위 최대 50개 선택
4. Gemini 프롬프트에 `[{"id":1, "word":"...", "meaning":"..."}]` 전달

난이도 기준:
- 유아(TODDLER): 3~5단어, 아주 쉬운 문장
- 초등(ELEMENTARY): 카페 주문 수준
- 중등(INTERMEDIATE): 카카오톡 대화 수준
- 고등(ADVANCED): 회사 대화 수준

#### 4. GenerateController + 테스트

```java
@RestController @RequestMapping("/api/generate")
POST   /api/generate              → 201
POST   /api/generate/word         → 201 (body에 wordId)
POST   /api/generate/pattern      → 201 (body에 patternId)
GET    /api/generate/history      → 200 (페이지네이션)
```

### 프론트엔드: 예문 생성 페이지

#### 5. 생성 페이지

`frontend/app/generate/page.tsx`:

- 난이도 선택 (유아/초등/중등/고등)
- 개수 선택 (10/20/30)
- 생성 버튼
- 생성 결과 표시: 예문 카드 목록 (영어 + 한국어 + 상황)
- 생성 이력 탭

#### 6. 생성 결과 카드

- 영어 문장 + 상황(situation) 표시
- 한국어 해석은 탭/클릭 시 펼침
- SituationCloud 컴포넌트 (UI_GUIDE.md 참고)

## Acceptance Criteria

```bash
cd backend && ./gradlew test      # 예문 생성 테스트 전체 통과
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

- 복습 로직(SM-2, 카드 선정)을 구현하지 마라. 이유: Step 4에서 진행한다.
- 대시보드/통계 로직을 구현하지 마라. 이유: Step 5에서 진행한다.
- Gemini 프롬프트에 하드코딩된 단어 목록을 넣지 마라. 이유: 반드시 DB에서 단어를 조회하여 동적으로 구성해야 한다.
- response_schema 없이 Gemini를 호출하지 마라. 이유: JSON 파싱 실패 위험. GeminiClient가 제공하는 구조화된 응답을 사용하라.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

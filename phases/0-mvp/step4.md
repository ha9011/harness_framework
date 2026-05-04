# Step 4: 복습 시스템 (TDD)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 특히 "Step 4: 복습 시스템", "SM-2 기반 커스텀 간격 반복", "복습 카드 선정", "구현 시 자주 헷갈리는 규칙"
- `/docs/ARCHITECTURE.md` — 데이터 모델 (review_items, review_logs)
- `/docs/ADR.md` — ADR-005 SM-2, ADR-006 복습 탭 분리
- `/docs/UI_GUIDE.md` — 카드 플립 애니메이션, TabPills, 레이아웃
- `/backend/src/main/java/com/english/review/` — ReviewItem Entity, ReviewItemRepository, ReviewItemService
- `/backend/src/main/java/com/english/generate/` — GeneratedSentence, SentenceSituation (SENTENCE 카드용)
- `/backend/src/main/java/com/english/word/` — Word (WORD 카드 뒷면 예문 조회용)

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 백엔드: TDD 순서로 진행

#### 1. ReviewLog Entity

`backend/src/main/java/com/english/review/ReviewLog.java`:

```java
// id, reviewItemId, result(EASY/MEDIUM/HARD), previousInterval, newInterval, previousEaseFactor, newEaseFactor, createdAt
```

#### 2. ReviewService 테스트 먼저

`backend/src/test/java/com/english/review/ReviewServiceTest.java`:

테스트 케이스:

**카드 선정:**
- type=WORD → WORD 타입만 반환
- type=PATTERN → PATTERN 타입만 반환
- type=SENTENCE → SENTENCE 타입만 반환
- next_review_date <= 오늘인 것만 선정
- deleted=true인 것 제외
- 우선순위: HARD 먼저 → 오래된 순 → 복습 적은 순
- daily_review_count 개수만큼 (부족하면 있는 만큼)
- 랜덤 셔플 후 반환
- exclude 파라미터로 이미 한 카드 제외
- 복습 대상 0개 → 빈 배열
- SENTENCE 카드 front에 situation 포함 (서버가 5개 중 랜덤 1개 선택)
- WORD RECOGNITION 카드 back에 examples (최대 3개, 4개 이상이면 랜덤 3개)

**SM-2 적용:**
- EASY: interval × ease_factor × 1.3, ease_factor += 0.15
- MEDIUM: interval × ease_factor, ease_factor 유지
- HARD: interval = 1, ease_factor = max(1.3, ef - 0.2)
- next_review_date = 오늘 + new_interval
- review_count 증가
- review_log 기록

#### 3. ReviewService 구현

```java
public List<ReviewCardResponse> getTodayCards(String type, List<Long> exclude)
public ReviewResultResponse submitResult(Long reviewItemId, String result)
```

카드 선정 알고리즘 (PLAN.md 그대로):
1. WHERE next_review_date <= 오늘 AND deleted=false AND item_type=type AND id NOT IN (exclude)
2. ORDER BY: last_result='HARD' 먼저 → last_reviewed_at ASC → review_count ASC
3. LIMIT N (UserSetting.dailyReviewCount, 기본 10)
4. 랜덤 셔플

카드 응답 구성:
- WORD RECOGNITION: front={word}, back={meaning, partOfSpeech, examples(최대3)}
- WORD RECALL: front={meaning}, back={word, partOfSpeech}
- PATTERN RECOGNITION: front={template}, back={description, examples}
- PATTERN RECALL: front={description}, back={template}
- SENTENCE RECOGNITION: front={englishSentence, situation(랜덤1)}, back={koreanTranslation}

#### 4. ReviewController + 테스트

```java
@RestController @RequestMapping("/api/reviews")
GET    /api/reviews/today?type=WORD&exclude=1,2,3  → 200
POST   /api/reviews/{id}                            → 200 (body: result=EASY/MEDIUM/HARD)
```

### 프론트엔드: 복습 페이지

#### 5. 복습 메인 페이지

`frontend/app/review/page.tsx`:

- TabPills: [단어][패턴][문장] — 각 탭에 남은 카드 수 뱃지
- 탭 전환 시 해당 타입 카드 로드

#### 6. 카드 플립 컴포넌트

`frontend/app/components/FlipCard.tsx`:

- CSS transform rotateY(180deg), transition 0.4s ease
- 앞면/뒷면 콘텐츠
- 탭/클릭으로 플립

#### 7. 복습 플로우

- 카드 표시 → 플립 → EASY/MEDIUM/HARD 버튼
- 다음 카드로 진행
- 모든 카드 완료 → 완료 화면
- "처음부터 다시" 버튼: 현재 카드 배열을 인덱스 0부터 재표시 (읽기 전용, SM-2 미적용)
- "추가 복습" 버튼: exclude에 현재까지 한 카드 ID 넣고 API 재호출

## Acceptance Criteria

```bash
cd backend && ./gradlew test      # 복습 관련 테스트 전체 통과
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

- 대시보드/통계/학습 기록 조회 로직을 구현하지 마라. 이유: Step 5에서 진행한다.
- UserSetting Entity를 만들지 마라. 이유: Step 5에서 생성한다. dailyReviewCount는 기본값 10으로 하드코딩하라.
- "처음부터 다시"에서 SM-2를 적용하지 마라. 이유: 읽기 전용 재표시. SM-2 이중 적용 방지 (PRD 참조).
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

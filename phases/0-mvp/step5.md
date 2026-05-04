# Step 5: 학습 기록 + 설정 + 대시보드

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 특히 "Step 5: 학습 기록 + 설정 + 대시보드", "구현 시 자주 헷갈리는 규칙" (streak, day_number)
- `/docs/ARCHITECTURE.md` — 데이터 모델 (study_records, user_settings), 디렉토리 구조
- `/docs/UI_GUIDE.md` — 대시보드 디자인, 색상 팔레트, 커피나무 시맨틱 색상
- `/design/coffee-tree.jsx` — 커피나무 성장 SVG 참고
- `/design/screens-home.jsx` — 홈 화면 디자인 참고
- `/backend/src/main/java/com/english/study/` — StudyRecord, StudyRecordService
- `/backend/src/main/java/com/english/review/` — ReviewItem (복습 남은 개수 계산용)
- `/backend/src/main/java/com/english/word/` — Word (카운트용)
- `/backend/src/main/java/com/english/pattern/` — Pattern (카운트용)
- `/backend/src/main/java/com/english/generate/` — GeneratedSentence (카운트용)

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 백엔드: TDD 순서로 진행

#### 1. UserSetting Entity

`backend/src/main/java/com/english/setting/`:

```java
// UserSetting: id, dailyReviewCount(기본 10), createdAt, updatedAt
// 단일 레코드 (MVP에서 사용자 1명)
```

#### 2. SettingService + 테스트

```java
public UserSettingResponse getSetting()
public UserSettingResponse updateSetting(SettingUpdateRequest request)
```

테스트:
- 설정 조회 (없으면 기본값으로 자동 생성)
- 설정 변경 (dailyReviewCount: 10/20/30)

#### 3. DashboardService 테스트 먼저

`backend/src/test/java/com/english/dashboard/DashboardServiceTest.java`:

테스트 케이스:
- 단어/패턴/예문 카운트 (deleted=false만)
- streak 계산: 오늘 복습함 → 오늘부터 역순 연속일
- streak 계산: 오늘 안 함, 어제 함 → 어제부터 역순 연속일
- streak 계산: 복습 기록 없음 → 0
- 타입별 todayReviewRemaining (WORD/PATTERN/SENTENCE 각각)
- 최근 학습 기록 5개
- 첫 사용자 (모든 카운트 0)

#### 4. DashboardService 구현

```java
public DashboardResponse getDashboard()
```

응답:
```java
// wordCount, patternCount, sentenceCount
// streak (연속 복습일)
// todayReviewRemaining: { word: N, pattern: N, sentence: N }
// recentStudyRecords: 최근 5개
```

streak 계산:
- review_logs 테이블에서 날짜별 그룹핑
- 오늘 기록 있으면 오늘부터, 없으면 어제부터 시작
- 연속된 날짜 카운트

#### 5. StudyRecordService 확장 + 테스트

기존 getOrCreateTodayRecord(), addItem()에 추가:

```java
public Page<StudyRecordResponse> getRecords(Pageable pageable)
```

테스트:
- 학습 기록 목록 조회 (최신순, 페이지네이션)
- day_number 올바르게 표시

#### 6. Controller + 테스트

```java
// DashboardController
@RestController @RequestMapping("/api/dashboard")
GET    /api/dashboard          → 200

// SettingController
@RestController @RequestMapping("/api/settings")
GET    /api/settings           → 200
PUT    /api/settings           → 200

// StudyRecordController
@RestController @RequestMapping("/api/study-records")
GET    /api/study-records      → 200 (페이지네이션)
```

#### 7. ReviewService 수정: dailyReviewCount 연동

Step 4에서 하드코딩했던 기본값 10을 UserSetting에서 읽도록 변경:

```java
// ReviewService.getTodayCards() 내부
int limit = settingService.getSetting().getDailyReviewCount();
```

### 프론트엔드

#### 8. 홈 대시보드

`frontend/app/page.tsx`:

- 통계 카드: 단어/패턴/예문 수
- 오늘 복습 남은 개수 (타입별)
- 커피나무 성장 SVG (streak 연동)
  - streak 0: 씨앗
  - streak 1~3: 새싹
  - streak 4~7: 작은 나무
  - streak 8~14: 꽃 핀 나무
  - streak 15+: 열매 맺은 나무
  - streak 끊김: 시들음 표시 (경고 색상)
- 최근 학습 기록 목록
- 빈 상태: "단어 등록하러 가기" 링크

#### 9. 학습 기록 페이지

`frontend/app/history/page.tsx`:

- Day N 형식으로 학습 기록 목록
- 각 기록에 등록한 단어/패턴 수 표시

#### 10. 설정 페이지

`frontend/app/settings/page.tsx`:

- 하루 복습 개수 선택 (10/20/30)
- 저장 버튼

## Acceptance Criteria

```bash
cd backend && ./gradlew test      # 대시보드, 설정, 학습기록 테스트 통과
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

- 통합 테스트(TestContainers)를 작성하지 마라. 이유: Step 6에서 진행한다.
- 커피나무 SVG를 처음부터 만들지 마라. `/design/coffee-tree.jsx`를 참고하여 React 컴포넌트로 변환하라.
- ReviewService의 SM-2 로직이나 카드 선정 로직을 변경하지 마라. 이유: dailyReviewCount 연동만 하면 된다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

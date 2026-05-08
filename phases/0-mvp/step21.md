# Step 21: frontend-history-screen

## Step Contract

- Capability: study history screen
- Layer: frontend-view
- Write Scope: `frontend/`
- Out of Scope: backend files, dashboard aggregation, word/pattern registration forms
- Critical Gates: `cd frontend && npm run test -- --run history`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/docs/UI_GUIDE.md`
- `/API설계서.md`
- `/design/screens-other.jsx`
- `/frontend/app/lib/api.ts`

## 작업

학습 기록 화면을 구현한다.

필수 구현:
- `/history`에서 `GET /api/study-records?page&size`를 호출한다.
- 날짜별 최신순 목록을 표시한다.
- 각 항목은 `Day N`, 날짜, 등록한 패턴 chip, 등록한 단어 chip을 표시한다.
- 페이지네이션 또는 더 보기 동작을 제공한다.
- loading, error, empty 상태를 구현한다.
- 모바일 shell에서 홈 계열 보조 화면으로 자연스럽게 뒤로갈 수 있어야 한다.

## Acceptance Criteria

```bash
cd frontend && npm run test -- --run history
cd frontend && npm run build
```

## 검증 절차

1. MSW로 페이지네이션 응답을 검증한다.
2. WORD/PATTERN item 표시 이름과 chip 스타일을 확인한다.
3. empty 상태에서 다음 행동이 명확한지 확인한다.
4. Step 21을 `completed`로 표시하고 summary에 history 화면 구현을 적는다.

## 금지사항

- 학습 기록 생성 form을 만들지 마라. 이유: 기록은 단어/패턴 등록 시 자동 생성된다.
- backend API를 수정하지 마라. 이유: frontend-view step이다.
- 설정 화면과 섞지 마라. 이유: Step 22에서 별도 구현한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

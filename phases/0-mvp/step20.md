# Step 20: frontend-home-dashboard

## Step Contract

- Capability: home dashboard screen
- Layer: frontend-view
- Write Scope: `frontend/`
- Out of Scope: backend files, word/pattern forms, review card implementation, settings mutation
- Critical Gates: `cd frontend && npm run test -- --run dashboard`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/docs/UI_GUIDE.md`
- `/API설계서.md`
- `/design/screens-home.jsx`
- `/design/coffee-tree.jsx`
- `/frontend/app/lib/api.ts`
- `/frontend/app/lib/auth-context.tsx`

## 작업

홈 대시보드를 Cozy Cafe & Coffee Tree UI로 구현한다.

필수 구현:
- `/`에서 `GET /api/dashboard`를 TanStack Query로 호출한다.
- 닉네임 인사말을 표시한다.
- 오늘 남은 복습 수를 word/pattern/sentence로 분리 표시한다.
- 큰 `오늘의 복습 시작` CTA는 `/review`로 이동한다.
- 누적 통계: 단어, 패턴, 예문, streak.
- 최근 학습 기록 최대 5건.
- 커피나무 성장 영역은 streak/day 정보를 기반으로 시각 상태를 보여준다.
- loading, error, empty 상태를 모두 구현한다.
- 모바일 하단 내비게이션에서 홈이 active여야 한다.

## Acceptance Criteria

```bash
cd frontend && npm run test -- --run dashboard
cd frontend && npm run build
```

## 검증 절차

1. MSW로 dashboard data/loading/error/empty 상태를 검증한다.
2. 복습 시작 버튼이 `/review`로 이동하는지 확인한다.
3. 모바일 폭에서 텍스트가 겹치지 않는지 테스트 또는 렌더 검토로 확인한다.
4. Step 20을 `completed`로 표시하고 summary에 홈 대시보드 상태 구현을 적는다.

## 금지사항

- backend dashboard API를 수정하지 마라. 이유: 이 step은 frontend-view 전용이다.
- 마케팅 hero 페이지를 만들지 마라. 이유: 첫 화면은 실제 학습 대시보드여야 한다.
- gradient orb 또는 보라색 AI 테마를 추가하지 마라. 이유: UI_GUIDE 위반이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

# Step 26: frontend-review-screen

## Step Contract

- Capability: tabbed review flip-card screen
- Layer: frontend-view
- Write Scope: `frontend/`
- Out of Scope: backend files, generate screen, settings service changes
- Critical Gates: `cd frontend && npm run test -- --run review`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/docs/UI_GUIDE.md`
- `/API설계서.md`
- `/design/screens-review.jsx`
- `/frontend/app/lib/api.ts`

## 작업

복습 플립 카드 화면을 구현한다.

필수 구현:
- `/review`는 WORD/PATTERN/SENTENCE 탭을 가진다.
- 탭 선택 시 `GET /api/reviews/today?type=...`를 호출한다.
- 각 탭은 독립 deck과 진행률을 가진다.
- 카드 click/tap으로 앞뒤 flip을 구현한다.
- WORD/PATTERN/SENTENCE별 front/back 구조를 API 응답에 맞춰 표시한다.
- SENTENCE 앞면에는 상황 말풍선을 표시한다.
- EASY/MEDIUM/HARD 버튼은 `POST /api/reviews/{reviewItemId}`를 호출한다.
- 완료 후 `처음부터 다시`는 같은 deck을 읽기 전용으로 보여주고 SM-2 mutation을 호출하지 않는다.
- `추가 복습`은 이미 복습한 reviewItemId를 exclude로 넘겨 다음 deck을 가져온다.
- 더 이상 없으면 빈 상태 메시지를 보여준다.

## Acceptance Criteria

```bash
cd frontend && npm run test -- --run review
cd frontend && npm run build
```

## 검증 절차

1. 탭별 API 호출과 deck 진행률을 MSW로 검증한다.
2. flip 상태와 읽기 전용 다시보기 모드를 확인한다.
3. EASY/MEDIUM/HARD mutation과 추가 복습 exclude를 확인한다.
4. Step 26을 `completed`로 표시하고 summary에 review 화면 구현을 적는다.

## 금지사항

- backend review API를 수정하지 마라. 이유: frontend-view step이다.
- 읽기 전용 다시보기에서 SM-2 mutation을 호출하지 마라. 이유: PRD 요구사항이다.
- SENTENCE 카드에 회상 모드를 만들지 마라. 이유: SENTENCE는 인식만 지원한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

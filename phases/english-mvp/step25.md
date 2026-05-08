# Step 25: frontend-generate-screen

## Step Contract

- Capability: sentence generation screen
- Layer: frontend-view
- Write Scope: `frontend/`
- Out of Scope: backend files, review screen, word/pattern CRUD screen changes unless shared component fix is necessary
- Critical Gates: `cd frontend && npm run test -- --run generate`

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

예문 생성 화면을 구현한다.

필수 구현:
- `/generate`에서 난이도 유아/초등/중등/고등을 선택한다.
- 개수 10/20/30을 선택한다.
- `POST /api/generate` mutation을 호출한다.
- Gemini 대기 중 Cozy Cafe 로딩 UI를 표시한다.
- 결과는 영어 문장, 상황 말풍선, 탭/클릭 시 펼쳐지는 한국어 해석, 패턴 태그, 단어 태그를 표시한다.
- NO_WORDS, NO_PATTERNS, AI_SERVICE_ERROR 등 API error를 사용자 메시지로 보여준다.
- loading, error, empty 상태를 구현한다.

## Acceptance Criteria

```bash
cd frontend && npm run test -- --run generate
cd frontend && npm run build
```

## 검증 절차

1. 난이도/개수 선택 payload를 검증한다.
2. 로딩, 성공, 에러 상태를 MSW로 검증한다.
3. 상황/해석/태그 UI가 겹치지 않고 펼쳐지는지 확인한다.
4. Step 25를 `completed`로 표시하고 summary에 generate 화면 구현을 적는다.

## 금지사항

- backend generate API를 수정하지 마라. 이유: frontend-view step이다.
- 결과를 저장 버튼이 필요한 별도 흐름으로 만들지 마라. 이유: 백엔드 생성 API가 이미 저장한다.
- review card UI를 구현하지 마라. 이유: Step 26의 scope다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

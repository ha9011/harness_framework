# Step 27: integration-hardening

## Step Contract

- Capability: end-to-end contract hardening for backend and frontend MVP
- Layer: integration-hardening
- Write Scope: `backend/`, `frontend/`, `package.json`, `README.md`, `MANUAL_TEST.md`
- Out of Scope: new product features, schema redesign, external deployment, MVP excluded features
- Critical Gates: `npm run test`, `npm run lint`, `npm run build`, and manual smoke commands for auth-word-generate-review flow

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/docs/UI_GUIDE.md`
- `/docs/PRD.md`
- `/API설계서.md`
- `/README.md`
- `/MANUAL_TEST.md`
- `/backend/`
- `/frontend/`

## 작업

프론트/백엔드 통합 계약을 검증하고 필요한 최소 조정을 한다.

필수 구현:
- 루트 `npm run dev`, `npm run build`, `npm run lint`, `npm run test`가 현재 프로젝트 구조와 맞는지 확인하고 필요 시 조정한다.
- CORS origin과 Cookie credentials 설정이 프론트 dev server와 맞는지 확인한다.
- 프론트 API client base URL과 백엔드 `/api` prefix가 일치하는지 확인한다.
- API error `{ error, message }`가 프론트에서 일관되게 표시되는지 확인한다.
- 빈 상태: 홈, 단어, 패턴, 예문 생성, 복습, 학습 기록, 설정을 확인한다.
- 모바일 폭에서 주요 텍스트와 버튼이 겹치지 않는지 확인한다.
- `README.md` 또는 `MANUAL_TEST.md`에 실행/검증 절차를 최신화한다.
- 실제 Gemini 호출은 `GEMINI_API_KEY`가 있을 때만 수동 확인 절차로 둔다.

## Acceptance Criteria

```bash
npm run test
npm run lint
npm run build
docker compose up -d
```

핵심 수동 시나리오:
```bash
# 1. 백엔드와 프론트 dev server를 실행한다.
# 2. 회원가입 후 홈 대시보드에 진입한다.
# 3. 단어 1개와 패턴 1개를 등록한다.
# 4. 예문 생성을 시도한다. GEMINI_API_KEY가 없으면 에러 표시가 명확해야 한다.
# 5. 복습 탭에서 생성된 WORD/PATTERN 카드를 확인하고 EASY/MEDIUM/HARD 중 하나를 기록한다.
```

## 검증 절차

1. 전체 테스트, lint, build를 실행한다.
2. 프론트/백엔드 API 계약 불일치를 최소 수정한다.
3. 문서의 실행 안내가 실제 명령과 맞는지 확인한다.
4. Step 27을 `completed`로 표시하고 summary에 통합 검증 결과와 남은 수동 확인 사항을 적는다.

## 금지사항

- 새 기능을 추가하지 마라. 이유: 이 step은 통합 안정화만 담당한다.
- DB 스키마를 대규모 재설계하지 마라. 이유: migration 안정성을 해칠 수 있다.
- Gemini API key를 코드나 문서에 실제 값으로 기록하지 마라. 이유: 비밀 정보 유출 방지다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

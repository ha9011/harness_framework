# Step 22: frontend-settings-screen

## Step Contract

- Capability: daily review count settings screen
- Layer: frontend-view
- Write Scope: `frontend/`
- Out of Scope: backend files, notification settings, account profile changes
- Critical Gates: `cd frontend && npm run test -- --run settings`

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

설정 화면을 구현한다.

필수 구현:
- `/settings`에서 `GET /api/settings`를 호출한다.
- 하루 복습 개수 10/20/30 segmented option을 보여준다.
- 저장 시 `PUT /api/settings/daily_review_count`를 호출한다.
- 타입별 N개씩 총 3N장이라는 보조 정보를 표시한다.
- 저장 중/loading/error/success 상태를 보여준다.
- 로그아웃 진입점은 AuthProvider의 logout을 사용한다.

## Acceptance Criteria

```bash
cd frontend && npm run test -- --run settings
cd frontend && npm run build
```

## 검증 절차

1. settings 조회와 변경 mutation을 MSW로 검증한다.
2. 10/20/30 외 값이 UI에서 선택 불가능한지 확인한다.
3. 저장 실패 시 사용자에게 에러가 보이는지 확인한다.
4. Step 22를 `completed`로 표시하고 summary에 settings 화면 구현을 적는다.

## 금지사항

- 닉네임/비밀번호 변경 UI를 만들지 마라. 이유: MVP 제외 사항이다.
- 알림/백업/내보내기를 실제 기능처럼 구현하지 마라. 이유: API가 없다.
- backend 설정 API를 수정하지 마라. 이유: frontend-view step이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

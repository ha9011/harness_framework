# Step 19: frontend-auth-shell

## Step Contract

- Capability: frontend authentication flow and protected app shell
- Layer: frontend-view
- Write Scope: `frontend/`
- Out of Scope: backend files, learning screens, dashboard data implementation, generate/review screens
- Critical Gates: `cd frontend && npm run test -- --run AuthProvider login signup protected-route`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/docs/UI_GUIDE.md`
- `/API설계서.md`
- `/frontend/app/lib/api.ts`
- `/frontend/app/layout.tsx`

## 작업

로그인/회원가입과 인증 shell을 구현한다.

필수 구현:
- `/login` 화면: email/password form, 실패 메시지 표시.
- `/signup` 화면: email/password/nickname form, password 최소 8자 validation.
- `AuthProvider`와 `useAuth` hook을 만든다.
- 앱 시작 시 `GET /api/auth/me`로 현재 사용자를 확인한다.
- 로그인/회원가입 성공 후 홈으로 이동한다.
- 로그아웃은 `POST /api/auth/logout` 후 로그인 화면으로 이동한다.
- 로그인/회원가입 외 보호 화면 접근 시 미로그인 사용자는 `/login`으로 리다이렉트한다.
- 모든 API 요청은 Cookie 인증을 위해 credentials include를 사용한다.
- Cozy Cafe shell과 하단 내비게이션은 로그인 후 화면에만 표시한다.

## Acceptance Criteria

```bash
cd frontend && npm run test -- --run AuthProvider
cd frontend && npm run test -- --run login
cd frontend && npm run test -- --run signup
cd frontend && npm run build
```

## 검증 절차

1. MSW로 signup/login/me/logout 흐름을 검증한다.
2. JWT 만료나 me 401 시 로그인 화면으로 이동하는지 확인한다.
3. form validation과 서버 error 표시를 확인한다.
4. Step 19를 `completed`로 표시하고 summary에 인증 shell 구현 파일을 적는다.

## 금지사항

- backend 인증 API를 수정하지 마라. 이유: 이미 백엔드 step에서 고정된 계약을 사용해야 한다.
- 학습 데이터를 더미로 넣지 마라. 이유: 화면별 step에서 API 상태를 따로 구현한다.
- 토큰을 localStorage에 저장하지 마라. 이유: JWT는 HttpOnly Cookie 정책이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

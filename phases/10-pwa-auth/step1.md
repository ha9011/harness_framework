# Step 1: pwa-token-storage

## Step Contract

- Capability: 로그인/회원가입 시 JWT 토큰을 localStorage에 저장하고 모든 API 요청에 `Authorization: Bearer` 헤더로 전송, 로그아웃·401 시 토큰 제거 (쿠키는 그대로 병행)
- Layer: frontend-view
- Write Scope: `frontend/lib/auth-token.ts`(신규), `frontend/lib/api.ts`, `frontend/lib/auth-context.tsx`, `frontend/lib/types.ts`, `frontend/lib/__tests__/`(auth-token 헬퍼 테스트 신규)
- Out of Scope: backend 코드 일체, 쿠키 관련 동작 변경(`credentials: "include"` 유지), UI 컴포넌트/페이지 시각 변경, `saved-email.ts` 변경
- Critical Gates: `cd frontend && npx vitest run lib/__tests__/auth-token` (신규 헬퍼 테스트: set 후 get이 값 반환, clear 후 get이 null) 통과 + `cd frontend && npm run build` + `npm run lint` 통과 + 기존 Vitest 회귀 없음

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Phase 10-pwa-auth 전체 계획 (Step 1 = 프론트 토큰 저장)
- `/docs/ADR.md` — **ADR-020**(하이브리드 인증, 로그아웃·401 시 토큰 제거 필수)
- `/docs/ARCHITECTURE.md` — "상태 관리"의 localStorage 토큰 저장 항목
- `/frontend/lib/saved-email.ts` — localStorage 헬퍼 동형 패턴(SSR 가드 `typeof window === "undefined"`). auth-token.ts를 이와 같은 형태로 작성
- `/frontend/lib/api.ts` — `request`/`uploadRequest`의 fetch 래핑. 401 시 `window.dispatchEvent(new Event('unauthorized'))` 처리 존재
- `/frontend/lib/auth-context.tsx` — `login`/`signup`/`logout` 및 'unauthorized' 이벤트 핸들러. `api.post<AuthUser>("/auth/login")` 형태
- `/frontend/lib/types.ts` — `AuthUser { id, email, nickname }`. 이전 step에서 백엔드 로그인/회원가입 응답에 `token`이 추가됨 → 대응 타입 필요

> 이전 step(0) 요약: 백엔드 로그인/회원가입 응답 body에 `token` 필드가 추가되었고(전용 DTO), `JwtAuthenticationFilter`와 `/auth/me`가 `Authorization: Bearer` 헤더를 수용한다. 쿠키도 그대로 발급된다.

## 작업

### 1. `lib/auth-token.ts` (신규) — localStorage 토큰 헬퍼

`saved-email.ts`와 동일한 형태로 작성한다(SSR 가드 포함):

```ts
const TOKEN_KEY = "auth_token";
export function getToken(): string | null { /* typeof window 가드 후 localStorage.getItem */ }
export function setToken(token: string): void { /* localStorage.setItem */ }
export function clearToken(): void { /* localStorage.removeItem */ }
```

### 2. `lib/types.ts` — 로그인/회원가입 응답 타입

로그인/회원가입 응답이 이제 토큰을 포함한다. `AuthUser`는 그대로 두고, 토큰을 포함하는 응답 타입을 추가한다. 예:

```ts
export interface AuthLoginResponse extends AuthUser {
  token: string;
}
```

### 3. `lib/api.ts` — Authorization 헤더 첨부

`request`와 `uploadRequest` 양쪽에서, `getToken()`이 값을 반환하면 요청 헤더에 `Authorization: Bearer <token>`를 추가한다.

- 기존 `credentials: "include"`는 **유지**(쿠키 병행)
- 기존 헤더(`Content-Type` 등)와 병합
- 401 응답 처리부('unauthorized' dispatch)에서 `clearToken()`도 호출(만료 토큰이 매 요청 재전송되는 루프 방지)

### 4. `lib/auth-context.tsx` — 저장/삭제 연결

- `login`/`signup` 성공 시 응답의 `token`을 `setToken(...)`으로 저장(응답 타입을 `AuthLoginResponse`로). `setUser`는 `{id,email,nickname}`만 사용
- `logout` 시 `clearToken()` 호출(기존 `api.post("/auth/logout")` + `setUser(null)` 유지)
- 'unauthorized' 이벤트 핸들러에서도 `clearToken()` 호출(이미 api.ts에서 호출한다면 중복 무해)

### 5. `lib/__tests__/auth-token.test.ts` (신규)

- `setToken("x")` 후 `getToken() === "x"`
- `clearToken()` 후 `getToken() === null`
- (jsdom 환경에서 localStorage 사용 가능)

핵심 규칙:
- 쿠키 흐름을 제거하지 마라. localStorage 토큰은 PWA 폴백이고, 쿠키는 그대로 1차 경로다(`credentials: "include"` 유지).
- 로그아웃·401 시 토큰 제거를 빠뜨리지 마라. 이유(ADR-020): 안 지우면 PWA가 계속 로그인 상태로 남고, 만료 토큰이 무한 재전송된다.

## Acceptance Criteria

```bash
cd frontend && npx vitest run lib/__tests__/auth-token
cd frontend && npm run build
cd frontend && npm run lint
cd frontend && npm test
```

- auth-token 헬퍼 테스트 통과(set/get/clear)
- 빌드/린트 통과, 기존 Vitest 회귀 없음

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - 단순 `npm run build`만으로 completed 처리하지 않았는가? → auth-token 헬퍼가 실제 동작(set/get/clear)함을 테스트로 증명했는가?
   - `api.ts`가 토큰이 있을 때 Authorization 헤더를 첨부하도록 실제로 수정했는가? (`credentials: "include"`는 유지?)
   - 로그아웃·401 경로에서 `clearToken()`이 호출되는가?
3. 아키텍처 체크리스트를 확인한다:
   - ADR-020(쿠키 병행, 로그아웃·401 토큰 제거) 결정을 따르는가?
   - CLAUDE.md CRITICAL(frontend/ 한정) 준수, Write Scope 밖(backend/) 미수정?
   - backend 코드를 동시에 수정하지 않았는가? (이 step은 frontend만)
4. 결과에 따라 `phases/10-pwa-auth/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약(auth-token.ts, api.ts 헤더, login/logout/401 연결)"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- backend 코드(`backend/`)를 수정하지 마라. 이유: 이 step은 frontend 한정 (룰6 위반).
- `credentials: "include"`를 제거하지 마라. 이유: 쿠키는 Safari·데스크톱의 1차 인증 경로. 헤더는 "추가"다.
- 로그아웃·401 시 `clearToken()`을 빠뜨리지 마라. 이유(ADR-020): PWA가 계속 로그인 상태로 남고 만료 토큰이 무한 재전송된다.
- auth-token 헬퍼에 SSR 가드(`typeof window`)를 빼지 마라. 이유: 서버 컴포넌트/SSR에서 localStorage 접근 시 크래시.
- 토큰을 쿠키 대신 완전히 대체하지 마라(쿠키 발급은 백엔드에서 유지됨). 이유: 다층 방어 — 브라우저는 HttpOnly 쿠키 보호 유지.
- UI 컴포넌트/페이지의 시각적 요소를 변경하지 마라. 이유: 이 step은 인증 저장 로직 한정.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라.
- `TODO`/`stub`으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

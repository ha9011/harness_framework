# Step 1: frontend-saved-email

## Step Contract

- Capability: 로그인 페이지 "이메일 저장" 기능 (localStorage 헬퍼 + 체크박스 UI)
- Layer: frontend-view
- Write Scope: `frontend/` 디렉토리 전체
- Out of Scope: `backend/` 디렉토리 수정. 백엔드 API 변경. 비밀번호 저장 기능. 자동 로그인(토큰 갱신)
- Critical Gates: `cd frontend && npx vitest run` (saved-email 헬퍼 테스트 통과) + `cd frontend && npm run build` (SSR 빌드 에러 없음)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 작업 목표와 전체 계획 (이메일 저장 기능 요구사항, SSR 가드 제약사항)
- `/docs/ARCHITECTURE.md` — 프론트엔드 디렉토리 구조, 상태 관리 패턴
- `/docs/UI_GUIDE.md` — 로그인 페이지 UI 스펙 (체크박스 스타일링)
- `frontend/app/login/page.tsx` — 현재 로그인 페이지 구현 (수정 대상)
- `frontend/lib/auth-context.tsx` — 인증 컨텍스트 (참조만, 수정하지 않음)
- `frontend/package.json` — 현재 의존성 확인 (vitest 설치 필요)

IMPORTANT: 이 프로젝트는 Next.js 16을 사용한다. `frontend/AGENTS.md`를 반드시 먼저 읽고, 그 안에 명시된 가이드 문서를 확인한 뒤 작업하라. 기존 학습 데이터와 다를 수 있다.

Step 0에서 변경된 파일: `backend/` 쪽만 변경되었으므로 이 step에 영향 없음.

## 작업

### 1. vitest 설치

```bash
cd frontend && npm install -D vitest
```

`package.json`의 `scripts`에 추가:
```json
"test": "vitest run",
"test:watch": "vitest"
```

### 2. saved-email 헬퍼 테스트 작성 (TDD — RED)

**`frontend/lib/__tests__/saved-email.test.ts`** (신규 파일):

테스트 케이스:
- 저장된 이메일이 없으면 null을 반환한다
- 이메일을 저장하면 다시 읽을 수 있다
- clearSavedEmail로 삭제하면 null을 반환한다

vitest의 jsdom 환경이 필요하다. 파일 상단에 `// @vitest-environment jsdom` 주석을 추가하라.

### 3. saved-email 헬퍼 구현 (GREEN)

**`frontend/lib/saved-email.ts`** (신규 파일):

인터페이스:
```typescript
export function getSavedEmail(): string | null
export function setSavedEmail(email: string): void
export function clearSavedEmail(): void
```

핵심 규칙:
- localStorage key: `"saved_email"`
- `getSavedEmail()`에서 `typeof window === "undefined"` 가드 필수. Next.js SSR 렌더링 시 localStorage에 접근하면 에러가 발생한다. SSR 환경이면 null을 반환하라.

### 4. 로그인 페이지 수정

**`frontend/app/login/page.tsx`** (기존 파일 수정):

추가할 사항:

1. **import**: `saved-email.ts`에서 헬퍼 함수 3개를 import
2. **state 추가**: `rememberEmail` (boolean, 기본값 false)
3. **useEffect 추가**: 컴포넌트 마운트 시 `getSavedEmail()` 호출 → 값이 있으면 email state에 설정 + rememberEmail을 true로 설정. useEffect에 import해야 할 것: `{ useEffect }` from "react"
4. **handleSubmit 수정**: `router.push("/")` 직전에 rememberEmail 체크 여부에 따라 `setSavedEmail(email)` 또는 `clearSavedEmail()` 호출
5. **체크박스 UI 추가**: 에러 메시지 영역(126행 부근)과 로그인 버튼(129행 부근) 사이에 배치

체크박스 UI 스타일 (UI_GUIDE.md 준수):
```tsx
<label className="flex items-center gap-2 px-1 cursor-pointer">
  <input
    type="checkbox"
    checked={rememberEmail}
    onChange={(e) => setRememberEmail(e.target.checked)}
    className="w-4 h-4 accent-primary"
  />
  <span className="text-[13px] text-ink-muted">이메일 저장</span>
</label>
```

핵심 규칙:
- 기존 로그인 로직(폼 제출, 에러 표시, 라우팅)을 변경하지 마라. 이메일 저장 로직만 추가한다.
- `useEffect`의 dependency array는 빈 배열 `[]`로 설정하라 (마운트 시 1회만 실행).

## Acceptance Criteria

```bash
# Critical Gates
cd frontend && npx vitest run                # saved-email 테스트 통과
cd frontend && npm run build                 # SSR 빌드 에러 없음

# 보조 검증
cd frontend && npm run lint                  # ESLint 에러 없음
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates 확인:
   - saved-email 헬퍼의 3개 테스트(get/set/clear)가 모두 통과하는가?
   - Next.js 빌드가 SSR 관련 에러 없이 완료되는가? (typeof window 가드가 제대로 동작하는가?)
3. 아키텍처 체크리스트:
   - ARCHITECTURE.md의 디렉토리 구조를 따르는가? (lib/ 하위에 헬퍼 배치)
   - UI_GUIDE.md의 로그인 페이지 체크박스 스펙을 준수하는가?
   - `backend/` 디렉토리를 수정하지 않았는가?
4. 결과에 따라 `phases/2-login-convenience/index.json`의 step 1을 업데이트한다.

## 금지사항

- `backend/` 디렉토리를 수정하지 마라. 이유: 백엔드/프론트 분리 원칙.
- 비밀번호를 localStorage에 저장하지 마라. 이유: 보안 위험. 이메일만 저장한다.
- `auth-context.tsx`를 수정하지 마라. 이유: 인증 흐름 자체를 변경하는 것이 아니라 UI 편의 기능만 추가한다.
- 기존 로그인 페이지의 폼 제출 로직, 에러 표시 로직, 라우팅 로직을 변경하지 마라. 이유: 이메일 저장은 부가 기능이며 기존 인증 흐름에 영향을 주면 안 된다.
- `TODO`, `not implemented`, `stub`으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

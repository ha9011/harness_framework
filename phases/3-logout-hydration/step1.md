# Step 1: logout-buttons

## Step Contract

- Capability: 홈화면 우상단과 설정 페이지에 로그아웃 버튼 추가
- Layer: frontend-view
- Write Scope: `frontend/app/page.tsx`, `frontend/app/settings/page.tsx`
- Out of Scope: 백엔드 코드 수정, 새로운 API 엔드포인트, AuthContext 수정, layout.tsx 수정
- Critical Gates: `cd frontend && npm run build` 성공. `grep -q 'logout' frontend/app/page.tsx` (홈에 logout 호출 존재). `grep -q 'logout' frontend/app/settings/page.tsx` (설정에 logout 호출 존재)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/UI_GUIDE.md`
- `frontend/lib/auth-context.tsx` — logout 함수 시그니처 확인
- `frontend/app/page.tsx` — 홈화면 현재 구조 파악
- `frontend/app/settings/page.tsx` — 설정 페이지 현재 구조 파악

## 작업

### 1. 홈화면 우상단 로그아웃 버튼 (`frontend/app/page.tsx`)

인사 헤더 영역에 로그아웃 아이콘 버튼을 추가한다.

- 위치: 인사 헤더(`{user?.nickname}님 안녕하세요`) 영역의 우상단
- 현재 인사 헤더는 `<div>` 안에 부제 + 제목만 있다. 이것을 flex justify-between 레이아웃으로 변경하여 우측에 로그아웃 버튼을 배치한다
- 버튼 스타일: 아이콘만 표시. SVG 인라인 로그아웃 아이콘 (문 + 화살표, strokeWidth 1.5). 색상은 `text-ink-muted`, 호버 시 `text-ink`
- 클릭 시: `useAuth()`의 `logout()` 호출
- `useAuth`는 이미 import되어 있으므로 `logout`을 destructuring에 추가하면 된다

```tsx
// 시그니처 참고
const { user, logout } = useAuth();
```

### 2. 설정 페이지 로그아웃 버튼 (`frontend/app/settings/page.tsx`)

설정 카드 아래에 로그아웃 버튼을 추가한다.

- 위치: 기존 설정 카드(`bg-raised rounded-[20px]`) 아래에 별도 버튼
- 스타일: Ghost 버튼 — `w-full rounded-[14px] h-[42px] border border-hairline text-ink-muted text-sm font-semibold`
- 텍스트: "로그아웃"
- 클릭 시: `useAuth()`의 `logout()` 호출
- `useAuth`는 이미 import되어 있으므로 `logout`을 destructuring에 추가하면 된다

## Acceptance Criteria

```bash
grep -q 'logout' frontend/app/page.tsx && echo "PASS: home logout found" || echo "FAIL"
grep -q 'logout' frontend/app/settings/page.tsx && echo "PASS: settings logout found" || echo "FAIL"
cd frontend && npm run build
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 홈화면과 설정 페이지 모두에 logout 호출이 존재하는지 확인한다.
3. `npm run build`가 에러 없이 완료되는지 확인한다.
4. 결과에 따라 `phases/3-logout-hydration/index.json`의 step 1을 업데이트한다.

## 금지사항

- AuthContext(`frontend/lib/auth-context.tsx`)를 수정하지 마라. 이유: logout 함수는 이미 구현되어 있다.
- `layout.tsx`를 수정하지 마라. 이유: Step 0에서 이미 처리했다.
- 확인 모달/다이얼로그를 추가하지 마라. 이유: 요청 범위 밖이다. 버튼 클릭 시 즉시 로그아웃한다.
- 외부 아이콘 라이브러리를 설치하지 마라. 이유: UI_GUIDE.md에 따라 SVG 인라인을 사용한다.
- `TODO`, `not implemented`, `stub`, 빈 배열/빈 객체, 고정 더미 반환으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

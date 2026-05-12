# Step 0: hydration-fix

## Step Contract

- Capability: 브라우저 확장 프로그램이 주입하는 속성으로 인한 hydration mismatch 콘솔 경고 제거
- Layer: frontend-view
- Write Scope: `frontend/app/layout.tsx`
- Out of Scope: 로그아웃 버튼 추가, 백엔드 코드 수정, 다른 프론트엔드 페이지 수정
- Critical Gates: `cd frontend && npm run build` 성공 (컴파일 에러 없음). `grep -q 'suppressHydrationWarning' frontend/app/layout.tsx` (속성이 추가되었는지 확인)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `frontend/app/layout.tsx`

## 작업

`frontend/app/layout.tsx`의 `<body>` 태그에 `suppressHydrationWarning` 속성을 추가하라.

### 변경 대상

`frontend/app/layout.tsx`의 `<body>` 태그:

```tsx
// 변경 전
<body className="min-h-full flex flex-col font-sans">

// 변경 후
<body className="min-h-full flex flex-col font-sans" suppressHydrationWarning>
```

### 배경

브라우저 확장 프로그램(Bing 등)이 `<body>`에 `__processed_*`, `bis_register` 같은 속성을 런타임에 주입한다. 서버 렌더링된 HTML에는 이 속성이 없으므로 React가 hydration mismatch 경고를 발생시킨다. `suppressHydrationWarning`은 해당 요소의 속성 불일치 경고만 억제하며, 자식 요소의 hydration은 정상 동작한다.

## Acceptance Criteria

```bash
grep -q 'suppressHydrationWarning' frontend/app/layout.tsx && echo "PASS: attribute found" || echo "FAIL: attribute missing"
cd frontend && npm run build
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. `suppressHydrationWarning`이 `<body>` 태그에 존재하는지 확인한다.
3. `npm run build`가 에러 없이 완료되는지 확인한다.
4. 결과에 따라 `phases/3-logout-hydration/index.json`의 step 0을 업데이트한다.

## 금지사항

- `<html>` 태그에 `suppressHydrationWarning`을 추가하지 마라. 이유: body만 확장 프로그램 속성 주입 대상이다.
- layout.tsx의 다른 부분(import, AuthProvider, BottomNav 등)을 수정하지 마라. 이유: 이 step의 scope는 body 태그 속성 추가뿐이다.
- `TODO`, `not implemented`, `stub`, 빈 배열/빈 객체, 고정 더미 반환으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

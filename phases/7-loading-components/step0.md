# Step 0: test-infra-coffee-spinner

## Step Contract

- Capability: 프론트엔드 테스트 인프라 구축 + CSS 로딩 키프레임 추가 + CoffeeSpinner 컴포넌트 구현
- Layer: frontend-view
- Write Scope: `frontend/vitest.config.ts`, `frontend/package.json`, `frontend/app/globals.css`, `frontend/app/components/CoffeeSpinner.tsx`, `frontend/app/components/__tests__/CoffeeSpinner.test.tsx`
- Out of Scope: CremaLoader 컴포넌트, 기존 페이지 수정, 백엔드 코드
- Critical Gates: `cd frontend && npx vitest run app/components/__tests__/CoffeeSpinner.test.tsx` — CoffeeSpinner가 SVG 렌더링, size/className prop 동작을 테스트로 검증

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 전체 작업 계획, 테스트 인프라 세팅 요구사항
- `/docs/UI_GUIDE.md` — 색상 팔레트, 애니메이션 규칙, 로딩 컴포넌트 스펙
- `/frontend/app/globals.css` — 기존 CSS 유틸리티 클래스 패턴 확인
- `/frontend/package.json` — 현재 의존성 확인 (vitest, jsdom 이미 있음)
- `/frontend/tsconfig.json` — `@/*` path alias 확인

## 작업

### 1. 테스트 인프라 세팅

**`frontend/vitest.config.ts` 생성:**

```typescript
// vitest.config.ts 시그니처
import { defineConfig } from "vitest/config";
import path from "path";

export default defineConfig({
  test: {
    environment: "jsdom",
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./"),
    },
  },
});
```

**의존성 설치:**

```bash
cd frontend && npm install -D @testing-library/react @testing-library/jest-dom
```

- `@testing-library/react`는 **v16.3.0 이상** 필요 (React 19.2.4 호환)
- 설치 후 `package.json`의 devDependencies에 추가되었는지 확인

### 2. CSS 키프레임 추가

`frontend/app/globals.css`의 기존 유틸리티 클래스 아래(`.duration-400` 다음)에 추가:

```css
/* 로딩 애니메이션 */
@keyframes crema-swirl {
  0%   { opacity: 0; transform: rotate(0deg) scale(0.3); }
  30%  { opacity: 1; transform: rotate(120deg) scale(1); }
  70%  { opacity: 1; transform: rotate(240deg) scale(1); }
  100% { opacity: 0; transform: rotate(360deg) scale(0.3); }
}

@keyframes coffee-spin {
  0%   { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
```

### 3. CoffeeSpinner 테스트 작성 (RED)

`frontend/app/components/__tests__/CoffeeSpinner.test.tsx`:

테스트 케이스:
- SVG 요소가 렌더링되는지 (`querySelector("svg")` 존재)
- 기본 size가 20인지 (width/height 속성)
- 커스텀 size prop이 반영되는지 (예: `size={32}` → width="32" height="32")
- className prop이 SVG 래퍼에 전달되는지
- `aria-hidden="true"` 적용되어 있는지 (장식용 요소)

### 4. CoffeeSpinner 구현 (GREEN)

`frontend/app/components/CoffeeSpinner.tsx`:

```typescript
// 시그니처
interface CoffeeSpinnerProps {
  size?: number;    // 기본값: 20
  className?: string;
}

export default function CoffeeSpinner({ size, className }: CoffeeSpinnerProps): JSX.Element
```

구현 요구사항:
- 사이드뷰 커피잔 SVG: 머그잔 몸체 + 손잡이 + 김(steam) 라인
- 테마 색상 사용: `#A67C52` (primary), `#6F4E37` (mocha), `#9A8676` (ink-muted)
- `animation: coffee-spin 1s linear infinite` 스타일 인라인 적용
- `aria-hidden="true"` 적용 (장식용)
- `"use client"` 지시문 불필요 (순수 SVG, hook 미사용)

## Acceptance Criteria

```bash
cd frontend && npx vitest run app/components/__tests__/CoffeeSpinner.test.tsx
cd frontend && npm run build
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - CoffeeSpinner 테스트가 SVG 렌더링, size prop, className prop, aria-hidden을 검증했는가?
   - vitest가 jsdom 환경에서 정상 실행되는가?
3. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가? (frontend/app/components/)
   - ADR 기술 스택을 벗어나지 않았는가?
   - CLAUDE.md CRITICAL 규칙을 위반하지 않았는가?
   - Step Contract의 Write Scope 밖을 수정하지 않았는가?
4. 결과에 따라 `phases/7-loading-components/index.json`의 해당 step을 업데이트한다.

## 금지사항

- CremaLoader를 이 step에서 구현하지 마라. 이유: Step 1의 scope다.
- 기존 페이지의 로딩 텍스트를 수정하지 마라. 이유: Step 2의 scope다.
- globals.css의 기존 유틸리티 클래스(`.perspective-1000` 등)를 수정하지 마라. 이유: 기존 카드 플립 기능이 깨진다.
- `@testing-library/react` 설치 시 `--legacy-peer-deps` 플래그를 사용하지 마라. 이유: 의존성 충돌을 숨긴다. 충돌 시 호환 버전을 찾아라.
- `TODO`, `not implemented`, `stub` 등으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 기존 테스트의 기대값을 변경하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

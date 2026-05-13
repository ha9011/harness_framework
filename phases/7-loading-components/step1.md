# Step 1: crema-loader

## Step Contract

- Capability: CremaLoader 컴포넌트 구현 — 머그잔 탑뷰 SVG + 크레마 회전 애니메이션
- Layer: frontend-view
- Write Scope: `frontend/app/components/CremaLoader.tsx`, `frontend/app/components/__tests__/CremaLoader.test.tsx`
- Out of Scope: CoffeeSpinner 수정, 기존 페이지 수정, globals.css 수정, 백엔드 코드
- Critical Gates: `cd frontend && npx vitest run app/components/__tests__/CremaLoader.test.tsx` — CremaLoader가 기본/커스텀 메시지 표시, SVG 렌더링, role="status" 접근성을 테스트로 검증

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — CremaLoader 스펙 (색상, SVG 구조, 애니메이션)
- `/docs/UI_GUIDE.md` — 색상 팔레트, 로딩 컴포넌트 섹션 (CremaLoader 스펙)
- `/frontend/app/globals.css` — `@keyframes crema-swirl` 키프레임 확인 (Step 0에서 추가됨)
- `/frontend/app/components/CoffeeSpinner.tsx` — Step 0에서 생성된 컴포넌트. 코드 스타일 참조
- `/frontend/app/components/__tests__/CoffeeSpinner.test.tsx` — Step 0에서 생성된 테스트. 테스트 패턴 참조
- `/frontend/vitest.config.ts` — Step 0에서 생성된 vitest 설정 확인

## 작업

### 1. CremaLoader 테스트 작성 (RED)

`frontend/app/components/__tests__/CremaLoader.test.tsx`:

테스트 케이스:
- 기본 메시지 "불러오는 중..."이 표시되는지
- 커스텀 message prop이 반영되는지 (예: `message="예문을 만들고 있어요..."`)
- SVG 요소가 렌더링되는지 (`querySelector("svg")` 존재)
- `role="status"` 속성이 적용되어 있는지
- `aria-live="polite"` 속성이 적용되어 있는지

Step 0에서 생성된 CoffeeSpinner 테스트의 패턴(import, render 방식, assertion 스타일)을 동일하게 따르라.

### 2. CremaLoader 구현 (GREEN)

`frontend/app/components/CremaLoader.tsx`:

```typescript
// 시그니처
interface CremaLoaderProps {
  message?: string;  // 기본값: "불러오는 중..."
}

export default function CremaLoader({ message }: CremaLoaderProps): JSX.Element
```

구현 요구사항:

**SVG 구조 (탑뷰 머그잔, viewBox="0 0 120 120"):**

동심원 구조로 위에서 내려다보는 커피 머그잔:
1. **소서(받침 접시)**: 가장 큰 원. 색상: `#E8DCC8` (kraft)
2. **머그잔 외벽**: 중간 원. 색상: `#A67C52` (primary)
3. **머그잔 내벽(rim)**: 약간 작은 원. 색상: `#8C6440` (primary-deep). 머그잔 두께 표현
4. **커피 액면**: 내부 원. 색상: `#523926` (mocha-deep)
5. **크레마 나선**: 커피 위의 나선형 path. 색상: `#E8D5BE` (primary-soft). `animation: crema-swirl 2.5s ease-in-out infinite` 적용
6. **손잡이**: 우측에 작은 반원형 path. 색상: `#A67C52` (primary)

**레이아웃:**
- 컨테이너: `flex flex-col items-center justify-center gap-3`
- SVG 크기: width/height 80px 정도 (모달 내에서도 적절한 크기)
- 메시지 텍스트: `text-sm text-ink-muted`
- 접근성: 최외곽 div에 `role="status"` + `aria-live="polite"`

**크레마 애니메이션:**
- globals.css에 이미 정의된 `crema-swirl` 키프레임 사용
- SVG의 크레마 path 요소에 인라인 style로 `animation: crema-swirl 2.5s ease-in-out infinite` 적용
- `transform-origin: center` 필수 (SVG 요소의 회전 중심)

## Acceptance Criteria

```bash
cd frontend && npx vitest run app/components/__tests__/CremaLoader.test.tsx
cd frontend && npx vitest run   # 전체 테스트 회귀 없음
cd frontend && npm run build
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - 기본 메시지와 커스텀 메시지가 모두 테스트되었는가?
   - SVG 렌더링과 접근성 속성(role, aria-live)이 검증되었는가?
3. 아키텍처 체크리스트를 확인한다:
   - frontend/app/components/ 디렉토리에 생성되었는가?
   - Step Contract의 Write Scope 밖을 수정하지 않았는가?
   - CoffeeSpinner나 globals.css를 수정하지 않았는가?
4. 결과에 따라 `phases/7-loading-components/index.json`의 해당 step을 업데이트한다.

## 금지사항

- CoffeeSpinner.tsx를 수정하지 마라. 이유: Step 0에서 완성된 컴포넌트이며 이 step의 scope가 아니다.
- globals.css를 수정하지 마라. 이유: Step 0에서 키프레임이 이미 추가되었다. 수정이 필요하면 error로 보고하라.
- 기존 페이지의 로딩 텍스트를 수정하지 마라. 이유: Step 2, 3의 scope다.
- 외부 애니메이션 라이브러리(framer-motion 등)를 설치하지 마라. 이유: SVG + CSS 애니메이션으로만 구현한다.
- `TODO`, `not implemented`, `stub` 등으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 기존 테스트의 기대값을 변경하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

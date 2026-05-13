# PLAN: 카페 테마 로딩 컴포넌트

## 작업 목표
API 대기 시 카페 테마에 맞는 로딩 애니메이션 제공 (CremaLoader + CoffeeSpinner)

## 구현할 기능
1. **CoffeeSpinner** — 사이드뷰 커피잔 SVG 회전 미니 스피너 (일반 페이지 로딩용)
2. **CremaLoader** — 머그잔 탑뷰 SVG + 크레마 회전 애니메이션 (AI 호출용)
3. **CSS keyframes** — `crema-swirl`, `coffee-spin` 키프레임을 globals.css에 추가
4. **전체 페이지 적용** — CoffeeSpinner 10곳 + CremaLoader 4곳 교체

## 기술적 제약사항
- 외부 라이브러리 없이 SVG + CSS 애니메이션으로 구현
- 기존 테마 색상(kraft, primary, mocha-deep, primary-soft) 재사용
- Tailwind CSS v4 (`@theme inline` 블록)
- 접근성: CremaLoader에 `role="status"`, `aria-live="polite"`

## 테스트 전략
- 기존 테스트 영향: 없음 (신규 컴포넌트만 추가, 기존 로직 변경 없음)
- 테스트 인프라 세팅 필요:
  - `vitest.config.ts` 생성 (현재 없음) — `@/*` path alias 설정 필수 (`tsconfig.json`의 `"@/*": ["./*"]` 미러링)
  - `@testing-library/react` v16.3.0+ 설치 (React 19.2.4 호환 필요)
  - `@testing-library/jest-dom` 설치
- 신규 테스트:
  - `CoffeeSpinner.test.tsx` — SVG 렌더링, size prop, className 전달
  - `CremaLoader.test.tsx` — 기본/커스텀 메시지, SVG 존재, role="status"
- 테스트 프레임워크: vitest + jsdom + @testing-library/react

## Phase/Step 초안

### Step 0: 테스트 인프라 + CSS keyframes + CoffeeSpinner
- 작업:
  - `vitest.config.ts` 생성 (environment: jsdom, resolve.alias: `@/` → `./`)
  - `@testing-library/react` (v16.3.0+) + `@testing-library/jest-dom` 설치
  - `globals.css`에 `@keyframes crema-swirl`, `@keyframes coffee-spin` 추가
  - `CoffeeSpinner.test.tsx` 작성 (RED)
  - `CoffeeSpinner.tsx` 구현 (GREEN)
- 산출물: 테스트 인프라 + CoffeeSpinner 컴포넌트 + 테스트 통과

### Step 1: CremaLoader
- 작업:
  - `CremaLoader.test.tsx` 작성 (RED)
  - `CremaLoader.tsx` 구현 — 탑뷰 머그잔 SVG + 크레마 애니메이션 (GREEN)
- 산출물: CremaLoader 컴포넌트 + 테스트 통과

### Step 2: CoffeeSpinner 적용 (10곳)
- 작업: 각 페이지의 로딩 텍스트를 `<CoffeeSpinner /> + 텍스트` 조합으로 교체
- **각 파일의 기존 마크업 구조·padding·글자 크기를 정확히 유지**:

| 파일 | 래퍼 | padding | 글자 | 교체 후 구조 |
|------|------|---------|------|-------------|
| `AuthGuard.tsx` | `<div flex center>` | py-20 | text-sm | div 유지, p → CoffeeSpinner+span |
| `page.tsx` (대시보드) | `<div flex center>` | py-20 | text-sm | div 유지, p → CoffeeSpinner+span |
| `settings/page.tsx` | `<div flex center>` | py-20 | text-sm | div 유지, p → CoffeeSpinner+span |
| `words/page.tsx` | 없음 (p만) | py-8 | text-sm | div로 감싸기, CoffeeSpinner+span |
| `words/[id]/page.tsx` | 없음 (p만) | py-8 | text-sm | div로 감싸기, CoffeeSpinner+span |
| `patterns/page.tsx` | 없음 (p만) | py-8 | text-sm | div로 감싸기, CoffeeSpinner+span |
| `patterns/[id]/page.tsx` | 없음 (p만) | py-8 | text-sm | div로 감싸기, CoffeeSpinner+span |
| `history/page.tsx` | 없음 (p만) | py-8 | text-sm | div로 감싸기, CoffeeSpinner+span |
| `review/page.tsx` | 없음 (div) | py-12 | text-sm | 기존 div 유지, CoffeeSpinner+span |
| `generate/page.tsx` (이력) | 없음 (p만) | py-4 | **text-xs** | div로 감싸기, CoffeeSpinner+span (text-xs 유지) |

- 산출물: 모든 일반 로딩 화면에 커피잔 스피너 표시

### Step 3: CremaLoader 적용 (4곳)
- 작업:
  - `generate/page.tsx` — loading 시 폼(난이도/개수) 숨기고 CremaLoader만 표시 (message: "예문을 만들고 있어요...")
  - `WordAddModal.tsx` — loading 시 폼을 숨기고 CremaLoader 표시 (message: "단어를 등록하고 있어요...")
  - `PatternAddModal.tsx` — loading 시 폼을 숨기고 CremaLoader 표시
    - `loadingType` state 추가: `useState<"submit" | "extract" | null>(null)`
    - 수동 등록(`handleSubmit`) 시: loadingType="submit" → "패턴을 등록하고 있어요..."
    - 이미지 추출(`handleImageExtract`) 시: loadingType="extract" → "이미지를 분석하고 있어요..."
    - finally 블록에서 `setLoadingType(null)`
- CremaLoader 배치 방식: **폼을 숨기고 CremaLoader만 표시** (모달 높이 제한 + 시선 분산 방지)
- 산출물: AI 호출 화면에 크레마 로딩 애니메이션 표시

## 검증 항목
- `cd frontend && npx vitest run` — 컴포넌트 테스트 통과
- `cd frontend && npm run build` — 빌드 성공 (TypeScript 오류 없음)
- `cd frontend && npm run dev` — 브라우저에서 시각 확인:
  - 대시보드 진입 시 CoffeeSpinner 표시 (py-20 영역)
  - 단어 목록 진입 시 CoffeeSpinner 표시 (py-8 영역)
  - 예문 생성 시 CremaLoader 표시 (폼 숨김)
  - 단어 등록 모달에서 CremaLoader 표시 (폼 숨김)

## 미결 사항
- 없음

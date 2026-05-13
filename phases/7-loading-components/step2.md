# Step 2: spinner-apply

## Step Contract

- Capability: 전체 페이지의 "불러오는 중..." 텍스트를 CoffeeSpinner + 텍스트 조합으로 교체
- Layer: frontend-view
- Write Scope: `frontend/app/components/AuthGuard.tsx`, `frontend/app/page.tsx`, `frontend/app/words/page.tsx`, `frontend/app/words/[id]/page.tsx`, `frontend/app/patterns/page.tsx`, `frontend/app/patterns/[id]/page.tsx`, `frontend/app/generate/page.tsx` (이력 탭 로딩만), `frontend/app/review/page.tsx`, `frontend/app/history/page.tsx`, `frontend/app/settings/page.tsx`
- Out of Scope: CoffeeSpinner/CremaLoader 컴포넌트 수정, globals.css 수정, WordAddModal/PatternAddModal 수정, 백엔드 코드, generate/page.tsx의 생성 버튼 로딩 (Step 3 scope)
- Critical Gates: `cd frontend && npm run build` 빌드 성공 + `cd frontend && grep -r "CoffeeSpinner" app/ --include="*.tsx" | wc -l` 결과 11 이상 (10개 파일 import + 컴포넌트 정의 1)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Step 2 적용 위치 테이블 (래퍼, padding, 글자 크기 정보)
- `/docs/UI_GUIDE.md` — CoffeeSpinner 사용 패턴
- `/frontend/app/components/CoffeeSpinner.tsx` — Step 0에서 생성된 컴포넌트. import 경로와 props 확인

**수정 대상 파일 전체를 먼저 읽어라** (각 파일의 기존 로딩 마크업 구조를 정확히 파악하기 위해):
- `/frontend/app/components/AuthGuard.tsx`
- `/frontend/app/page.tsx`
- `/frontend/app/words/page.tsx`
- `/frontend/app/words/[id]/page.tsx`
- `/frontend/app/patterns/page.tsx`
- `/frontend/app/patterns/[id]/page.tsx`
- `/frontend/app/generate/page.tsx`
- `/frontend/app/review/page.tsx`
- `/frontend/app/history/page.tsx`
- `/frontend/app/settings/page.tsx`

## 작업

각 페이지의 로딩 텍스트를 CoffeeSpinner + 텍스트 조합으로 교체한다. **기존 마크업 구조(래퍼 유무, padding, 글자 크기)를 정확히 유지해야 한다.**

### 교체 규칙

#### 그룹 A: 래퍼 div가 있는 파일 (py-20)

**AuthGuard.tsx, page.tsx (대시보드), settings/page.tsx**

기존:
```tsx
<div className="flex items-center justify-center py-20">
  <p className="text-sm text-ink-muted">불러오는 중...</p>
</div>
```

변경:
```tsx
<div className="flex items-center justify-center gap-2 py-20">
  <CoffeeSpinner />
  <span className="text-sm text-ink-muted">불러오는 중...</span>
</div>
```

변경 포인트: div에 `gap-2` 추가, `<p>` → `<CoffeeSpinner /> + <span>`

#### 그룹 B: 래퍼 없이 p만 있는 파일 (py-8)

**words/page.tsx, words/[id]/page.tsx, patterns/page.tsx, patterns/[id]/page.tsx, history/page.tsx**

기존:
```tsx
<p className="text-sm text-ink-muted text-center py-8">불러오는 중...</p>
```

변경:
```tsx
<div className="flex items-center justify-center gap-2 py-8">
  <CoffeeSpinner />
  <span className="text-sm text-ink-muted">불러오는 중...</span>
</div>
```

변경 포인트: `<p>` → `<div>` 래퍼 + `<CoffeeSpinner /> + <span>`. **py-8 유지**.

#### 그룹 C: review/page.tsx (py-12)

기존:
```tsx
<div className="text-center py-12 text-ink-muted text-sm">
  카드를 불러오는 중...
</div>
```

변경:
```tsx
<div className="flex items-center justify-center gap-2 py-12 text-ink-muted text-sm">
  <CoffeeSpinner />
  <span>카드를 불러오는 중...</span>
</div>
```

변경 포인트: `text-center` → `flex items-center justify-center gap-2`. **py-12 유지**. 텍스트 "카드를 불러오는 중..." **유지**.

#### 그룹 D: generate/page.tsx 이력 탭 (py-4, text-xs)

기존:
```tsx
<p className="text-xs text-ink-muted text-center py-4">
  로딩 중...
</p>
```

변경:
```tsx
<div className="flex items-center justify-center gap-2 py-4">
  <CoffeeSpinner size={16} />
  <span className="text-xs text-ink-muted">로딩 중...</span>
</div>
```

변경 포인트: **text-xs 유지**, **py-4 유지**, CoffeeSpinner size를 16으로 축소 (text-xs에 맞게).

### import 추가

각 파일 상단에 CoffeeSpinner import를 추가한다. 상대 경로는 파일 위치에 따라 다르다:

- `app/components/AuthGuard.tsx` → `import CoffeeSpinner from "./CoffeeSpinner";`
- `app/page.tsx` → `import CoffeeSpinner from "./components/CoffeeSpinner";`
- `app/words/page.tsx` → `import CoffeeSpinner from "../components/CoffeeSpinner";`
- `app/words/[id]/page.tsx` → `import CoffeeSpinner from "../../components/CoffeeSpinner";`
- `app/patterns/page.tsx` → `import CoffeeSpinner from "../components/CoffeeSpinner";`
- `app/patterns/[id]/page.tsx` → `import CoffeeSpinner from "../../components/CoffeeSpinner";`
- `app/generate/page.tsx` → `import CoffeeSpinner from "../components/CoffeeSpinner";`
- `app/review/page.tsx` → `import CoffeeSpinner from "../components/CoffeeSpinner";`
- `app/history/page.tsx` → `import CoffeeSpinner from "../components/CoffeeSpinner";`
- `app/settings/page.tsx` → `import CoffeeSpinner from "../components/CoffeeSpinner";`

### 주의: generate/page.tsx에서 수정 범위 제한

generate/page.tsx에서는 **이력 탭의 `historyLoading` 로딩 텍스트만** 교체한다 (약 224-228줄). 생성 버튼의 `{loading ? "생성 중..." : "예문 생성"}` 텍스트는 **수정하지 않는다** — Step 3에서 CremaLoader로 교체할 부분이다.

## Acceptance Criteria

```bash
cd frontend && npm run build
cd frontend && grep -r "CoffeeSpinner" app/ --include="*.tsx" | wc -l  # 11 이상
cd frontend && npx vitest run  # 전체 테스트 회귀 없음
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - 10개 파일 모두에서 CoffeeSpinner가 import 및 사용되었는가?
   - 빌드가 TypeScript 오류 없이 통과하는가?
3. 아키텍처 체크리스트를 확인한다:
   - Write Scope 밖 파일을 수정하지 않았는가? (특히 WordAddModal, PatternAddModal)
   - 기존 로딩 로직(useState, setLoading 등)을 변경하지 않았는가?
   - generate/page.tsx의 생성 버튼 로딩을 수정하지 않았는가?
4. 결과에 따라 `phases/7-loading-components/index.json`의 해당 step을 업데이트한다.

## 금지사항

- CoffeeSpinner.tsx 또는 CremaLoader.tsx를 수정하지 마라. 이유: Step 0, 1에서 완성된 컴포넌트다.
- globals.css를 수정하지 마라. 이유: Step 0에서 키프레임이 이미 추가되었다.
- WordAddModal.tsx, PatternAddModal.tsx를 수정하지 마라. 이유: Step 3의 scope다.
- generate/page.tsx의 생성 버튼 로딩 (`{loading ? "생성 중..." : "예문 생성"}`)을 수정하지 마라. 이유: Step 3에서 CremaLoader로 교체할 부분이다.
- 각 페이지의 **비즈니스 로직**(API 호출, 상태 관리, 이벤트 핸들러)을 수정하지 마라. 이유: UI 표시 레이어만 변경하는 step이다.
- 기존 padding(py-20, py-8, py-12, py-4)을 변경하지 마라. 이유: 레이아웃이 깨진다.
- `TODO`, `not implemented`, `stub` 등으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 기존 테스트의 기대값을 변경하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

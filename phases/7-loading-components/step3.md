# Step 3: crema-apply

## Step Contract

- Capability: AI 호출 화면(예문 생성, 단어 등록, 패턴 등록)에 CremaLoader 적용
- Layer: frontend-view
- Write Scope: `frontend/app/generate/page.tsx` (생성 버튼 로딩 영역), `frontend/app/words/WordAddModal.tsx`, `frontend/app/patterns/PatternAddModal.tsx`
- Out of Scope: CoffeeSpinner/CremaLoader 컴포넌트 수정, globals.css 수정, 백엔드 코드, Step 2에서 적용한 CoffeeSpinner 교체 부분
- Critical Gates: `cd frontend && npm run build` 빌드 성공 + `cd frontend && grep -r "CremaLoader" app/ --include="*.tsx" | wc -l` 결과 4 이상 (3개 파일 import + 컴포넌트 정의 1)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Step 3 적용 위치, CremaLoader 배치 방식 (폼 숨기고 표시), PatternAddModal loadingType 요구사항
- `/docs/UI_GUIDE.md` — CremaLoader 스펙
- `/frontend/app/components/CremaLoader.tsx` — Step 1에서 생성된 컴포넌트. import 경로와 props 확인

**수정 대상 파일 전체를 먼저 읽어라:**
- `/frontend/app/generate/page.tsx` — 생성 버튼과 loading 상태 확인. Step 2에서 이력 탭 로딩이 CoffeeSpinner로 교체되었음을 확인
- `/frontend/app/words/WordAddModal.tsx` — 단건/벌크 등록 로딩 상태 확인
- `/frontend/app/patterns/PatternAddModal.tsx` — 수동 등록 + 이미지 추출 두 가지 로딩 시나리오 확인

## 작업

### 1. generate/page.tsx — 예문 생성 로딩

**현재 동작**: `loading` 상태가 true이면 버튼에 "생성 중..." 텍스트 표시 + disabled.

**변경**: `loading` 상태가 true이면 **폼 영역(난이도 선택 + 개수 선택 + 버튼)을 숨기고** CremaLoader만 표시.

```tsx
// mode === "generate" 블록 내부에서
{loading ? (
  <CremaLoader message="예문을 만들고 있어요..." />
) : (
  <>
    {/* 난이도 선택 */}
    {/* 개수 선택 */}
    {/* 생성 버튼 */}
  </>
)}
```

- 에러 표시 (`{error && ...}`)와 생성 결과 (`{sentences.length > 0 && ...}`)는 loading과 무관하게 유지
- import 추가: `import CremaLoader from "../components/CremaLoader";`

### 2. WordAddModal.tsx — 단어 등록 로딩

**현재 동작**: `loading` 상태가 true이면 버튼에 "등록 중..." 텍스트 표시 + disabled. 폼은 그대로 보임.

**변경**: `loading` 상태가 true이면 **폼 영역(탭 + 입력 필드 + 버튼)을 숨기고** CremaLoader만 표시.

```tsx
// 모달 내부 (헤더 아래)
{loading ? (
  <div className="py-8">
    <CremaLoader message="단어를 등록하고 있어요..." />
  </div>
) : (
  <>
    {/* 탭 (단건/벌크) */}
    {/* 폼 (mode에 따라 single/bulk) */}
  </>
)}
```

- 모달 헤더(제목 + 닫기 버튼)는 loading 중에도 **유지** (사용자가 닫기 가능)
- 에러/결과 메시지는 loading과 무관하게 유지
- import 추가: `import CremaLoader from "../components/CremaLoader";`

### 3. PatternAddModal.tsx — 패턴 등록/이미지 추출 로딩

**현재 동작**: `loading` 상태가 true이면 버튼에 "처리 중..." 텍스트 표시. `handleSubmit`과 `handleImageExtract` 모두 같은 `loading` state 사용.

**변경**:

#### 3-1. loadingType state 추가

```typescript
const [loadingType, setLoadingType] = useState<"submit" | "extract" | null>(null);
```

#### 3-2. handleSubmit 수정

```typescript
const handleSubmit = async () => {
  // 기존 validation 유지
  setLoading(true);
  setLoadingType("submit");  // 추가
  // ... 기존 로직 유지 ...
  // finally 블록에서:
  setLoading(false);
  setLoadingType(null);  // 추가
};
```

#### 3-3. handleImageExtract 수정

```typescript
const handleImageExtract = async (file: File) => {
  setLoading(true);
  setLoadingType("extract");  // 추가
  // ... 기존 로직 유지 ...
  // finally 블록에서:
  setLoading(false);
  setLoadingType(null);  // 추가
};
```

#### 3-4. UI 교체

```tsx
// 모달 내부 (헤더 아래)
{loading ? (
  <div className="py-8">
    <CremaLoader message={
      loadingType === "extract"
        ? "이미지를 분석하고 있어요..."
        : "패턴을 등록하고 있어요..."
    } />
  </div>
) : (
  <>
    {/* 모드 전환 탭 */}
    {/* 이미지 업로드 영역 (mode === "image") */}
    {/* 직접 입력 폼 */}
    {/* 등록 버튼 */}
  </>
)}
```

- 모달 헤더(제목 + 닫기 버튼)는 loading 중에도 **유지**
- 에러 메시지는 loading과 무관하게 유지
- import 추가: `import CremaLoader from "../components/CremaLoader";`

### 핵심 규칙

- **기존 비즈니스 로직을 절대 변경하지 마라.** setLoading, API 호출, 에러 처리, onSuccess 콜백 등은 그대로 유지. loadingType state만 추가.
- **모달 헤더(제목 + 닫기)는 항상 보이게 유지.** 사용자가 로딩 중에도 모달을 닫을 수 있어야 한다.
- **폼을 숨기되, 에러/결과 메시지는 유지.** loading이 끝난 후 에러나 성공 메시지가 표시되어야 한다.

## Acceptance Criteria

```bash
cd frontend && npm run build
cd frontend && grep -r "CremaLoader" app/ --include="*.tsx" | wc -l  # 4 이상
cd frontend && npx vitest run  # 전체 테스트 회귀 없음
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - 3개 파일 모두에서 CremaLoader가 import 및 사용되었는가?
   - 빌드가 TypeScript 오류 없이 통과하는가?
3. 아키텍처 체크리스트를 확인한다:
   - Write Scope 밖 파일을 수정하지 않았는가?
   - 기존 비즈니스 로직(API 호출, 상태 관리)을 변경하지 않았는가?
   - PatternAddModal에 loadingType state가 추가되었는가?
   - 모달 헤더가 loading 중에도 보이는가?
4. 결과에 따라 `phases/7-loading-components/index.json`의 해당 step을 업데이트한다.

## 금지사항

- CoffeeSpinner.tsx, CremaLoader.tsx를 수정하지 마라. 이유: Step 0, 1에서 완성된 컴포넌트다.
- globals.css를 수정하지 마라. 이유: Step 0에서 키프레임이 이미 추가되었다.
- Step 2에서 교체한 CoffeeSpinner 부분을 수정하지 마라. 이유: 이미 완료된 scope다.
- generate/page.tsx의 이력 탭 로딩(Step 2에서 CoffeeSpinner로 교체된 부분)을 수정하지 마라. 이유: Step 2의 산출물이다.
- 기존 API 호출 로직(api.post, api.upload 등)을 수정하지 마라. 이유: UI 레이어만 변경하는 step이다.
- loading state의 set/clear 타이밍을 변경하지 마라. 이유: 기존 동작을 깨뜨린다. loadingType만 추가로 세팅하라.
- `TODO`, `not implemented`, `stub` 등으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 기존 테스트의 기대값을 변경하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

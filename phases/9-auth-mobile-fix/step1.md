# Step 1: mobile-input-zoom

## Step Contract

- Capability: iOS Safari 입력 포커스 자동 줌 방지 — 모바일(터치) 환경에서 form control(input/textarea/select) 폰트를 16px 이상으로 강제
- Layer: frontend-view
- Write Scope: `frontend/app/globals.css`
- Out of Scope: backend 코드 일체, 개별 페이지/컴포넌트의 input className 수정, `app/layout.tsx`의 viewport export 추가, `user-scalable=no`/`maximum-scale=1` 사용
- Critical Gates: `cd frontend && npm run build` 성공 + `cd frontend && npm run lint` 통과 + `grep -i "coarse" frontend/app/globals.css` 와 `grep -i "16px" frontend/app/globals.css`로 모바일 16px 규칙 존재 확인 + `cd frontend && npm test`(Vitest) 회귀 없음

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Phase 9-auth-mobile-fix 전체 계획 (Step 1 = 프론트 모바일 입력 줌 방지)
- `/docs/ADR.md` — **ADR-019** (iOS 입력 줌 방지, **Tailwind 특이성 주의**: element selector가 유틸 클래스 `text-sm`을 못 이김 → `!important` 필요)
- `/docs/UI_GUIDE.md` — "모바일 입력 줌 방지 (iOS)" 지침
- `/frontend/app/globals.css` — 현재 Tailwind v4(`@import "tailwindcss"`) + `@theme inline` 토큰 + `body` + 카드플립/로딩 애니메이션. **이 파일 끝에 규칙을 추가**한다
- `/frontend/app/login/page.tsx` — 입력 필드가 `className="... text-sm ..."`(14px)를 사용해 iOS가 포커스 시 자동 줌인하는 원인 확인

이전 작업에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### `globals.css` 파일 끝에 모바일 form control 16px 규칙 추가

iOS Safari는 `font-size`가 16px 미만인 입력 필드에 포커스하면 자동으로 줌인하고, 그 줌이 페이지 이동(홈) 후에도 유지된다. 입력 필드는 login/signup/words·page/WordAddModal/PatternAddModal 등 여러 파일에 분산되어 있으므로, **개별 className을 고치지 않고 globals.css 전역 규칙 1곳**으로 일괄 해결한다.

`frontend/app/globals.css` 맨 끝에 추가:

```css
/* iOS Safari 입력 포커스 자동 줌 방지: 모바일(터치) 환경에서 form control 16px 보장 (ADR-019) */
@media (pointer: coarse) {
  input,
  textarea,
  select {
    font-size: 16px !important;
  }
}
```

핵심 규칙:
- **`!important`는 필수**다. 이유: 입력 필드는 Tailwind 유틸 클래스 `text-sm`(특이성 0,1,0)을 쓰는데, element selector `input`(특이성 0,0,1)은 이를 이기지 못한다. `!important` 없이는 규칙이 적용되지 않는다 (ADR-019)
- **`@media (pointer: coarse)`로 한정**한다. 이유: 터치 기기(모바일)에만 16px를 적용하고 데스크톱(마우스, fine pointer)은 기존 14px 디자인을 보존하기 위함
- 체크박스(`input[type=checkbox]`, 예: 로그인 "이메일 저장")는 `font-size`가 박스 크기에 영향을 주지 않으므로(`w-4 h-4`로 결정됨) 이 규칙에 포함돼도 무해하다 — 별도 예외 처리 불필요
- 기존 `@theme inline` 토큰, `body`, `.perspective-1000`/`.rotate-y-180` 등 카드플립·로딩 애니메이션 규칙은 **절대 변경하지 마라**. 새 규칙만 append

## Acceptance Criteria

```bash
cd frontend && npm run build
cd frontend && npm run lint
cd frontend && npm test
grep -i "coarse" app/globals.css
grep -i "16px" app/globals.css
```

- 빌드 성공 (컴파일/타입 에러 없음)
- lint 통과
- Vitest 기존 테스트 회귀 없음
- globals.css에 `@media (pointer: coarse)` 와 `font-size: 16px !important` 규칙이 존재

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - 단순 `npm run build`만으로 completed 처리하지 않았는가? → globals.css에 16px 규칙이 실제로 추가되었고 `!important`/`pointer: coarse`를 포함하는가(grep 확인)?
   - `!important`를 빠뜨려 Tailwind `text-sm`에 묻히는 무효 규칙을 만들지 않았는가?
3. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가?
   - ADR-019(16px + !important + user-scalable 유지) 결정을 위반하지 않았는가?
   - CLAUDE.md CRITICAL 규칙(frontend/ 폴더 외부 작성 금지)을 위반하지 않았는가?
   - Step Contract의 Write Scope 밖(backend/, layout.tsx 등)을 수정하지 않았는가?
   - backend 코드를 동시에 수정하지 않았는가? (이 step은 frontend만)
4. 결과에 따라 `phases/9-auth-mobile-fix/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- backend 코드(`backend/`)를 수정하지 마라. 이유: 이 step은 frontend 한정 (룰6 위반).
- `!important`를 생략하지 마라. 이유: Tailwind 유틸 `text-sm`(0,1,0)이 element selector(0,0,1)를 이겨서 규칙이 무효가 된다. 그러면 줌 버그가 그대로 남는데 빌드는 통과하므로 "고쳤다"고 오판하게 된다 (ADR-019).
- `app/layout.tsx`에 viewport export를 추가하지 마라. 이유: Next.js 기본 viewport(`width=device-width, initial-scale=1`)와 동일해 줌 해결과 무관한 불필요 변경이다. 이번 스코프는 globals.css 한정.
- viewport에 `user-scalable=no` / `maximum-scale=1`을 넣어 줌 자체를 막지 마라. 이유: 핀치 줌 접근성을 위배하고 iOS 일부 버전이 무시한다 (ADR-019). 해결책은 16px 폰트다.
- login/signup/words 등 개별 페이지의 input `className`(text-sm)을 일일이 바꾸지 마라. 이유: 5개 파일에 분산되어 있어 유지보수가 어렵다. globals.css 전역 규칙 1곳으로 처리.
- 기존 globals.css의 `@theme`/색상 토큰/애니메이션 규칙을 변경하지 마라. 이유: 테마·카드플립·로딩이 깨진다.
- `TODO`, `not implemented`, `stub`으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

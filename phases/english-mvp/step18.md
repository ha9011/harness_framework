# Step 18: frontend-scaffold

## Step Contract

- Capability: Next.js frontend scaffold with design tokens and test foundation
- Layer: frontend-view
- Write Scope: `frontend/`
- Out of Scope: backend files, auth flow pages beyond placeholder routes, learning screens, API implementation changes
- Critical Gates: `cd frontend && npm run test -- --run` and `cd frontend && npm run build`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/docs/UI_GUIDE.md`
- `/design/tokens.jsx`
- `/design/shared.jsx`
- `/design/app.jsx`
- `/package.json`

## 작업

`frontend/`에 Next.js App Router 프론트엔드 기반을 만든다.

필수 구현:
- TypeScript strict mode, Tailwind CSS, ESLint, Vitest, Testing Library, MSW를 설정한다.
- shadcn/ui를 사용할 수 있는 기본 구조를 만든다.
- lucide-react, TanStack Query, React Hook Form, Zod를 설치/설정한다.
- `app/layout.tsx`에 QueryProvider와 기본 shell provider를 연결한다.
- `app/lib/api.ts`는 base URL과 `credentials: "include"`를 기본으로 둔다.
- `app/lib/design-tokens.ts` 또는 동등 파일에 Cozy Cafe 색상 토큰을 정의한다.
- 하단 내비게이션과 기본 surface/button/chip 컴포넌트를 준비한다.
- 빈 홈 placeholder는 실제 학습 기능 없이 shell만 보여준다.

## Acceptance Criteria

```bash
cd frontend && npm run test -- --run
cd frontend && npm run lint
cd frontend && npm run build
```

## 검증 절차

1. Next.js app이 build 가능한지 확인한다.
2. 기본 provider와 API client 테스트가 통과하는지 확인한다.
3. Cozy Cafe token이 UI 컴포넌트에서 import 가능한지 확인한다.
4. Step 18을 `completed`로 표시하고 summary에 frontend scaffold와 test setup을 적는다.

## 금지사항

- `backend/`를 수정하지 마라. 이유: 이 step은 프론트 scaffold만 담당한다.
- 단어/패턴/복습 화면을 완성하지 마라. 이유: 후속 frontend-view step에서 구현한다.
- 루트 package script를 수정하지 마라. 이유: 통합 조정은 Step 27에서 다룬다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

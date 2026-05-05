# Step 7: 프론트엔드 — 인증 UI + FlipCard 모던 스타일

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 프론트엔드 엣지케이스 체크리스트
- `/docs/ARCHITECTURE.md` — 프론트엔드 패턴, 상태 관리
- `/docs/UI_GUIDE.md` — 로그인/회원가입 페이지 디자인, 입력 필드 스타일, 컴포넌트 규칙
- `/design/screens-auth.jsx` — 로그인/회원가입 목업 (레이아웃, 카피, 아이콘 참고)
- `/design/screens-review.jsx` — 53~56줄 모던 카드 스타일
- `/frontend/lib/api.ts` — 현재 API 클라이언트
- `/frontend/lib/types.ts` — 현재 타입 정의
- `/frontend/app/layout.tsx` — 현재 레이아웃
- `/frontend/app/page.tsx` — 현재 홈 페이지
- `/frontend/app/components/BottomNav.tsx` — 네비게이션
- `/frontend/app/components/FlipCard.tsx` — 현재 카드 컴포넌트
- `/frontend/AGENTS.md` — Next.js 16 주의사항. 반드시 읽고 `node_modules/next/dist/docs/` 확인

## 작업

### 1. api.ts 수정

`frontend/lib/api.ts`:

**request 함수:**
```typescript
const res = await fetch(`${BASE_URL}${path}`, {
  credentials: "include",  // 추가 — HttpOnly Cookie 전송
  headers: { "Content-Type": "application/json" },
  ...options,
});
```

**uploadRequest 함수에도 동일하게 credentials: "include" 추가**

**401 전역 핸들러:**
```typescript
if (res.status === 401) {
  // /api/auth/me 호출은 제외 (초기 인증 확인 시 401은 정상)
  if (!path.includes('/auth/me')) {
    window.dispatchEvent(new Event('unauthorized'));
  }
  throw new ApiError('UNAUTHORIZED', '인증이 필요합니다', 401);
}
```

### 2. 타입 추가

`frontend/lib/types.ts`에 추가:

```typescript
export interface AuthUser {
  id: number;
  email: string;
  nickname: string;
}
```

### 3. auth-context.tsx

`frontend/lib/auth-context.tsx` 생성:

```typescript
// AuthContext: { user: AuthUser | null, loading: boolean, login, signup, logout }
// AuthProvider:
//   - 마운트 시 GET /api/auth/me로 인증 상태 확인
//   - 성공 → user 설정, 실패(401) → user = null
//   - loading: 초기 인증 확인 중 true → 완료 후 false
//   - login(email, password): POST /api/auth/login → user 설정
//   - signup(email, password, nickname): POST /api/auth/signup → user 설정
//   - logout(): POST /api/auth/logout → user = null → router.push('/login')
//   - 'unauthorized' 이벤트 리스너: user = null → router.push('/login')
// useAuth(): AuthContext 반환
```

### 4. AuthGuard 컴포넌트

`frontend/app/components/AuthGuard.tsx`:

```typescript
// Props: { children: React.ReactNode }
// 동작:
//   - loading=true → 로딩 UI 표시 ("불러오는 중..." 기존 스타일)
//   - loading=false + user=null → router.push('/login')
//   - loading=false + user 존재 → children 렌더링
// ⚠️ 로딩 중에 /login으로 리다이렉트하면 안 됨 (깜빡임 방지)
```

### 5. layout.tsx 수정

`frontend/app/layout.tsx`:

- `"use client"` 추가하지 마라. Server Component 유지.
- `AuthProvider`를 Client Component로 import하여 children을 래핑하라.
- BottomNav 조건부 렌더링: BottomNav 내부에서 `usePathname()`으로 /login, /signup 경로를 감지하고 자체적으로 숨김 처리하라 (layout.tsx를 Client Component로 바꾸지 않기 위함).

### 6. BottomNav 수정

`frontend/app/components/BottomNav.tsx`:

```typescript
const hidePaths = ['/login', '/signup'];
const pathname = usePathname();
if (hidePaths.some(p => pathname.startsWith(p))) return null;
```

### 7. 로그인 페이지

`frontend/app/login/page.tsx`:

- `design/screens-auth.jsx`의 Login 컴포넌트를 참고하여 구현
- 상단 창가 SVG 일러스트 (목업에서 SVG 코드 추출)
- 카피: "같은 자리, 같은 잔으로 다시 시작해요"
- 이메일 입력 (메일 아이콘)
- 비밀번호 입력 (자물쇠 아이콘 + 눈 토글)
- "로그인" 버튼
- 에러 메시지: "이메일 또는 비밀번호가 올바르지 않습니다"
- 하단: "계정이 없나요? 회원가입 →" → /signup 링크
- 입력 필드 스타일: `bg-raised rounded-[14px]` (UI_GUIDE.md 참조, 기존 페이지의 bg-soft와 의도적으로 다름)

### 8. 회원가입 페이지

`frontend/app/signup/page.tsx`:

- `design/screens-auth.jsx`의 Signup 컴포넌트를 참고하여 구현
- 카피: "처음 오신 걸 환영해요"
- 닉네임, 이메일, 비밀번호 (최소 8글자 힌트), 비밀번호 확인
- 비밀번호 검증: **onBlur 시점**에서 에러 표시 (입력 중 에러 방지)
- 비밀번호 확인: confirmPassword가 비어있으면 에러 미표시, 입력 후 불일치 시 에러
- 비밀번호 일치 시 세이지 체크 뱃지
- "가입하기" 버튼
- 하단: "이미 회원이신가요? 로그인" → /login 링크
- 가입 성공 시 자동 로그인 → 홈으로 이동

### 9. 홈 페이지 수정

`frontend/app/page.tsx`:

- `useAuth()`에서 user 가져오기
- 인사 헤더 변경: "Cozy Cafe" → `{user.nickname}님 안녕하세요`
- AuthGuard로 보호 (loading 중 로딩 UI, 미로그인 시 리다이렉트)
- useEffect 의존성 배열에 user 추가 (로그아웃 → 재로그인 시 데이터 재로드)

### 10. 다른 페이지 수정

모든 페이지(`words`, `patterns`, `generate`, `review`, `history`, `settings`)의 useEffect에 user 변경 감지를 추가하라. 로그아웃 후 다른 계정으로 로그인 시 이전 사용자 데이터가 남지 않도록 한다.

### 11. FlipCard 모던 스타일

`frontend/app/components/FlipCard.tsx`:

- 앞면: 기존 `bg-raised` 유지 + shadow 강화 (`shadow-md` 또는 커스텀)
- 뒷면: `bg-primary-soft` 배경 (#E8D5BE)
- 참고: `design/screens-review.jsx` 53~56줄

```jsx
// 앞면
className="... bg-raised ... shadow-md"

// 뒷면
className="... bg-primary-soft ..."
```

## Acceptance Criteria

```bash
cd frontend && npm run build && npm run lint
```

- 빌드 에러 없음
- 린트 에러 없음

## 검증 절차

1. `cd frontend && npm run build` 실행
2. `cd frontend && npm run lint` 실행
3. 로그인/회원가입 페이지가 생성되었는지 확인
4. AuthGuard가 layout에 적용되었는지 확인
5. api.ts에 credentials: "include"가 추가되었는지 확인
6. BottomNav가 /login, /signup에서 숨겨지는지 확인
7. FlipCard 뒷면이 bg-primary-soft인지 확인
8. 홈 페이지에 닉네임이 표시되는지 확인

## 금지사항

- 백엔드 코드를 수정하지 마라. 이유: 백엔드는 Step 0~6에서 완료되었다.
- layout.tsx를 `"use client"` Client Component로 바꾸지 마라. 이유: metadata export가 동작하지 않게 된다. AuthProvider는 Client Component로 import하되, layout 자체는 Server Component 유지.
- 새로운 npm 패키지를 설치하지 마라. 이유: 기존 의존성으로 충분하다 (React Context, fetch, Next.js Router).
- 기존 페이지의 비즈니스 로직을 변경하지 마라. 변경하는 것은 useEffect 의존성 배열(user 추가)과 AuthGuard 래핑뿐이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

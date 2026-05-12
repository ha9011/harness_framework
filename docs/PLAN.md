# PLAN: 로그아웃 버튼 + Hydration 수정

## 작업 목표
프론트엔드에 로그아웃 버튼을 추가하고(홈 우상단 + 설정 페이지), body 태그의 hydration mismatch 경고를 제거한다.

## 구현할 기능
1. 홈화면 인사 헤더 우상단에 로그아웃 아이콘 버튼
2. 설정 페이지 하단에 로그아웃 버튼
3. layout.tsx body 태그에 suppressHydrationWarning 추가

## 기술적 제약사항
- 백엔드 변경 없음 (기존 POST /api/auth/logout 엔드포인트 + AuthContext.logout() 활용)
- 프론트엔드만 수정 (3개 파일: page.tsx, settings/page.tsx, layout.tsx)

## 테스트 전략
- 기존 테스트 영향: 없음 (프론트엔드 UI 변경만)
- 신규 테스트: 해당 없음 (프론트엔드 단위테스트 미도입 상태)

## Phase/Step 초안

### Step 0: hydration-fix
- 작업: `frontend/app/layout.tsx`의 `<body>` 태그에 `suppressHydrationWarning` 속성 추가
- 산출물: hydration mismatch 콘솔 경고 제거

### Step 1: logout-buttons
- 작업:
  - `frontend/app/page.tsx` — 인사 헤더 영역에 로그아웃 아이콘 버튼 추가 (useAuth().logout 호출)
  - `frontend/app/settings/page.tsx` — 설정 카드 아래에 "로그아웃" 버튼 추가 (Ghost 스타일)
- 산출물: 홈 우상단 + 설정 페이지에서 로그아웃 가능

## 미결 사항
- 없음

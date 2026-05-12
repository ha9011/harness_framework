# PLAN: 로그인 편의 기능 (토큰 7일 + 이메일 저장)

## 작업 목표
매번 로그인하는 불편함 해소 — JWT 유효기간 7일 확장 + 이메일 저장 체크박스 추가

## 구현할 기능
1. JWT 토큰 유효기간 24시간 → 7일 (쿠키 maxAge 동기화)
2. 로그인 페이지 "이메일 저장" 체크박스 (localStorage)

## 기술적 제약사항
- Refresh Token 도입 안함 (단순 유효기간 확장)
- 비밀번호 저장 안함 (이메일만)
- TDD 준수
- saved-email.ts에서 `typeof window === "undefined"` 가드 필수 (Next.js SSR 환경)

## 테스트 전략
- 기존 테스트 영향:
  - AuthControllerTest: Set-Cookie 검증에 Max-Age=604800 추가
  - IntegrationTestBase: jwt.expiration 86400000 → 604800000 변경
  - JwtProviderTest: EXPIRATION 상수 86400000 → 604800000 변경
- 신규 테스트: saved-email.test.ts (localStorage 헬퍼 단위 테스트, vitest)
- 프론트엔드 테스트 인프라: vitest 신규 설치 필요

## Phase/Step 초안

### Step 0: 백엔드 테스트 — Max-Age 검증 추가 (RED)
- 작업: AuthControllerTest의 login_success, signup_success에 Max-Age=604800 assertion 추가
- 산출물: 테스트 실패 확인 (현재 86400이므로)

### Step 1: 백엔드 구현 — 토큰/쿠키 유효기간 7일 (GREEN)
- 작업: application.yml expiration 604800000, AuthController maxAge 604800, IntegrationTestBase expiration 604800000, JwtProviderTest EXPIRATION 604800000
- 산출물: 백엔드 테스트 전체 통과

### Step 2: 프론트엔드 테스트 인프라 + saved-email 테스트 (RED)
- 작업: vitest 설치, saved-email.test.ts 작성
- 산출물: 테스트 실패 확인 (구현체 없음)

### Step 3: 프론트엔드 구현 — saved-email 헬퍼 + 로그인 페이지 (GREEN)
- 작업: saved-email.ts 구현, login/page.tsx에 체크박스 + useEffect 추가
- 산출물: 프론트엔드 테스트 통과 + 빌드 성공

## 미결 사항
- 화이트리스트 외 문서에 "24시간/86400" 잔존 — 구현 완료 후 별도 업데이트 필요:
  - API설계서.md (60, 84행): Max-Age=86400
  - 요구사항정의서.md (121, 131행): 24시간 만료
  - 기획서.md (86행): 24시간
  - DESIGN.md (19행): 24시간 만료

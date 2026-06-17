# PLAN: iOS 홈화면 PWA 로그인 유지 — 하이브리드 인증 (Phase 10-pwa-auth)

## 작업 목표
홈화면 PWA에서 강제 종료 후에도 로그인이 유지되도록, 기존 쿠키 인증에 더해 localStorage 토큰 + `Authorization: Bearer` 헤더 인증 경로를 추가한다.

## 구현할 기능
1. **백엔드**: `JwtAuthenticationFilter`와 `/auth/me`가 헤더(Bearer) 또는 쿠키 토큰을 모두 수용 + 로그인/회원가입 응답 body에 JWT 토큰 노출
2. **프론트엔드**: 로그인/회원가입 시 토큰을 localStorage 저장, `api.ts`가 Authorization 헤더 첨부, 로그아웃·401 시 토큰 제거

## 기술적 제약사항
- CRITICAL: 백엔드는 `backend/`, 프론트엔드는 `frontend/`에서만 작업 (harness 룰6 — step 분리)
- 쿠키 인증은 **그대로 유지**(HttpOnly/Secure/SameSite/Path/Max-Age 불변). 쿠키는 1차 경로, localStorage 토큰은 PWA 폴백 (ADR-020)
- `JwtAuthenticationFilter`: Authorization 헤더(Bearer) **우선**, 없으면 쿠키. 동일 JWT 검증 경로 재사용
- `GET /api/auth/me`는 현재 `@CookieValue`로 쿠키만 직접 읽음 → **헤더도 수용하도록 반드시 변경**(또는 SecurityContext 경유). 누락 시 PWA에서 /auth/me 401 지속
- `AuthResponse`는 /auth/me와 공용 + 다수 테스트가 `@AllArgsConstructor` 3-arg 생성자 사용 → **필드 추가 금지**. 로그인/회원가입 전용 응답 DTO를 신설해 토큰을 노출(비파괴)
- 프론트 토큰 헬퍼는 `saved-email.ts`와 동형(`typeof window` SSR 가드)
- 로그아웃·401 처리에서 localStorage 토큰을 반드시 제거(보안 노출면 축소 + 만료 토큰 루프 방지)
- 보안: localStorage 토큰은 XSS 노출 위험 수용(ADR-020). Refresh Token/토큰 회전은 범위 외

## 테스트 전략
- **기존 테스트 영향**: 없음(쿠키 계속 발급되므로 기존 쿠키 인증 테스트 유지). 전용 DTO 사용으로 `AuthResponse` 생성자 사용처 비파괴 → 컴파일/단언 영향 없음
- **신규 테스트(백엔드)**:
  - AuthIntegrationTest: 로그인 응답 body에서 토큰 추출 → **쿠키 없이 `Authorization: Bearer <token>`만으로** 보호 엔드포인트(GET /api/auth/me 또는 /api/dashboard) 호출 시 200
  - 로그인/회원가입 응답 body에 token 필드가 존재함을 단언
  - (회귀) 쿠키만으로의 기존 인증 경로도 여전히 200
- **신규 테스트(프론트)**: `auth-token.ts` 헬퍼 단위 테스트(get/set/clear, SSR 가드). api.ts 헤더 첨부는 build/lint + 기존 Vitest 회귀로 확인
- **테스트 수정이 필요한 Step**: 없음(추가만). 만약 부득이 AuthResponse를 변경해 기존 생성자 호출이 깨지면 PRD 기능 15 근거로 summary에 명시 — 단, 전용 DTO 방식으로 회피하는 것을 우선한다

## Phase/Step 초안

### Step 0: bearer-header-auth (백엔드)
- Layer: controller
- 작업:
  - `JwtAuthenticationFilter`: `Authorization: Bearer` 헤더에서 토큰 추출(없으면 기존 쿠키 추출)로 확장. 검증/SecurityContext 설정 로직은 재사용
  - `AuthController.getMe`: 쿠키(`@CookieValue`) 외에 `Authorization` 헤더도 수용하도록 변경(또는 SecurityContext의 인증 주체 사용)
  - 로그인/회원가입 응답에 JWT 토큰을 포함하는 **전용 응답 DTO 신설**(예: `{ id, email, nickname, token }` 또는 `{ token, user }`). `AuthResponse`는 미변경. `LoginResult.getToken()` 재사용
  - 쿠키 발급(`createTokenCookie`)은 변경 없음
- 산출물: 확장된 필터/컨트롤러 + 전용 DTO + AuthIntegrationTest 신규 케이스. `./gradlew test` green
- Critical Gates: 쿠키 없이 Authorization 헤더만으로 보호 엔드포인트 200 + 로그인 응답 body에 토큰 포함 + 기존 쿠키 인증 회귀 없음

### Step 1: pwa-token-storage (프론트엔드)
- Layer: frontend-view
- 작업:
  - `lib/auth-token.ts` 신설: `getToken/setToken/clearToken`(localStorage, `typeof window` 가드)
  - `lib/auth-context.tsx`: 로그인/회원가입 성공 시 응답 토큰을 `setToken`. 로그아웃 시 `clearToken`(+ 기존 /auth/logout)
  - `lib/api.ts`: `request`/`uploadRequest`에서 토큰 있으면 `Authorization: Bearer` 헤더 첨부(기존 `credentials: "include"` 유지). 401 'unauthorized' 처리 시 `clearToken`
  - 로그인/회원가입 응답 타입에 token 반영(types.ts)
- 산출물: auth-token 헬퍼 + api/auth-context 수정 + 헬퍼 단위 테스트. `npm run build`·`lint`·`test` green
- Critical Gates: auth-token 헬퍼 테스트 통과 + 빌드/린트/기존 Vitest 회귀 없음 + (수동) iOS PWA에서 로그인 유지 확인은 배포 후

### Step 2: 운영 검증 (배포 후, 수동 — 자동화 범위 밖)
- 작업: 배포 후 iOS 홈화면 PWA에서 로그인 → 강제 종료 → 재실행 시 로그인 유지 확인. Safari·데스크톱도 기존대로 동작 확인
- 산출물: 수동 검증 결과

## 성공 기준
- 백엔드: 쿠키 없이 `Authorization: Bearer` 헤더만으로 보호 엔드포인트 200, 로그인 응답에 토큰 포함, 기존 쿠키 경로 회귀 없음 (테스트 green)
- 프론트: 로그인 시 토큰 localStorage 저장, 모든 API 요청에 헤더 첨부, 로그아웃·401 시 토큰 제거
- 운영: iOS 홈화면 PWA 강제 종료 후에도 로그인 유지

## 미결 사항
- Refresh Token / 토큰 회전 미도입(7일 만료 그대로) — 후속 과제
- localStorage 토큰 XSS 노출 위험은 ADR-020에서 수용 결정(개인 학습 앱). 장기적으로 in-memory + refresh 패턴 검토 가능
- manifest.json / 서비스워커가 이미 있으면 유지, 신규 PWA 보강은 범위 외

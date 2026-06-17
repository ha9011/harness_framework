# PLAN: 운영 로그인 세션 유지 + 모바일 입력 줌 수정 (Phase 9-auth-mobile-fix)

## 작업 목표
운영(HTTPS, iOS Safari) 환경의 두 버그를 수정한다 — (1) 백그라운드 복귀 시 로그아웃, (2) 로그인 입력 포커스 시 화면 줌인이 메인페이지까지 유지.

## 구현할 기능
1. **세션 유지**: JWT 쿠키에 `Secure` 속성을 profile-aware하게 부여 (prod=true, local/default=false)
2. **입력 줌 방지**: 모바일 form control 폰트를 16px 이상으로 보장 + layout.tsx viewport 명시

## 기술적 제약사항
- CRITICAL: 백엔드는 `backend/`, 프론트엔드는 `frontend/`에서만 작업
- Cloudflare Flexible SSL이라 origin(Spring)은 HTTP로 요청을 받음 → `request.isSecure()` 자동판단 불가, **설정값 `app.cookie.secure`로 명시 분기** (ADR-018)
- 로컬 HTTP 개발에서 Secure=true면 쿠키 미전송 → 로그인 깨짐. 반드시 local/default=false
- SameSite=Lax / Max-Age=7일 / HttpOnly / Path=/api / JWT 만료(7일)는 변경 없음
- `user-scalable=no`/`maximum-scale=1`로 줌을 막지 않는다 (접근성 위배, iOS 무시) — 16px 폰트 방식 (ADR-019)
- 데스크톱 시각 디자인 보존: 16px 강제는 미디어쿼리/coarse pointer 한정 적용
- TDD: 백엔드는 테스트 먼저. 프론트 시각 변경은 빌드 + 수동(iOS) 확인

## 테스트 전략
- **기존 테스트 영향**: 없음. AuthControllerTest/AuthIntegrationTest는 Set-Cookie의 `Max-Age=604800`/`Max-Age=0`만 `containsString`으로 단언 → `Secure` 추가에도 통과. MockMvc/통합 테스트는 prod profile 미사용 → `app.cookie.secure` 기본 false
- **신규 테스트**: AuthControllerTest에 `app.cookie.secure=true`(`@TestPropertySource`)일 때 Set-Cookie에 `Secure` 포함, 미설정/false일 때 미포함을 단언하는 테스트 추가
- **테스트 수정이 필요한 Step**: 없음 (추가만)
- **프론트엔드**: globals.css 폰트 규칙 / layout.tsx viewport는 시각적 변경 → `npm run build` + `npm run lint` 통과로 검증, iOS Safari 수동 확인(포커스 시 줌 없음, 메인페이지 정상 비율)

## Phase/Step 초안

### Step 0: 백엔드 쿠키 Secure profile-aware 설정 (TDD)
- 작업:
  - `application-local.yml`(및 공통 `application.yml` 기본값)에 `app.cookie.secure: false`, `application-prod.yml`에 `app.cookie.secure: true` 추가
  - `AuthController`가 `app.cookie.secure` 값을 주입받아(`@Value("${app.cookie.secure:false}")` — **기본값 false로 테스트/누락 환경 안전**) `createTokenCookie`에서 `.secure(...)` 적용 (기존 httpOnly/path/sameSite/maxAge 유지). `@RequiredArgsConstructor`라 final 필드면 생성자 파라미터로, 아니면 `@Value` 필드 주입
  - set/clear 공통 헬퍼라 로그아웃 쿠키도 동일 속성으로 발급됨(쿠키 삭제 정합)
  - 테스트 먼저: cookie.secure=true(`@TestPropertySource`) → Set-Cookie에 `Secure` 포함 단언, 기본(false) → 미포함 단언
- 산출물: 수정된 AuthController + application-*.yml + AuthControllerTest(신규 단언). `./gradlew test` green

### Step 1: 프론트엔드 모바일 입력 줌 방지
- 작업:
  - `app/globals.css`에 모바일 한정 form control 16px 규칙 추가. **Tailwind 특이성 주의**: `@media (pointer: coarse) { input, textarea, select { font-size: 16px !important; } }` — `!important` 없으면 `text-sm` 유틸을 못 이겨 무효(ADR-019). 입력 5개 파일(login/signup/words·page/WordAddModal/PatternAddModal)을 전역 규칙 1곳으로 일괄 처리
  - (선택) `app/layout.tsx`에 `viewport` export 추가 (`width: "device-width", initialScale: 1`) — 줌 해결의 핵심 아님(명시용). Next.js 16 viewport API는 `frontend/AGENTS.md` 지시대로 `node_modules/next/dist/docs/`에서 현재 시그니처 확인 후 적용
- 산출물: 수정된 globals.css(+선택 layout.tsx). `npm run build` + `npm run lint` 통과. iOS Safari에서 포커스 줌 미발생 수동 확인

### Step 2: 운영 검증 (배포 후)
- 작업: prod 빌드/배포 후 iOS Safari에서 (1) 로그인 → 백그라운드 → 복귀 시 세션 유지, (2) 로그인 입력 포커스 시 줌 없음 + 메인페이지 정상 비율 확인
- **Secure 적용 후에도 (1)이 재현되면** 추가 진단: 운영 응답의 실제 Set-Cookie 헤더 확인(Secure/SameSite/Max-Age 실제 부여 여부), iOS 웹인스펙터로 쿠키 잔존 확인, 인앱 브라우저/PWA 여부 확인 → 원인 재특정
- 산출물: 수동 검증 결과 (성공 기준 충족 확인)

## 성공 기준
- 백엔드: `app.cookie.secure=true` 시 Set-Cookie에 `Secure` 포함, false 시 미포함 (테스트 통과). 기존 테스트 전부 green
- 프론트: iOS Safari에서 로그인 입력 포커스 시 자동 줌 발생 안 함, 메인페이지가 확대되지 않음
- 운영: iOS Safari 백그라운드 복귀 후에도 로그인 세션 유지

## 미결 사항
- 인앱 브라우저(카카오 등)/PWA standalone의 더 제한적인 쿠키 유지 정책 — 이번 범위 외, 재현 시 별도 과제
- next.config.ts dev rewrites 부재(ADR-017 트레이드오프)와 별개 이슈 — 이번 작업 범위 아님

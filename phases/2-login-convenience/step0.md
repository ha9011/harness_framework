# Step 0: backend-token-expiry

## Step Contract

- Capability: JWT 토큰 유효기간 24시간 → 7일 확장 (쿠키 maxAge 동기화)
- Layer: controller
- Write Scope: `backend/` 디렉토리 전체
- Out of Scope: `frontend/` 디렉토리 수정. Refresh Token 도입. 새로운 API 엔드포인트 추가
- Critical Gates: `cd backend && ./gradlew test --tests "com.english.auth.AuthControllerTest"` (Max-Age=604800 검증) + `cd backend && ./gradlew test` (전체 회귀 없음, AuthIntegrationTest logout Max-Age=0 유지)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 작업 목표와 전체 계획
- `/docs/ARCHITECTURE.md` — 인증 흐름, 환경 설정 섹션
- `/docs/ADR.md` — ADR-007: JWT + HttpOnly Cookie 인증 (7일 만료로 변경됨)
- `backend/src/main/java/com/english/auth/AuthController.java` — 쿠키 생성 로직 (28행, 39행의 maxAge)
- `backend/src/main/resources/application.yml` — jwt.expiration 설정 (25행)
- `backend/src/test/java/com/english/auth/AuthControllerTest.java` — 기존 테스트 구조 파악
- `backend/src/test/java/com/english/auth/JwtProviderTest.java` — EXPIRATION 상수 (13행)
- `backend/src/test/java/com/english/integration/IntegrationTestBase.java` — jwt.expiration 오버라이드 (45행)
- `backend/src/test/java/com/english/integration/AuthIntegrationTest.java` — logout Max-Age=0 검증 (79행, 변경하면 안 됨)

## 작업

### 1. 테스트 보강 (TDD — RED 확인 후 GREEN)

**`AuthControllerTest.java`** — 기존 `login_success`와 `signup_success` 테스트에 Set-Cookie의 Max-Age=604800 검증 assertion을 추가한다.

```java
// 추가할 assertion 예시 (기존 andExpect 체인에 추가)
.andExpect(header().string("Set-Cookie",
    org.hamcrest.Matchers.containsString("Max-Age=604800")))
```

### 2. 프로덕션 코드 수정

**`application.yml`** (25행):
- `jwt.expiration`: `86400000` → `604800000` (7일, 밀리초)

**`AuthController.java`** (28행, 39행):
- `createTokenCookie(...)` 호출 시 두 번째 인자: `86400` → `604800` (7일, 초)
- logout의 `createTokenCookie("", 0)`은 변경하지 않는다.

### 3. 테스트 코드 정합성

**`IntegrationTestBase.java`** (45행):
- `registry.add("jwt.expiration", () -> "86400000")` → `"604800000"` 변경

**`JwtProviderTest.java`** (13행):
- `EXPIRATION = 86400000L; // 24시간` → `EXPIRATION = 604800000L; // 7일` 변경

### 주의: 변경하면 안 되는 테스트

- `AuthIntegrationTest.java` 79행의 `assertThat(logoutCookie).contains("Max-Age=0")` — 이것은 로그아웃 쿠키 삭제 검증이므로 절대 변경하지 마라. 이유: 로그아웃 시 쿠키 즉시 만료는 기능 요구사항이며 이번 변경과 무관하다.

## Acceptance Criteria

```bash
# Critical Gates
cd backend && ./gradlew test --tests "com.english.auth.AuthControllerTest.login_success"
cd backend && ./gradlew test --tests "com.english.auth.AuthControllerTest.signup_success"

# 보조 검증: 전체 테스트 회귀 없음
cd backend && ./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates 확인:
   - login_success에서 `Max-Age=604800`이 Set-Cookie에 포함되는가?
   - signup_success에서 `Max-Age=604800`이 Set-Cookie에 포함되는가?
   - AuthIntegrationTest의 logout `Max-Age=0` 검증이 여전히 통과하는가?
3. 아키텍처 체크리스트:
   - ARCHITECTURE.md의 인증 흐름에 기술된 대로 HttpOnly Cookie를 사용하는가?
   - ADR-007의 "7일 만료" 결정에 부합하는가?
   - `frontend/` 디렉토리를 수정하지 않았는가?
4. 결과에 따라 `phases/2-login-convenience/index.json`의 step 0을 업데이트한다.

## 금지사항

- `frontend/` 디렉토리를 수정하지 마라. 이유: 백엔드/프론트 분리 원칙. 프론트엔드는 step 1에서 처리한다.
- `AuthIntegrationTest.java`의 `Max-Age=0` assertion을 변경하지 마라. 이유: 로그아웃 기능은 이번 변경 범위가 아니다.
- Refresh Token 관련 코드를 추가하지 마라. 이유: ADR-007에서 Access Token만 사용하기로 결정했다.
- `TODO`, `not implemented`, `stub`으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 단, 이 step에서 의도적으로 변경하는 Max-Age 값(86400→604800)과 EXPIRATION 상수는 예외. ⚠️ 이 변경의 근거: ADR-007 (7일 만료 결정), PLAN.md (토큰 유효기간 확장 요구사항).
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

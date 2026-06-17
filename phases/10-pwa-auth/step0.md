# Step 0: bearer-header-auth

## Step Contract

- Capability: `Authorization: Bearer` 헤더 기반 인증 경로 추가(쿠키와 병행) + 로그인/회원가입 응답 body에 JWT 토큰 노출. 쿠키 인증은 그대로 유지
- Layer: controller
- Write Scope: `backend/src/main/java/com/english/auth/JwtAuthenticationFilter.java`, `backend/src/main/java/com/english/auth/AuthController.java`, `backend/src/main/java/com/english/auth/`(로그인/회원가입 전용 응답 DTO 신규 1개), `backend/src/test/java/com/english/integration/AuthIntegrationTest.java`, `backend/src/test/java/com/english/auth/AuthControllerTest.java`
- Out of Scope: frontend 코드 일체, `AuthResponse.java` 필드 변경(아래 이유 참고), `AuthService` 비즈니스 로직 변경, `createTokenCookie`/쿠키 속성 변경, `SecurityConfig`/`JwtProvider` 변경
- Critical Gates: `cd backend && ./gradlew test --tests 'com.english.integration.AuthIntegrationTest'` 통과 — (a) 로그인 응답 body에서 토큰을 추출해 **쿠키 없이 `Authorization: Bearer <token>`만으로** 보호 엔드포인트(GET /api/auth/me) 호출 시 200 + 사용자 정보 반환, (b) 로그인/회원가입 응답 body에 `token`이 존재, (c) 기존 쿠키만으로의 인증 경로도 여전히 200(회귀 없음). 추가로 `cd backend && ./gradlew test` 전체 회귀 없음

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Phase 10-pwa-auth 전체 계획 (Step 0 = 백엔드 헤더 인증)
- `/docs/ADR.md` — **ADR-020**(하이브리드 인증), 참고로 **ADR-007**(원래 HttpOnly 쿠키 채택 이유)
- `/docs/ARCHITECTURE.md` — "인증 흐름"의 하이브리드 인증 주석
- `/backend/src/main/java/com/english/auth/JwtAuthenticationFilter.java` — 현재 `extractTokenFromCookies`로 쿠키만 읽음. doFilterInternal에서 토큰 검증 → SecurityContext 설정
- `/backend/src/main/java/com/english/auth/AuthController.java` — `login`/`signup`/`getMe` 구현. **`getMe`는 `@CookieValue(name="token")`로 쿠키를 직접 읽어 필터를 우회**함에 주의. `login`은 `result.getAuthResponse()` 반환, `signup`은 내부에서 `authService.login(...)`으로 `LoginResult`(token 포함)를 이미 구함
- `/backend/src/main/java/com/english/auth/AuthResponse.java` — `@AllArgsConstructor`, 필드 `id/email/nickname`, `from(User)`. /auth/me와 공용 + 다수 테스트가 3-arg 생성자 사용
- `/backend/src/main/java/com/english/auth/LoginResult.java` — `{ token, authResponse }`
- `/backend/src/test/java/com/english/integration/AuthIntegrationTest.java` — TestContainers 기반 실제 Security 체인. Set-Cookie에서 토큰 추출하는 기존 패턴 참고
- `/backend/src/test/java/com/english/auth/AuthControllerTest.java` — standalone Mockito + MockMvc 구조

이전 작업에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 1. `JwtAuthenticationFilter` — Authorization 헤더 우선 추출

토큰 추출을 "Authorization 헤더(Bearer) → 없으면 쿠키" 순으로 확장한다. 검증/SecurityContext 설정 로직은 그대로 재사용한다.

- `request.getHeader("Authorization")`이 `Bearer ` 접두사로 시작하면 그 뒤 토큰을 사용
- 없으면 기존 `extractTokenFromCookies(request)` 사용
- 추출 이후 `jwtProvider.validateToken` → `SecurityContextHolder` 설정 로직은 변경 없음

### 2. `AuthController.getMe` — 헤더도 수용

**핵심**: 현재 `getMe`는 `@CookieValue(name="token")`로 쿠키만 읽어 PWA(쿠키 없음)에서 항상 401이 된다. 헤더 토큰도 받도록 변경한다.

- 시그니처에 `@RequestHeader(value = "Authorization", required = false) String authHeader`를 추가
- 토큰 결정: `authHeader`가 `Bearer `로 시작하면 그 토큰, 아니면 기존 `@CookieValue` 쿠키 토큰
- 그 토큰으로 기존과 동일하게 `jwtProvider.validateToken` 검증 → 실패 시 `AuthenticationException`
- (대안 허용) SecurityContext의 인증 주체를 사용해도 되지만, 결과적으로 **헤더 또는 쿠키 어느 쪽으로도 /auth/me가 인증되어야 한다**

### 3. 로그인/회원가입 응답에 토큰 포함 — 전용 DTO 신설

`AuthResponse`에 필드를 추가하지 마라. 이유: `/auth/me`와 공용이고 다수 테스트가 `new AuthResponse(id, email, nickname)` 3-arg 생성자를 사용하므로 필드 추가 시 컴파일이 깨진다.

- 로그인/회원가입 전용 응답 DTO를 **신규 생성**한다. 예: `AuthTokenResponse { Long id; String email; String nickname; String token; }` + 정적 팩토리(`of(AuthResponse, token)` 또는 `from(User, token)`)
- `AuthController.login`: 반환 타입을 새 DTO로 바꾸고 `result.getAuthResponse()` + `result.getToken()`으로 body 구성. **쿠키 발급(Set-Cookie)은 그대로 유지**
- `AuthController.signup`: 이미 컨트롤러에서 `LoginResult`(token 포함)를 구하므로, 그 token + 가입 결과로 새 DTO body 구성. 쿠키 발급 유지
- `getMe`의 반환 타입은 기존 `AuthResponse` 그대로 둔다(토큰 노출 불필요)

### 4. 테스트 (기존 메서드 수정 금지, 신규 추가)

- `AuthIntegrationTest`: 신규 케이스 추가
  - 로그인 → 응답 **body의 token** 추출 → 새 요청에 **쿠키를 싣지 않고** `Authorization: Bearer <token>` 헤더만으로 `GET /api/auth/me` → 200 + 사용자 정보
  - 기존 쿠키 기반 인증 케이스가 여전히 통과(회귀 확인)
- `AuthControllerTest`: 로그인/회원가입 응답 body에 `token` 필드가 존재함을 단언하는 신규 테스트 추가(standalone Mockito — `LoginResult`에 들어간 토큰 문자열이 body에 나오는지 `jsonPath("$.token")`로 확인). 기존 `$.id/$.email/$.nickname` 단언은 새 DTO에도 동일 필드라 그대로 통과

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests 'com.english.integration.AuthIntegrationTest'
cd backend && ./gradlew test --tests 'com.english.auth.AuthControllerTest'
cd backend && ./gradlew test
```

- 쿠키 없이 Authorization 헤더만으로 /api/auth/me 200 (헤더 인증 경로 동작 증명)
- 로그인/회원가입 응답 body에 token 포함
- 기존 쿠키 인증 + 전체 테스트 회귀 없음

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - 단순 `./gradlew test` 전체 통과만으로 completed 처리하지 않았는가? → **쿠키 없이 헤더만으로 인증 200**을 명시적으로 검증했는가?
   - `/auth/me`가 헤더로도 인증되는가? (쿠키 전용이면 PWA에서 여전히 401 — 이 step의 핵심)
3. 아키텍처 체크리스트를 확인한다:
   - ADR-020(헤더 우선, 쿠키 폴백, 쿠키 유지) 결정을 따르는가?
   - `AuthResponse`를 변경하지 않고 전용 DTO로 토큰을 노출했는가?
   - CLAUDE.md CRITICAL(backend/ 한정) 준수, Write Scope 밖(frontend/, SecurityConfig 등) 미수정?
   - frontend 코드를 동시에 수정하지 않았는가? (이 step은 backend만)
4. 결과에 따라 `phases/10-pwa-auth/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약(신규 DTO명, getMe/filter 변경, 헤더 인증 테스트 통과 명시)"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- frontend 코드(`frontend/`)를 수정하지 마라. 이유: 이 step은 backend 한정 (룰6 위반).
- `AuthResponse.java`에 필드를 추가하지 마라. 이유: /auth/me 공용 + 다수 테스트의 3-arg 생성자 사용처가 깨진다. 전용 DTO로 토큰을 노출하라.
- 쿠키 발급(`createTokenCookie`, Set-Cookie)을 제거하거나 속성을 바꾸지 마라. 이유: Safari·데스크톱은 쿠키가 1차 경로. 쿠키는 그대로 유지하고 헤더 경로를 "추가"하는 것이다.
- `getMe`를 쿠키 전용으로 남겨두지 마라. 이유: PWA는 쿠키가 없으므로 헤더를 수용하지 않으면 /auth/me가 계속 401 → 이 작업의 목적 자체가 무산된다.
- 헤더 인증을 검증하지 않고 completed 하지 마라(쿠키 테스트만으로 끝내지 마라). 이유: 핵심은 "쿠키 없이 헤더만으로" 인증되는 것이다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: 회귀를 숨긴다. 신규 메서드만 추가하라.
- `TODO`/`stub`/고정 더미로 핵심 기능을 대체하고 completed 처리하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

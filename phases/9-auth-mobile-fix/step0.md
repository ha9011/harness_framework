# Step 0: auth-cookie-secure

## Step Contract

- Capability: JWT 인증 쿠키에 profile-aware `Secure` 속성 부여 (운영 HTTPS에서만 Secure=true, 로컬/테스트/default profile은 false)
- Layer: controller
- Write Scope: `backend/src/main/java/com/english/auth/AuthController.java`, `backend/src/main/resources/application-prod.yml`, `backend/src/test/java/com/english/auth/AuthControllerTest.java`
- Out of Scope: frontend 코드 일체, `application.yml`/`application-local.yml`/`logback-spring.xml` 수정, `AuthService`/`JwtProvider`/`JwtAuthenticationFilter` 수정, SameSite/Path/HttpOnly/Max-Age/JWT 만료 정책 변경
- Critical Gates: `cd backend && ./gradlew test --tests 'com.english.auth.AuthControllerTest'` 통과 — 신규 테스트가 (a) 기본(cookieSecure=false)에서 `POST /api/auth/login` 응답 Set-Cookie에 `Secure` **미포함**, (b) `ReflectionTestUtils.setField(authController, "cookieSecure", true)` 적용 후 login/signup 응답 Set-Cookie에 `Secure` **포함**을 단언. 추가로 `cd backend && ./gradlew test` 전체 회귀 없음

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Phase 9-auth-mobile-fix 전체 계획 (Step 0 = 백엔드 쿠키 Secure)
- `/docs/ADR.md` — **ADR-018** (JWT 쿠키 `Secure` 속성 profile-aware 설정). Cloudflare Flexible SSL이라 origin은 HTTP로 요청을 받으므로 `request.isSecure()` 자동판단 불가 → 설정값으로 분기하는 이유 숙지
- `/docs/ARCHITECTURE.md` — "인증 흐름"의 Set-Cookie 라인 + "환경 설정"의 `app.cookie.secure` 항목
- `/backend/src/main/java/com/english/auth/AuthController.java` — 현재 `createTokenCookie(String token, long maxAge)` 구현. `httpOnly(true).path("/api").sameSite("Lax").maxAge(maxAge)` 체인 확인. `@RequiredArgsConstructor` + `private final` 필드 구조
- `/backend/src/main/resources/application-prod.yml` — 운영 profile (여기에 `app.cookie.secure: true` 추가)
- `/backend/src/test/java/com/english/auth/AuthControllerTest.java` — **`@ExtendWith(MockitoExtension.class)` + `MockMvcBuilders.standaloneSetup(authController)`** 구조임을 반드시 확인 (Spring 컨텍스트 없음)

이전 작업에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 1. `AuthController.java` — 설정값 주입 + `.secure(...)` 적용

쿠키의 `Secure` 여부를 설정값으로 제어한다. **요청 scheme로 자동 판단하지 마라** (Cloudflare Flexible은 CF↔origin이 HTTP라 운영에서도 `request.isSecure()`가 false가 됨 — ADR-018).

`@Value` 필드를 추가한다 (기본값 `false` 필수):

```java
@Value("${app.cookie.secure:false}")
private boolean cookieSecure;
```

`createTokenCookie`에 `.secure(cookieSecure)` 한 줄만 추가한다:

```java
private ResponseCookie createTokenCookie(String token, long maxAge) {
    return ResponseCookie.from("token", token)
            .httpOnly(true)
            .path("/api")
            .sameSite("Lax")
            .secure(cookieSecure)   // ← 추가
            .maxAge(maxAge)
            .build();
}
```

핵심 규칙:
- `@Value("${app.cookie.secure:false}")`의 **기본값 `:false`를 반드시 포함**하라. 이유: prod 외 profile(local/default/test)에서 프로퍼티가 없어도 false로 안전하게 동작해야 함
- 기존 `httpOnly(true)` / `path("/api")` / `sameSite("Lax")` / `maxAge(maxAge)`의 값과 순서를 바꾸지 마라. `.secure(...)`만 추가
- `createTokenCookie`는 set(login/signup, maxAge=604800)과 clear(logout, maxAge=0)에 공통 사용되므로, 로그아웃 쿠키도 동일하게 Secure가 적용된다 (쿠키 삭제 정합 — 의도된 동작)

### 2. `application-prod.yml` — 운영 profile에 설정 추가

운영은 HTTPS(Cloudflare)이므로 Secure를 켠다. 파일 끝에 아래 블록을 **추가**한다 (기존 항목 수정 금지):

```yaml
app:
  cookie:
    secure: true
```

### 3. `AuthControllerTest.java` — 신규 테스트 추가 (기존 메서드 수정 금지)

**중요**: 이 테스트는 `@WebMvcTest`가 아니라 `MockitoExtension` + `MockMvcBuilders.standaloneSetup(authController)` 구조다. Spring 컨텍스트가 없어 **`@Value`가 주입되지 않고 `@TestPropertySource`도 적용되지 않는다.** 따라서 `cookieSecure` 값은 `org.springframework.test.util.ReflectionTestUtils.setField(authController, "cookieSecure", <bool>)`로 직접 세팅한다.

신규 테스트 메서드 2~3개를 추가하라 (기존 메서드는 건드리지 말 것):

- **(a) 기본(false) — Secure 미부여**: `cookieSecure`를 세팅하지 않은 기본 상태(boolean 기본값 false)에서 `POST /api/auth/login` 수행 → `header().string("Set-Cookie", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Secure")))` 단언
- **(b) prod(true) — Secure 부여**: `ReflectionTestUtils.setField(authController, "cookieSecure", true)` 호출 후 `POST /api/auth/login` 수행 → `header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Secure"))` 단언
- **(c) (권장) signup도 true 분기**: (b)와 동일하게 setField(true) 후 `POST /api/auth/signup` → Set-Cookie에 `Secure` 포함 단언

기존 `signup_success`/`login_success`의 mock 셋업(`given(authService...)`) 패턴을 그대로 참고해 재현하라. `@BeforeEach`의 standaloneSetup은 매 테스트 새 `authController` 인스턴스를 쓰므로 (b)에서 setField한 값이 (a)로 누수되지 않는다.

핵심 규칙:
- import 추가: `org.springframework.test.util.ReflectionTestUtils`
- 기존 테스트 메서드의 `andExpect`(특히 `containsString("Max-Age=604800")`)를 변경하지 마라

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests 'com.english.auth.AuthControllerTest'
cd backend && ./gradlew test
```

- AuthControllerTest 신규 단언 통과: cookieSecure=false → Set-Cookie에 `Secure` 미포함, cookieSecure=true → `Secure` 포함
- 전체 테스트 회귀 없음 (기존 AuthControllerTest/AuthIntegrationTest 등 그대로 통과)

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - 단순 `./gradlew test` 전체 통과만으로 completed 처리하지 않았는가? → Secure **분기**(false 미포함 / true 포함)를 명시적으로 단언했는가?
   - `ReflectionTestUtils.setField`로 true 케이스를 검증했는가? (standalone Mockito라 @TestPropertySource는 무효임을 이해했는가?)
3. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가?
   - ADR-018(설정값 분기, scheme 자동판단 금지) 결정을 위반하지 않았는가?
   - CLAUDE.md CRITICAL 규칙(backend/ 폴더 외부 작성 금지)을 위반하지 않았는가?
   - Step Contract의 Write Scope 밖(frontend/, application.yml 등)을 수정하지 않았는가?
   - frontend 코드를 동시에 수정하지 않았는가? (이 step은 backend만)
4. 결과에 따라 `phases/9-auth-mobile-fix/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- frontend 코드(`frontend/`)를 수정하지 마라. 이유: 이 step은 backend 한정 (룰6 위반).
- 쿠키 Secure 여부를 `request.isSecure()`나 `X-Forwarded-Proto` 파싱으로 판단하지 마라. 이유: Cloudflare Flexible은 CF↔origin이 HTTP라 운영에서도 false가 나옴 → 반드시 `app.cookie.secure` 설정값으로 분기 (ADR-018).
- `@Value("${app.cookie.secure}")`에서 기본값 `:false`를 빼지 마라. 이유: prod 외 profile에서 프로퍼티 미해결로 부팅/테스트가 깨진다.
- `createTokenCookie`의 `sameSite`/`path`/`httpOnly`/`maxAge` 값을 바꾸지 마라. 이유: 세션 정책 회귀(SameSite=Lax, Path=/api, 7일 만료는 유지).
- `application.yml`/`application-local.yml`에 `app.cookie.secure`를 추가하지 마라. 이유: 인라인 `@Value` 기본값 false로 충분. 공유 dev 설정 불필요 변경 회피.
- 테스트를 `@WebMvcTest`/`@SpringBootTest`/`@TestPropertySource`로 재작성하지 마라. 이유: 기존 standalone Mockito 구조에 맞춰 `ReflectionTestUtils`로 해결. 테스트 인프라를 바꾸면 회귀 위험.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다.
- `TODO`, `not implemented`, `stub`, 빈 객체/고정 더미 반환으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

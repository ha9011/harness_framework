# Step 3: AuthService + AuthController (TDD)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 보안 체크리스트, 로그인 실패 메시지 정책
- `/docs/ARCHITECTURE.md` — 인증 흐름, 에러 처리 흐름 (GlobalExceptionHandler)
- `/API설계서.md` — 섹션 0. 인증 API (signup/login/logout/me 상세 스펙)
- `/backend/src/main/java/com/english/auth/User.java` — Step 1
- `/backend/src/main/java/com/english/auth/UserRepository.java` — Step 1
- `/backend/src/main/java/com/english/auth/JwtProvider.java` — Step 2
- `/backend/src/main/java/com/english/word/WordService.java` — Service 패턴 참고
- `/backend/src/main/java/com/english/word/WordController.java` — Controller 패턴 참고
- `/backend/src/main/java/com/english/config/GlobalExceptionHandler.java` — 기존 예외 핸들러

## 작업

### 1. DTO

`backend/src/main/java/com/english/auth/` 패키지에 DTO 생성:

**SignupRequest**: email (`@Email @NotBlank`), password (`@NotBlank @Size(min=8)`), nickname (`@NotBlank`)
**LoginRequest**: email (`@Email @NotBlank`), password (`@NotBlank`)
**AuthResponse**: id (Long), email (String), nickname (String)

### 2. AuthService

`backend/src/main/java/com/english/auth/AuthService.java`:

```java
public AuthResponse signup(SignupRequest request)
// 1. 이메일 중복 검사 → existsByEmail → true면 DuplicateException
// 2. 비밀번호 BCrypt 해싱 (PasswordEncoder 주입)
// 3. User 생성 + 저장
// 4. AuthResponse 반환

public LoginResult login(LoginRequest request)
// 1. findByEmail → 없으면 AuthenticationException
// 2. passwordEncoder.matches → 불일치면 AuthenticationException
// 3. JWT 토큰 생성 (JwtProvider)
// 4. LoginResult(token, AuthResponse) 반환
// ⚠️ 이메일 없음과 비밀번호 불일치를 구분하지 마라 → 동일 메시지: "이메일 또는 비밀번호가 올바르지 않습니다"

public AuthResponse getMe(String email)
// findByEmail → AuthResponse 반환
```

LoginResult는 내부 DTO (token + AuthResponse를 함께 반환하기 위한 용도).

PasswordEncoder는 Step 4의 SecurityConfig에서 Bean으로 등록할 예정이므로, 이 Step에서는 테스트용으로 `new BCryptPasswordEncoder()`를 사용하거나, Bean 정의를 AuthService 내에 임시로 두어라.

### 3. AuthController

`backend/src/main/java/com/english/auth/AuthController.java`:

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/signup")
    // 201 Created + Set-Cookie: token=<jwt>; HttpOnly; Path=/api; SameSite=Lax; Max-Age=86400
    // ResponseEntity<AuthResponse>

    @PostMapping("/login")
    // 200 OK + Set-Cookie (동일)
    // ResponseEntity<AuthResponse>

    @PostMapping("/logout")
    // 204 No Content + Set-Cookie: token=; HttpOnly; Path=/api; Max-Age=0
    // ResponseEntity<Void>

    @GetMapping("/me")
    // 200 OK + AuthResponse
    // Cookie에서 JWT 추출 → email → getMe(email)
    // ⚠️ 이 단계에서는 @CookieValue("token")으로 직접 추출. Step 4에서 SecurityContext 방식으로 전환
}
```

Cookie 생성 헬퍼:
```java
private ResponseCookie createTokenCookie(String token, long maxAge)
// HttpOnly=true, Path="/api", SameSite=Lax, maxAge=파라미터
```

### 4. GlobalExceptionHandler 확장

기존 `GlobalExceptionHandler.java`에 추가:

- `AuthenticationException` (커스텀 또는 Spring Security의 것) → `401 UNAUTHORIZED`
- `ForbiddenException` (커스텀) → `403 FORBIDDEN`
- 기존 일반 Exception 핸들러의 `e.getMessage()` → `"서버 오류가 발생했습니다"` 로 마스킹 (스택 트레이스 미노출)

### 5. 테스트

TDD 원칙에 따라 **테스트를 먼저 작성**하라.

**AuthServiceTest** (`@ExtendWith(MockitoExtension.class)`):
- signup 성공: 비밀번호가 BCrypt로 해싱되었는지 검증
- signup 실패: 이메일 중복 → DuplicateException
- login 성공: JWT 토큰 반환
- login 실패: 이메일 없음 → AuthenticationException (메시지: "이메일 또는 비밀번호가 올바르지 않습니다")
- login 실패: 비밀번호 불일치 → AuthenticationException (동일 메시지)
- getMe 성공

**AuthControllerTest** (`MockMvcBuilders.standaloneSetup` 패턴, 기존 Controller 테스트와 동일):
- POST /api/auth/signup 성공 → 201 + Set-Cookie 헤더 존재
- POST /api/auth/signup 실패: 이메일 중복 → 409
- POST /api/auth/signup 실패: 비밀번호 8글자 미만 → 400
- POST /api/auth/login 성공 → 200 + Set-Cookie 헤더 존재
- POST /api/auth/login 실패 → 401
- POST /api/auth/logout → 204 + Max-Age=0 Cookie
- GET /api/auth/me 성공 → 200

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

- 전체 테스트 통과 (기존 + AuthServiceTest + AuthControllerTest)

## 검증 절차

1. `cd backend && ./gradlew test` 실행
2. AuthServiceTest 6개, AuthControllerTest 7개 테스트 통과 확인
3. 기존 테스트가 영향받지 않았는지 확인
4. GlobalExceptionHandler에 401/403 핸들러가 추가되었는지 확인
5. 일반 Exception 메시지가 마스킹되었는지 확인

## 금지사항

- SecurityFilterChain을 이 단계에서 수정하지 마라. 이유: Step 4에서 전면 교체한다.
- CorsConfig.java를 수정하지 마라. 이유: Step 4에서 SecurityConfig로 흡수한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

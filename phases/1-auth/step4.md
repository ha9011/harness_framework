# Step 4: SecurityConfig + JwtAuthenticationFilter

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 보안 체크리스트, CORS 설정
- `/docs/ADR.md` — ADR-007 (JWT + HttpOnly Cookie), ADR-008 (데이터 격리)
- `/docs/ARCHITECTURE.md` — 인증 흐름, 환경 설정 (CORS + JWT)
- `/backend/src/main/java/com/english/config/CorsConfig.java` — 현재 CORS 설정 (흡수 대상)
- `/backend/src/main/java/com/english/config/SecurityConfig.java` — Step 0에서 만든 임시 설정 (교체 대상)
- `/backend/src/main/java/com/english/auth/JwtProvider.java` — Step 2
- `/backend/src/main/java/com/english/auth/AuthController.java` — Step 3
- `/backend/src/main/java/com/english/auth/User.java` — Step 1
- `/backend/src/main/java/com/english/auth/UserRepository.java` — Step 1

## 작업

### 1. JwtAuthenticationFilter

`backend/src/main/java/com/english/auth/JwtAuthenticationFilter.java`:

- `OncePerRequestFilter` 상속
- 동작:
  1. 요청의 Cookie에서 "token" 이름의 값 추출
  2. token이 없거나 빈 문자열이면 → 필터 체인 계속 (인증 없이 진행, SecurityContext 미설정)
  3. `jwtProvider.validateToken(token)` → false면 → 필터 체인 계속
  4. `jwtProvider.getEmailFromToken(token)` → email 추출
  5. `userRepository.findByEmail(email)` → User 조회
  6. User를 Principal로 하는 `UsernamePasswordAuthenticationToken` 생성 → SecurityContext에 설정
  7. 필터 체인 계속

- `@Component`로 등록하지 마라. SecurityConfig에서 `new`로 생성하거나 Bean 등록하라.

### 2. SecurityConfig 전면 교체

기존 Step 0의 임시 SecurityConfig를 **완전히 교체**하라:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}");
                })
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS 설정 — CorsConfig.java의 내용을 여기로 흡수
    private CorsConfigurationSource corsConfigurationSource() {
        // allowedOrigins: "http://localhost:3000"
        // allowedMethods: GET, POST, PUT, PATCH, DELETE
        // allowedHeaders: "*"
        // allowCredentials: true  ← 필수! HttpOnly Cookie 전송
    }
}
```

### 3. CorsConfig.java 삭제

`backend/src/main/java/com/english/config/CorsConfig.java`를 삭제하라. CORS 설정은 SecurityConfig로 흡수되었다.

### 4. AuthController 수정

Step 3에서 `@CookieValue("token")`으로 직접 JWT를 추출했다면, `@AuthenticationPrincipal`로 전환하라:

```java
@GetMapping("/me")
public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal User user) {
    // SecurityContext에서 User 객체 직접 사용
    return ResponseEntity.ok(AuthResponse.from(user));
}
```

이를 위해 JwtAuthenticationFilter에서 Principal을 User 객체로 설정해야 한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

- 전체 테스트 통과
- 기존 단위 테스트는 standaloneSetup이므로 Security 필터 미적용 → 영향 없어야 함
- AuthControllerTest가 통과하는지 확인 (standaloneSetup이므로 Security 미적용)

## 검증 절차

1. `cd backend && ./gradlew test` 실행
2. CorsConfig.java가 삭제되었는지 확인
3. SecurityConfig에 CORS + CSRF + 세션 + 필터 + 인증 엔트리포인트가 설정되었는지 확인
4. allowCredentials(true)가 설정되었는지 확인
5. /api/auth/signup과 /api/auth/login만 permitAll인지 확인
6. 기존 테스트가 영향받지 않았는지 확인

## 금지사항

- 기존 Entity, Repository, Service, Controller를 수정하지 마라. 이유: Step 5에서 userId 적용을 일괄 처리한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

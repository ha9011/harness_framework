# Step 0: 의존성 및 인프라 설정

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/backend/build.gradle.kts`
- `/backend/src/main/resources/application.yml`

## 작업

### 1. build.gradle.kts에 의존성 추가

`backend/build.gradle.kts`의 dependencies 블록에 아래 의존성을 추가하라:

```kotlin
// Spring Security
implementation("org.springframework.boot:spring-boot-starter-security")

// JWT (jjwt)
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

// Security 테스트
testImplementation("org.springframework.security:spring-security-test")
```

기존 의존성은 건드리지 마라.

### 2. application.yml에 JWT 설정 추가

`backend/src/main/resources/application.yml`에 아래 설정을 추가하라:

```yaml
jwt:
  secret: ${JWT_SECRET:dev-secret-key-that-is-at-least-32-characters-long}
  expiration: 86400000
```

- `secret`: 환경변수 `JWT_SECRET`으로 주입. 미설정 시 개발용 기본값 사용.
- `expiration`: 24시간 (밀리초).
- 기존 설정(spring, gemini)은 건드리지 마라.

### 3. Spring Security 자동 설정 임시 비활성화

Spring Security를 추가하면 모든 엔드포인트가 즉시 보호된다. Step 4에서 SecurityConfig를 작성할 때까지 기존 기능이 정상 동작하도록 **임시 SecurityConfig**를 작성하라:

`backend/src/main/java/com/english/config/SecurityConfig.java`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

이것은 Step 4에서 완전한 설정으로 교체될 임시 설정이다.

## Acceptance Criteria

```bash
cd backend && ./gradlew build
```

- 컴파일 에러 없음
- 기존 테스트 전체 통과 (Security가 임시로 모든 요청 허용하므로)

## 검증 절차

1. `cd backend && ./gradlew build` 실행
2. 기존 단위 테스트가 모두 통과하는지 확인
3. application.yml에 jwt 설정이 추가되었는지 확인
4. SecurityConfig가 모든 요청을 permitAll하는지 확인

## 금지사항

- 기존 의존성을 변경하거나 삭제하지 마라. 이유: MVP에서 동작하는 기존 기능을 깨뜨릴 수 있다.
- CorsConfig.java를 이 단계에서 수정하지 마라. 이유: Step 4에서 SecurityConfig로 흡수한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

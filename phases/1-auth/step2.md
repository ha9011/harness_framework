# Step 2: JwtProvider (TDD)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — JWT 관련 기술적 제약사항
- `/docs/ADR.md` — ADR-007 (JWT + HttpOnly Cookie 인증)
- `/backend/src/main/resources/application.yml` — jwt.secret, jwt.expiration 설정
- `/backend/src/main/java/com/english/auth/User.java` — Step 1에서 생성된 User 엔티티

## 작업

### 1. JwtProvider

`backend/src/main/java/com/english/auth/JwtProvider.java` 생성:

- `@Component`
- `@Value("${jwt.secret}")` — secret key 주입
- `@Value("${jwt.expiration}")` — 만료 시간 주입

메서드 시그니처:

```java
public String generateToken(String email)
// email을 subject로 하는 JWT 생성. 만료 시간은 application.yml의 jwt.expiration 사용

public boolean validateToken(String token)
// 토큰 유효성 검증. 만료, 변조, 형식 오류 시 false 반환

public String getEmailFromToken(String token)
// 토큰에서 email(subject) 추출
```

- jjwt 라이브러리(io.jsonwebtoken) 사용
- SecretKey는 `Jwts.SIG.HS256.key()` 또는 `Keys.hmacShaKeyFor(secret.getBytes())` 사용
- 만료된 토큰, 변조된 토큰, null/빈 문자열에 대해 validateToken이 false 반환해야 함

### 2. 테스트

TDD 원칙에 따라 **테스트를 먼저 작성**하라.

`backend/src/test/java/com/english/auth/JwtProviderTest.java`:

테스트 케이스:
- 토큰 생성 → 이메일 추출 성공
- 토큰 생성 → 유효성 검증 성공 (true)
- 만료된 토큰 → 유효성 검증 실패 (false). 힌트: 만료 시간을 음수로 설정한 JwtProvider 인스턴스 사용
- 변조된 토큰 → 유효성 검증 실패 (false)
- null/빈 문자열 → 유효성 검증 실패 (false)

테스트에서 JwtProvider를 직접 생성하라 (Spring Context 불필요, `new JwtProvider(secret, expiration)` 방식). 이를 위해 JwtProvider 생성자가 secret과 expiration을 파라미터로 받도록 설계하라.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

- 전체 테스트 통과 (기존 + 신규 JwtProviderTest)

## 검증 절차

1. `cd backend && ./gradlew test` 실행
2. JwtProviderTest의 5개 테스트가 통과하는지 확인
3. 기존 테스트가 영향받지 않았는지 확인

## 금지사항

- Refresh Token을 구현하지 마라. 이유: ADR-007에서 Access Token만 사용으로 결정.
- JwtProvider에 Cookie 생성 로직을 포함하지 마라. 이유: Cookie 관련 로직은 Step 3 AuthController에서 처리한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

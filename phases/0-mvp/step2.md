# Step 2: backend-auth-service

## Step Contract

- Capability: authentication service logic and JWT token handling
- Layer: service
- Write Scope: `backend/src/main/java/com/english/auth/`, `backend/src/test/java/com/english/auth/`
- Out of Scope: HTTP controllers, SecurityConfig authorization rules, non-auth services, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*AuthServiceTest"` and `cd backend && ./gradlew test --tests "*JwtProviderTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/auth/User.java`
- `/backend/src/main/java/com/english/auth/UserRepository.java`

## 작업

회원가입, 로그인, JWT 발급/검증, 현재 사용자 조회 service 계층을 TDD로 구현한다.

필수 구현:
- `AuthService.signup(email, password, nickname)`은 비밀번호 8자 이상을 검증하고 BCrypt hash를 저장한다.
- 회원가입 성공 시 사용자 응답 DTO와 JWT 발급에 필요한 정보를 반환한다.
- 중복 email은 `DUPLICATE` 에러로 매핑 가능한 도메인 예외를 발생시킨다.
- `AuthService.login(email, password)`는 실패 시 email/password 구분 없는 동일한 인증 실패 예외를 발생시킨다.
- `JwtProvider`는 24시간 만료 token을 발급하고 검증한다.
- `JwtAuthenticationFilter`가 사용할 수 있는 token parsing 결과를 제공한다.
- 테스트 secret과 만료 시간은 test profile에서 제어 가능해야 한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*AuthServiceTest"
cd backend && ./gradlew test --tests "*JwtProviderTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 회원가입 시 BCrypt hash가 평문과 다르게 저장되는지 확인한다.
2. 로그인 실패 메시지가 email/password 구분 없이 동일한지 확인한다.
3. JWT subject가 user id를 담고 만료 검증이 동작하는지 확인한다.
4. Step 2를 `completed`로 표시하고 summary에 AuthService/JwtProvider 테스트 통과를 적는다.

## 금지사항

- Controller와 Cookie 설정을 구현하지 마라. 이유: HTTP 계약은 Step 3에서 다룬다.
- 인증 실패 원인을 사용자에게 노출하지 마라. 이유: PRD의 정보 노출 방지 요구사항이다.
- 실제 user_settings 생성을 구현하지 마라. 이유: settings service step에서 다룬다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

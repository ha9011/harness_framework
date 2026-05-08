# Step 1: backend-auth-domain

## Step Contract

- Capability: user authentication domain persistence
- Layer: domain
- Write Scope: `backend/src/main/java/com/english/auth/`, `backend/src/main/resources/db/migration/`, `backend/src/test/java/com/english/auth/`
- Out of Scope: AuthService business logic, SecurityConfig, controllers, non-auth domain tables, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*UserRepositoryTest"` and `cd backend && ./gradlew test --tests "*UserMigrationTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/phases/0-mvp/index.json`
- `/backend/build.gradle`
- `/backend/src/main/java/com/english/EnglishApplication.java`

## 작업

인증 도메인의 DB/Entity/Repository 기반을 TDD로 구현한다.

필수 구현:
- Flyway migration으로 `users` 테이블을 만든다.
- 컬럼은 `id`, `email`, `password`, `nickname`, `created_at`을 포함한다.
- `email`은 DB unique 제약을 가진다.
- `User` Entity와 `UserRepository`를 만든다.
- 인증 요청/응답 DTO를 준비한다: signup request, login request, current user response.
- password 컬럼은 BCrypt hash 저장을 전제로 길이를 충분히 둔다.
- 아직 실제 BCrypt hashing service는 구현하지 않는다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*UserMigrationTest"
cd backend && ./gradlew test --tests "*UserRepositoryTest"
cd backend && ./gradlew test
```

## 검증 절차

1. migration이 빈 PostgreSQL에 적용되는지 확인한다.
2. 같은 email 중복 저장이 실패하는지 Repository 테스트로 확인한다.
3. `UserRepository.findByEmail`이 동작하는지 확인한다.
4. Step 1을 `completed`로 표시하고 summary에 migration 파일명과 auth domain 파일명을 적는다.

## 금지사항

- 로그인/회원가입 service나 controller를 구현하지 마라. 이유: service/controller step과 분리한다.
- `user_settings`를 만들지 마라. 이유: 전체 스키마는 Step 4에서 다룬다.
- password를 평문으로 검증하는 로직을 만들지 마라. 이유: BCrypt 정책은 service step에서 고정한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

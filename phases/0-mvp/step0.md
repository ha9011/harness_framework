# Step 0: backend-scaffold

## Step Contract

- Capability: Spring Boot backend scaffold with PostgreSQL/Flyway/JPA/Security/Testcontainers test foundation
- Layer: domain
- Write Scope: `backend/`
- Out of Scope: authentication endpoints, learning domain entities beyond scaffold placeholders, Gemini client, frontend files, root scripts
- Critical Gates: `cd backend && ./gradlew test --tests "*ContextLoadsTest"` and `cd backend && ./gradlew test --tests "*HealthControllerTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/AGENTS.md`
- `/package.json`
- `/docker-compose.yml`

## 작업

`backend/`에 Java 21, Spring Boot, Gradle 기반 프로젝트를 생성한다.

필수 구성:
- package root는 `com.english`로 둔다.
- Spring Web MVC, Spring Data JPA, Spring Security, Validation, PostgreSQL driver, Flyway, jjwt 또는 동등한 JWT 라이브러리, Lombok 없이 구현한다.
- 테스트는 JUnit5, MockMvc, Testcontainers, AssertJ를 사용한다.
- `EnglishApplication`을 추가한다.
- `/api/health` GET endpoint를 추가해 `{ "status": "ok" }` JSON을 반환한다.
- 공통 에러 응답 DTO는 `{ "error": "...", "message": "..." }` 형태를 준비한다.
- `application.yml`은 로컬 PostgreSQL 기본값과 테스트용 Testcontainers 구성을 분리한다.
- Gradle wrapper를 포함해 `cd backend && ./gradlew test`가 동작해야 한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*ContextLoadsTest"
cd backend && ./gradlew test --tests "*HealthControllerTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Testcontainers 기반 PostgreSQL 연결과 Spring context load가 통과하는지 확인한다.
3. `/api/health`가 인증 없이 200 OK를 반환하는지 MockMvc로 확인한다.
4. `phases/0-mvp/index.json`의 Step 0을 `completed`로 바꾸고, summary에 생성된 backend scaffold와 핵심 테스트명을 적는다.

## 금지사항

- `frontend/`를 수정하지 마라. 이유: 이 step은 백엔드 scaffold만 담당한다.
- 실제 인증/단어/패턴 비즈니스 기능을 구현하지 마라. 이유: 후속 step의 scope를 침범한다.
- `TODO`, `stub`, 고정 더미 service로 completed 처리하지 마라. 이유: scaffold의 핵심은 실행 가능한 Spring/Testcontainers 기반이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

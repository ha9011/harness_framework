# Step 0: backend-docker

## Step Contract

- Capability: Backend Docker 이미지화(multi-stage Dockerfile) + Spring `prod` profile 정의(환경변수만 받는 fail-fast 구성)
- Layer: integration-hardening
- Write Scope: `backend/Dockerfile`, `backend/.dockerignore`, `backend/src/main/resources/application-prod.yml`
- Out of Scope: frontend 코드 일체, docker-compose, nginx, CI/CD workflow, 기존 `application.yml`/`application-local.yml` 수정
- Critical Gates: `docker build -t harness-backend ./backend` 성공 + `cd backend && ./gradlew test` 기존 테스트 전체 통과(회귀 없음) + `docker run --rm -e SPRING_PROFILES_ACTIVE=prod harness-backend 2>&1 | head -80` 실행 시 환경변수 부재로 부팅 실패(`Could not resolve placeholder ...` 또는 datasource 연결 실패 + 컨테이너 즉시 종료, exit code ≠ 0) — prod profile fail-fast 동작 증명

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Phase 8-deploy 전체 계획
- `/docs/ADR.md` — ADR-014, 015, 016, 017 (Nginx + Flexible SSL + GHCR SSH + prod profile fail-fast)
- `/docs/ARCHITECTURE.md` — 배포 토폴로지 + `application-prod.yml` 스펙
- `/backend/build.gradle.kts` — Java 17, Spring Boot 3.5.0, `logstash-logback-encoder:8.0` 의존성 확인
- `/backend/src/main/resources/application.yml` — 개발용 datasource/jwt/gemini 기본값(운영용 prod profile은 이 값들을 모두 환경변수로 받는다)
- `/backend/src/main/resources/application-local.yml` — 로컬 profile 형태 참고
- `/backend/src/main/resources/logback-spring.xml` — `<springProfile name="prod">` 블록 존재 확인. prod 활성 시 `./logs/`에 파일 출력하므로 Dockerfile에서 `/app/logs` 디렉토리 권한 필요

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 1. `backend/Dockerfile` (multi-stage)

베이스 이미지: `eclipse-temurin:17-jdk-jammy`(빌더) + `eclipse-temurin:17-jre-jammy`(런타임). Java 17 toolchain과 정합.

구조:

```dockerfile
# Stage 1: builder
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace
# 의존성 캐시 최대화: gradle 설정만 먼저 복사
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true
# 소스 복사 후 bootJar (테스트는 빌드에서 제외 — CI에서 별도 실행)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# Stage 2: runtime
FROM eclipse-temurin:17-jre-jammy
RUN useradd -r -u 1001 -g root spring \
  && mkdir -p /app/logs \
  && chown -R spring /app
USER spring
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
```

핵심 규칙:
- **non-root user**(spring, uid 1001) 사용
- `/app/logs` 디렉토리 사전 생성 + 권한 부여 (logback-spring.xml prod 블록이 이곳에 파일 출력)
- `JAVA_OPTS`를 env로 받아 `sh -c`로 풀어서 적용 (compose에서 `-Xms256m -Xmx512m` 주입 가능하게)
- `bootJar -x test` — Docker 빌드 단계에서는 테스트 제외. 테스트는 별도 `./gradlew test`로 회귀 검증.
- `gradlew` 실행 권한 누락 케이스 대비 `chmod +x` 명시

### 2. `backend/.dockerignore`

빌드 컨텍스트 크기 최소화 + 시크릿/로컬 설정 유입 방지:

```
.gradle
build
.idea
*.iml
.git
.gitignore
logs
out
src/test
src/main/resources/application-local.yml
HELP.md
README.md
*.log
```

특히 `application-local.yml`은 실제 Gemini API 키가 평문 저장되어 있으므로 **반드시 제외**.

### 3. `backend/src/main/resources/application-prod.yml` (신규)

기본값 없이 환경변수만 받는다(fail-fast). 누락 시 부팅 실패가 발생해야 dev 시크릿이 운영에 새는 사고를 막을 수 있다.

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false

server:
  forward-headers-strategy: framework
  tomcat:
    remoteip:
      remote-ip-header: X-Forwarded-For
      protocol-header: X-Forwarded-Proto

gemini:
  api-key: ${GEMINI_API_KEY}
  model: ${GEMINI_MODEL:gemini-2.5-flash}

jwt:
  secret: ${JWT_SECRET}
  expiration: 604800000
```

핵심 규칙:
- `${SPRING_DATASOURCE_URL}`, `${SPRING_DATASOURCE_USERNAME}`, `${SPRING_DATASOURCE_PASSWORD}`, `${GEMINI_API_KEY}`, `${JWT_SECRET}`는 **기본값 없음** — 누락 시 부팅 실패
- `${GEMINI_MODEL:gemini-2.5-flash}`만 안전한 기본값 허용
- `forward-headers-strategy: framework` 필수 — Nginx가 보낸 `X-Forwarded-Proto: https`를 신뢰해 redirect 응답이 https로 나가도록
- `ddl-auto: validate` — 운영 DB는 사전 복원된 스키마와 정합해야 함
- `format_sql: false` — JSON 로그 한 줄 가독성 유지

### 4. 빌드 검증

빌드가 의존성 다운로드로 오래 걸릴 수 있으므로 시간 여유 두기. 캐시가 도는 두 번째 빌드는 빠르다.

```bash
docker build -t harness-backend ./backend
```

성공 후 이미지 확인:
```bash
docker images harness-backend
docker run --rm harness-backend java -version
# openjdk version "17..." 출력 확인
```

prod profile fail-fast 검증:
```bash
docker run --rm -e SPRING_PROFILES_ACTIVE=prod harness-backend 2>&1 | head -80
# 출력에 "Could not resolve placeholder" 또는 datasource 연결 실패 메시지가 있고
# 컨테이너가 즉시 종료(exit code != 0)되어야 한다 — 이게 정상.
```

## Acceptance Criteria

```bash
docker build -t harness-backend ./backend
cd backend && ./gradlew test
docker run --rm -e SPRING_PROFILES_ACTIVE=prod harness-backend 2>&1 | head -80 || true
docker images harness-backend
```

- 빌드 성공
- 기존 테스트 전체 통과 (회귀 없음)
- prod profile 컨테이너 실행 시 환경변수 부재로 부팅 실패(이건 정상 동작이므로 `|| true`로 종료코드 무시하되 로그에 placeholder/connection 실패 메시지가 보여야 함)
- 이미지가 docker images에 등록되어 있음

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - `docker build`가 단순 통과가 아니라 multi-stage가 의도대로 동작했는지(이미지 크기가 JRE 베이스 수준인지 — 보통 400~500MB대) 확인
   - prod profile fail-fast가 실제 발동하는지 (출력에 placeholder/connection 에러 메시지 존재)
   - 기존 단위 테스트가 prod profile 추가로 깨지지 않음을 보장
3. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가?
   - ADR-017(prod profile fail-fast) 결정을 위반하지 않았는가? — 기본값 추가 금지
   - CLAUDE.md CRITICAL 규칙(backend/ 폴더 외부 작성 금지)을 위반하지 않았는가?
   - Step Contract의 Write Scope 밖(frontend/, 루트 등)을 수정하지 않았는가?
   - frontend 코드를 동시에 수정하지 않았는가? (이 step은 backend만)
4. 결과에 따라 `phases/8-deploy/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "구체적 사유"`

## 금지사항

- frontend 코드(`frontend/`)를 수정하지 마라. 이유: 이 step은 backend 한정. 룰 6 위반.
- `application.yml`, `application-local.yml`, `logback-spring.xml`을 수정하지 마라. 이유: 기존 dev/test 동작이 깨진다.
- `application-prod.yml`에 dev 기본값(`dev-secret-...`, `app1234`, 등)을 넣지 마라. 이유: dev 시크릿이 운영에 새는 ADR-017 위반.
- Dockerfile에서 `USER root` 그대로 유지하지 마라. 이유: 보안 — non-root 강제.
- `application-local.yml`이 빌드 컨텍스트에 들어가지 않도록 `.dockerignore`에서 반드시 제외하라. 이유: 평문 Gemini 키 노출.
- `TODO`, `not implemented`, `stub`, 빈 객체로 핵심 기능을 대체하고 completed 처리하지 마라.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: 회귀를 숨긴다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

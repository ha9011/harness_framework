# Step 0: logback-config

## Step Contract

- Capability: logback-spring.xml 기반 프로파일별 로깅 설정 + application.yml 정리
- Layer: service
- Write Scope: `backend/build.gradle.kts`, `backend/src/main/resources/logback-spring.xml`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-local.yml`
- Out of Scope: Java 소스 코드 수정 금지. MdcLoggingFilter, GlobalExceptionHandler는 Step 1에서 처리. 프론트엔드 수정 금지. Actuator/Prometheus 추가 금지
- Critical Gates: `cd backend && ./gradlew test` 전체 통과 (로깅 설정 변경이 기존 테스트를 깨뜨리지 않음을 증명)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md` (ADR-011: logback-spring.xml 기반 로깅 통합 관리)
- `backend/build.gradle.kts` (현재 의존성 확인)
- `backend/src/main/resources/application.yml` (현재 설정 확인)
- `backend/src/main/resources/application-local.yml` (현재 logging 설정 확인)
- `backend/src/test/resources/application.yml` (테스트 설정 확인 — 수정하지 않을 것)

## 작업

### 1. build.gradle.kts — logstash-logback-encoder 의존성 추가

`dependencies` 블록에 아래 의존성 1개를 추가한다:

```kotlin
implementation("net.logstash.logback:logstash-logback-encoder:8.0")
```

- 이유: prod 환경에서 JSON 포맷 로그 출력(ELK/CloudWatch 수집용)에 필요
- 기존 의존성은 건드리지 않는다

### 2. logback-spring.xml 생성

`backend/src/main/resources/logback-spring.xml`을 새로 생성한다.

**공통 설정:**
- LOG_PATH: `./logs`
- 콘솔 패턴: `%d{HH:mm:ss.SSS} %highlight(%-5level) [%blue(%8.8X{traceId:-________})] %cyan(%-40.40logger{39}) : %msg%n`
  - `%8.8X{traceId:-________}`: traceId가 없을 때 밑줄 8개로 자리 유지 (로그 정렬 보호)
  - `%-40.40logger{39}`: 로거명 40자 고정폭 (정렬)
- 파일 패턴: `%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{traceId:-________}] %-40.40logger{39} : %msg%n`

**`<springProfile name="local">`:**
- CONSOLE appender: 컬러 콘솔 (위 콘솔 패턴 사용)
- root level: INFO
- `org.hibernate.SQL`: DEBUG (SQL 쿼리 출력)
- `org.hibernate.orm.jdbc.bind`: TRACE (바인드 파라미터 출력)
- `org.springframework.security`: WARN (Security 노이즈 억제)

**`<springProfile name="prod">`:**
- JSON_CONSOLE appender: `LogstashEncoder` 사용
  - `<includeMdcKeyName>traceId</includeMdcKeyName>`
  - `<timeZone>Asia/Seoul</timeZone>`
  - `<shortenedLoggerNameLength>36</shortenedLoggerNameLength>`
- FILE appender: `RollingFileAppender`
  - file: `${LOG_PATH}/app.log`
  - `SizeAndTimeBasedRollingPolicy`: `app.%d{yyyy-MM-dd}.%i.log.gz`, maxFileSize 100MB, maxHistory 30, totalSizeCap 3GB
  - 위 파일 패턴 사용
- ERROR_FILE appender: `RollingFileAppender`
  - file: `${LOG_PATH}/error.log`
  - `ThresholdFilter` level ERROR
  - `SizeAndTimeBasedRollingPolicy`: `error.%d{yyyy-MM-dd}.%i.log.gz`, maxFileSize 50MB, maxHistory 90, totalSizeCap 1GB
  - 위 파일 패턴 사용
- root level: WARN, appenders: JSON_CONSOLE, FILE, ERROR_FILE
- `com.english`: INFO
- `org.hibernate.SQL`: DEBUG (사용자 요청: prod에서도 SQL 로그 필요)
- `org.hibernate.orm.jdbc.bind`: TRACE (사용자 요청: prod에서도 파라미터 필요)

**`<springProfile name="default">`:**
- CONSOLE_DEFAULT appender: 컬러 콘솔 (위 콘솔 패턴 사용)
- root level: INFO
- 이유: 테스트 환경 등 프로파일 미지정 시 기본 동작. SQL 로그 OFF (테스트 노이즈 방지)

### 3. application.yml — show-sql 제거

현재:
```yaml
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

변경 후:
```yaml
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
```

- `show-sql: true` 행을 제거한다
- 이유: `show-sql`은 `System.out.println`으로 출력하여 logback을 경유하지 않음 → traceId 미포함, 파일 롤링 미적용. `org.hibernate.SQL=DEBUG`가 logback 경유로 동일한 SQL을 출력하므로 완전 대체
- `format_sql: true`는 유지한다. 이유: logback 경유 SQL의 포맷팅에도 영향을 줌

### 4. application-local.yml — logging 섹션 제거

현재:
```yaml
logging:
  level:
    org.hibernate.orm.jdbc.bind: TRACE
```

이 `logging` 섹션 전체를 제거한다.
- 이유: logback-spring.xml의 `<springProfile name="local">`에서 동일한 설정을 관리. 두 곳에 분산되면 혼란
- gemini 설정은 그대로 유지한다

## Acceptance Criteria

```bash
cd backend && ./gradlew test
# 전체 테스트 통과 확인. 로깅 설정 변경이 기존 테스트를 깨뜨리지 않음을 증명

cd backend && ./gradlew dependencies --configuration runtimeClasspath | grep logstash
# logstash-logback-encoder 의존성이 포함되어 있음을 확인
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates 확인:
   - `./gradlew test` 전체 통과 → 기존 비즈니스 로직에 영향 없음 증명
   - `logstash-logback-encoder` 의존성 존재 확인
3. 아키텍처 체크리스트:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가? → logback-spring.xml이 `src/main/resources/`에 위치
   - ADR-011 결정을 따르는가? → show-sql 제거, logback-spring.xml 통합 관리
   - CLAUDE.md CRITICAL 규칙 위반 없는가? → 백엔드 코드만 수정, TDD 불필요 (설정 파일만 변경)
   - Write Scope 밖을 수정하지 않았는가? → Java 소스, 프론트엔드, 테스트 yml 미수정
4. 결과에 따라 index.json 업데이트

## 금지사항

- Java 소스 코드(`.java`)를 수정하지 마라. 이유: MdcLoggingFilter, GlobalExceptionHandler는 Step 1에서 처리
- 테스트 `application.yml`(`src/test/resources/application.yml`)을 수정하지 마라. 이유: 테스트 설정 변경은 범위 밖
- `spring-boot-starter-actuator`, `micrometer-registry-prometheus` 등 모니터링 의존성을 추가하지 마라. 이유: 모니터링은 별도 작업
- `spring-boot-starter-aop` 의존성을 추가하지 마라. 이유: AOP 미사용 (ADR-012)
- logback-spring.xml에서 `<springProfile>` 외의 방식(예: `logback-local.xml` 분리)을 사용하지 마라. 이유: 단일 파일 관리 원칙 (ADR-011)

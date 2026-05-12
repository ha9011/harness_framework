# PLAN: 백엔드 실무 로깅 구성

## 작업 목표
쿼리 로그(SQL+파라미터), 요청/응답 로그(traceId), 에러 로그(소스 위치 추적)를 실무 수준으로 구성한다. 로컬은 콘솔, prod는 파일 롤링+JSON.

## 구현할 기능
1. MDC traceId 필터로 요청 추적 (UUID 8자리, 요청 시작/완료 로깅)
2. logback-spring.xml 프로파일별 로깅 (local: 컬러 콘솔, prod: JSON+파일 롤링+에러 파일 분리)
3. SQL 쿼리 + 바인드 파라미터 로깅 (로컬/prod 모두)
4. GlobalExceptionHandler 에러 로깅 개선 (4xx WARN+스택트레이스, 5xx ERROR+스택트레이스)
5. application.yml 정리 (show-sql 제거, logging 섹션 logback으로 이관)

## 기술적 제약사항
- AOP 미사용 — Filter로 충분. AOP는 메서드 단위라 Security 필터 이전 로그를 놓침
- Actuator/Prometheus 제외 — 별도 작업
- 기존 @Slf4j 클래스의 로그 코드 수정 안 함
- logstash-logback-encoder 의존성 1개만 추가

## 테스트 전략
- 기존 테스트 영향: 없음 (로깅은 비즈니스 로직에 영향 없음)
- 테스트 환경: profile 미지정 → logback default 프로파일 (INFO 콘솔, SQL OFF). MdcLoggingFilter가 @Component로 @SpringBootTest에서도 동작하지만 기능에 해 없음
- 테스트 application.yml의 `show-sql: false`: main에서 show-sql 제거 후 의미없는 설정이 되지만, 제거하지 않음 (테스트 yml 변경은 범위 밖)
- 신규 테스트: 해당 없음 (로깅 설정은 통합 테스트로 수동 검증)
- 테스트 수정이 필요한 Step: 없음

## Phase/Step 초안

### Step 0: 의존성 + logback-spring.xml + application.yml 정리
- 작업:
  - `backend/build.gradle.kts` 수정 — `logstash-logback-encoder` 의존성 추가
  - `backend/src/main/resources/logback-spring.xml` 생성 — local/prod/default 프로파일별 로깅 설정
    - local: 컬러 콘솔, SQL(DEBUG) + 바인드 파라미터(TRACE), Security 노이즈 억제(WARN)
    - prod: JSON 콘솔(LogstashEncoder) + 전체 로그 파일(30일, 100MB, 3GB) + 에러 파일(90일) + SQL/파라미터 출력
    - default: 컬러 콘솔 INFO
  - `backend/src/main/resources/application.yml` 수정 — `show-sql: true` 제거 (logback의 org.hibernate.SQL로 대체)
  - `backend/src/main/resources/application-local.yml` 수정 — `logging` 섹션 제거 (logback-spring.xml에서 관리)
- 산출물: local 프로파일에서 컬러 콘솔 + SQL + 파라미터 출력 확인
- 수정 파일: `build.gradle.kts`, `application.yml`, `application-local.yml`
- 생성 파일: `logback-spring.xml`

### Step 1: MDC traceId 필터 + GlobalExceptionHandler 로깅 + .gitignore
- 작업:
  - `backend/src/main/java/com/english/config/MdcLoggingFilter.java` 생성
    - OncePerRequestFilter + @Order(HIGHEST_PRECEDENCE) — Security 필터보다 먼저 실행
    - 요청마다 UUID 8자리 traceId → MDC 세팅
    - 요청 시작 로그: `>>> GET /api/words started`
    - 응답 완료 로그: `<<< GET /api/words 200 (59ms)` (status + 처리시간)
    - finally에서 MDC.clear() (스레드풀 누수 방지)
    - /favicon.ico 제외
  - `backend/src/main/java/com/english/config/GlobalExceptionHandler.java` 수정
    - 4xx 핸들러 9개: `log.warn("라벨: {}", e.getMessage(), e)` 추가 (스택트레이스 포함)
    - 5xx GeminiException: `log.error("Gemini API error: {}", e.getMessage(), e)` 추가
    - 기존 handleGeneral의 `log.error`는 유지
  - `backend/.gitignore` 수정 — `logs/` 추가 (prod 실행 시 backend/ 기준 생성)
- 산출물: 요청마다 traceId 부여, 에러 시 소스 위치(클래스+라인번호) 추적 가능
- 생성 파일: `MdcLoggingFilter.java`
- 수정 파일: `GlobalExceptionHandler.java`, `backend/.gitignore`

## 검증 방법
1. `cd backend && ./gradlew test` — 기존 테스트 전부 통과 확인
2. `cd backend && ./gradlew bootRun` (local 프로파일) 후 API 호출:
   - traceId가 모든 로그에 일관되게 붙는지 확인
   - SQL 쿼리가 포맷팅되어 출력되는지 확인
   - 바인드 파라미터(`?`에 실제 값)가 표시되는지 확인
3. 존재하지 않는 리소스 조회(예: `GET /api/words/999`):
   - WARN 레벨 로그 출력 확인
   - 스택트레이스에 `WordService.java:XX` 라인번호 확인
   - traceId로 요청 시작~완료 전체 추적 가능 확인
4. 잘못된 Gemini API 키로 예문 생성 시도:
   - ERROR 레벨 로그 출력 확인
   - 스택트레이스에 `GeminiClient.java:XX` 라인번호 확인

## 엣지케이스 / 사이드이펙트 체크리스트

| # | 항목 | 상태 | 설명 |
|---|------|------|------|
| 1 | Security 401 로깅 | 인지함 | SecurityConfig authenticationEntryPoint 직접 응답 → GlobalExceptionHandler 미경유. MdcLoggingFilter 완료 로그에서 401 status는 확인 가능 |
| 2 | 테스트 환경 영향 | 해 없음 | default 프로파일 적용 (INFO, SQL OFF). MdcLoggingFilter 동작하나 기능 무해 |
| 3 | test yml show-sql: false | 남겨둠 | 의미없는 설정이 되지만 테스트 yml 변경은 범위 밖 |
| 4 | prod SQL 로그 용량 | 롤링으로 관리 | 벌크 300건 → SQL 대량 발생. 100MB/파일, 30일, 3GB 상한으로 제어 |
| 5 | format_sql 유지 | 의도적 | show-sql 제거해도 logback 경유 SQL 포맷팅에 여전히 필요 |
| 6 | MDC 스레드 안전성 | 문제 없음 | Spring MVC 스레드-퍼-리퀘스트 모델. finally에서 MDC.clear()로 누수 방지 |
| 7 | 필터 실행 순서 | 검증함 | MdcLoggingFilter(@Order HIGHEST_PRECEDENCE, 서블릿 레벨) → Spring Security 체인 → JwtAuthenticationFilter |

## 미결 사항
- 없음

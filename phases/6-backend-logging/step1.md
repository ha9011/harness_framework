# Step 1: trace-filter-error-logging

## Step Contract

- Capability: MDC traceId 요청 추적 필터 + GlobalExceptionHandler 에러 로깅 개선
- Layer: service
- Write Scope: `backend/src/main/java/com/english/config/MdcLoggingFilter.java` (신규), `backend/src/main/java/com/english/config/GlobalExceptionHandler.java`, `backend/.gitignore`
- Out of Scope: logback-spring.xml 수정 금지 (Step 0에서 완료). build.gradle.kts 수정 금지. application.yml 수정 금지. 프론트엔드 수정 금지. 기존 @Slf4j 클래스(GeminiClient, WordService, PatternService, GenerateService)의 로그 코드 수정 금지
- Critical Gates: `cd backend && ./gradlew test` 전체 통과 + MdcLoggingFilter가 @Component로 컨텍스트에 등록되어 테스트에서도 동작함을 확인

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` (엣지케이스/사이드이펙트 체크리스트 7항목 반드시 확인)
- `/docs/ARCHITECTURE.md` (데이터 흐름, 인증 흐름, 에러 처리 흐름의 MdcLoggingFilter 위치 확인)
- `/docs/ADR.md` (ADR-012: MDC traceId + Filter 기반, ADR-013: 4xx WARN/5xx ERROR)
- `backend/src/main/java/com/english/config/GlobalExceptionHandler.java` (현재 핸들러 구조 확인)
- `backend/src/main/java/com/english/config/SecurityConfig.java` (필터 순서 확인)
- `backend/src/main/java/com/english/auth/JwtAuthenticationFilter.java` (기존 필터 구조 참고)
- `backend/src/main/resources/logback-spring.xml` (Step 0에서 생성된 설정 확인)
- `backend/.gitignore` (현재 내용 확인)

## 작업

### 1. MdcLoggingFilter.java 생성

`backend/src/main/java/com/english/config/MdcLoggingFilter.java`를 새로 생성한다.

**클래스 시그니처:**
```java
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter
```

**핵심 규칙:**

1. **traceId 생성**: `UUID.randomUUID().toString().substring(0, 8)` — 8자리로 축약 (가독성 + 유일성 균형)
2. **MDC 세팅**: `MDC.put("traceId", traceId)` — logback-spring.xml의 `%X{traceId}` 패턴이 이 값을 참조
3. **요청 시작 로그**: `log.info(">>> {} {} started", request.getMethod(), request.getRequestURI())`
4. **응답 완료 로그**: `log.info("<<< {} {} {} ({}ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), duration)`
   - `duration = System.currentTimeMillis() - startTime`
5. **MDC 정리**: `finally` 블록에서 반드시 `MDC.clear()` — 스레드풀 환경에서 이전 요청의 traceId가 남는 버그 방지
6. **shouldNotFilter**: `/favicon.ico` 경로 제외 (노이즈 방지)

**필터 실행 순서 설명 (SecurityConfig 수정 불필요):**
- `@Component` + `@Order(HIGHEST_PRECEDENCE)` → 서블릿 컨테이너 레벨에서 등록
- Spring Security의 `DelegatingFilterProxy`(기본 order -100)보다 먼저 실행
- JwtAuthenticationFilter는 Security 체인 내부에 `addFilterBefore`로 등록되어 있으므로 자연스럽게 그 뒤에 실행
- 결과 순서: MdcLoggingFilter → Spring Security 체인 → JwtAuthenticationFilter → DispatcherServlet → Controller

**엣지케이스 (PLAN.md 체크리스트 참조):**
- Security 401 (JWT 없음/만료): SecurityConfig의 authenticationEntryPoint에서 직접 응답 → GlobalExceptionHandler를 거치지 않음 → WARN 스택트레이스 없음. 단, MdcLoggingFilter의 완료 로그에서 401 status + 처리시간은 확인 가능. 이것은 정상 동작임
- 테스트 환경: MdcLoggingFilter가 @Component로 @SpringBootTest에서도 동작하지만 기능에 해 없음

### 2. GlobalExceptionHandler.java — 에러 로깅 추가

기존 `GlobalExceptionHandler.java`를 수정한다. **기존 응답 구조(ErrorResponse, HttpStatus)는 절대 변경하지 않는다.** 각 핸들러의 return문 앞에 로깅 코드 1줄만 추가한다.

**4xx 핸들러 9개 — `log.warn` + 스택트레이스:**

```java
// 각 핸들러에 return문 앞에 1줄 추가. 패턴:
log.warn("라벨: {}", e.getMessage(), e);
//                                    ↑ 세 번째 인자 e → Slf4j가 자동으로 스택트레이스 출력
//                                      어떤 클래스, 몇 번째 줄에서 예외 발생했는지 추적 가능
```

| 핸들러 | 로그 코드 |
|--------|-----------|
| handleDuplicate | `log.warn("Duplicate: {}", e.getMessage(), e);` |
| handleEmptyRequest | `log.warn("Empty request: {}", e.getMessage(), e);` |
| handleNoWords | `log.warn("No words: {}", e.getMessage(), e);` |
| handleNoPatterns | `log.warn("No patterns: {}", e.getMessage(), e);` |
| handleInvalidImage | `log.warn("Invalid image: {}", e.getMessage(), e);` |
| handleNotFound | `log.warn("Not found: {}", e.getMessage(), e);` |
| handleValidation | `log.warn("Validation failed: {}", message);` (스택트레이스 불필요 — Spring이 던지는 예외라 소스 위치가 무의미) |
| handleAuthentication | `log.warn("Authentication failed: {}", e.getMessage(), e);` |
| handleForbidden | `log.warn("Forbidden: {}", e.getMessage(), e);` |

**5xx 핸들러 — `log.error` + 스택트레이스:**

| 핸들러 | 로그 코드 |
|--------|-----------|
| handleGemini | `log.error("Gemini API error: {}", e.getMessage(), e);` |
| handleGeneral | 이미 `log.error("Unhandled exception", e);`가 있음 → **수정하지 않는다** |

**handleValidation 특이사항:**
- MethodArgumentNotValidException은 Spring이 자동으로 던지므로 스택트레이스가 Spring 프레임워크 내부만 가리킴 → 스택트레이스 대신 메시지만 로깅
- 이미 `message` 변수를 만들어서 `e.getBindingResult()`에서 추출하고 있으므로 그 값을 사용

### 3. backend/.gitignore — logs/ 추가

`backend/.gitignore` 파일 맨 끝에 아래 2줄을 추가한다:

```
# Application logs
logs/
```

- 이유: prod 실행 시 `backend/logs/` 디렉토리에 로그 파일이 생성됨. git에 올라가지 않도록 방지

## Acceptance Criteria

```bash
cd backend && ./gradlew test
# 전체 테스트 통과 확인. MdcLoggingFilter 추가와 GlobalExceptionHandler 로깅 변경이 기존 테스트를 깨뜨리지 않음을 증명

# MdcLoggingFilter 존재 확인
test -f backend/src/main/java/com/english/config/MdcLoggingFilter.java && echo "OK"

# GlobalExceptionHandler에 log.warn/log.error 호출이 추가되었는지 확인
grep -c "log.warn\|log.error" backend/src/main/java/com/english/config/GlobalExceptionHandler.java
# 기대값: 11 이상 (warn 9개 + error 2개)

# .gitignore에 logs/ 포함 확인
grep "logs/" backend/.gitignore && echo "OK"
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates 확인:
   - `./gradlew test` 전체 통과
   - MdcLoggingFilter.java 파일 존재
   - GlobalExceptionHandler에 log.warn 9개 + log.error 2개 확인
   - backend/.gitignore에 logs/ 포함
3. 아키텍처 체크리스트:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가? → MdcLoggingFilter가 `config/` 패키지에 위치
   - ADR-012(Filter 기반 traceId), ADR-013(4xx WARN/5xx ERROR) 결정을 따르는가?
   - CLAUDE.md CRITICAL 규칙 위반 없는가? → 백엔드 코드만 수정
   - Write Scope 밖을 수정하지 않았는가? → logback-spring.xml, build.gradle.kts, application.yml 미수정
4. 결과에 따라 index.json 업데이트

## 금지사항

- `logback-spring.xml`을 수정하지 마라. 이유: Step 0에서 완료된 설정
- `build.gradle.kts`를 수정하지 마라. 이유: Step 0에서 완료된 의존성
- `application.yml`, `application-local.yml`을 수정하지 마라. 이유: Step 0에서 완료된 설정 정리
- `SecurityConfig.java`를 수정하지 마라. 이유: MdcLoggingFilter는 @Component + @Order로 서블릿 레벨 등록. Security 체인에 추가할 필요 없음
- 기존 @Slf4j 클래스(GeminiClient, WordService, PatternService, GenerateService)의 로그 코드를 수정하지 마라. 이유: 기존 로그 코드는 있는 그대로 유지
- GlobalExceptionHandler의 기존 응답 구조(ErrorResponse 필드, HttpStatus 코드)를 변경하지 마라. 이유: 프론트엔드가 기존 응답 포맷에 의존
- `handleGeneral` 메서드의 기존 `log.error("Unhandled exception", e);`를 수정하지 마라. 이유: 이미 올바르게 동작 중
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지

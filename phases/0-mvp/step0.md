# Step 0: 프로젝트 세팅 + 공통 인프라

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/docs/UI_GUIDE.md`

## 작업

### 1. Docker Compose (PostgreSQL 16)

루트에 `docker-compose.yml` 생성:

```yaml
# DB: english_app, user: app, password: app1234, port: 5432
```

### 2. Spring Boot 백엔드 초기화

`backend/` 디렉토리에 Spring Boot 프로젝트 생성:

- **Gradle (Kotlin DSL)** 빌드 시스템
- 의존성: Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Validation, Lombok
- 테스트 의존성: Spring Boot Test, TestContainers (PostgreSQL)
- Java 17+
- 패키지 루트: `com.english`
- `application.yml` 설정:
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/english_app
      username: app
      password: app1234
    jpa:
      hibernate:
        ddl-auto: update
      show-sql: true
  gemini:
    api-key: ${GEMINI_API_KEY}
  ```

### 3. Next.js 프론트엔드 초기화

`frontend/` 디렉토리에 Next.js 프로젝트 생성:

- App Router, TypeScript, Tailwind CSS
- ESLint 설정
- `lib/api.ts` 기본 fetch 래퍼 생성 (baseURL: `http://localhost:8080/api`)

### 4. CORS 설정

`backend/src/main/java/com/english/config/CorsConfig.java`:

```java
// WebMvcConfigurer 구현
// 허용 origin: http://localhost:3000
// 허용 methods: GET, POST, PUT, PATCH, DELETE
```

### 5. GlobalExceptionHandler

`backend/src/main/java/com/english/config/GlobalExceptionHandler.java`:

- `@RestControllerAdvice`
- 커스텀 예외 클래스 생성:
  - `DuplicateException` → 409
  - `EmptyRequestException` → 400
  - `NoWordsException` → 400
  - `NoPatternsException` → 400
  - `InvalidImageException` → 400
  - `GeminiException` → 502
- 에러 응답 형식: `{ "code": "DUPLICATE", "message": "..." }`

### 6. GeminiClient

`backend/src/main/java/com/english/config/GeminiClient.java`:

- `@Component`
- 시그니처:
  ```java
  public <T> T generateContent(String prompt, Class<T> responseType)
  public <T> T generateContentWithImage(byte[] imageData, String mimeType, String prompt, Class<T> responseType)
  ```
- 재시도 로직: 총 3회 시도 (즉시 → 1초 후 → 3초 후). 3회 모두 실패 시 `GeminiException` throw
- `response_mime_type="application/json"` + `response_schema` 설정 필수
- Gemini REST API 직접 호출 (SDK 아닌 RestTemplate/WebClient 사용)

### 7. GeminiClient 테스트

`backend/src/test/java/com/english/config/GeminiClientTest.java`:

- 재시도 로직 테스트 (MockWebServer 또는 Mock 사용)
- 1회 실패 후 성공 케이스
- 3회 모두 실패 시 GeminiException throw 케이스
- JSON 파싱 테스트

### 8. StudyRecordService 기본 구조

`backend/src/main/java/com/english/study/`:

- `StudyRecord` Entity: id, dayNumber, createdAt
- `StudyRecordItem` Entity: id, studyRecordId, itemType(WORD/PATTERN), itemId
  - UNIQUE(study_record_id, item_type, item_id)
- `StudyRecordRepository`
- `StudyRecordService`:
  ```java
  // 오늘 날짜 레코드가 없으면 생성, 있으면 반환
  public StudyRecord getOrCreateTodayRecord()
  // 학습 기록에 아이템 추가
  public void addItem(StudyRecord record, String itemType, Long itemId)
  ```
- `day_number` = `SELECT COALESCE(MAX(day_number), 0) + 1 FROM study_records`

### 9. ReviewItem 기본 Entity

`backend/src/main/java/com/english/review/`:

- `ReviewItem` Entity: id, itemType(WORD/PATTERN/SENTENCE), itemId, direction(RECOGNITION/RECALL), deleted, nextReviewDate, intervalDays, easeFactor, reviewCount, lastResult, lastReviewedAt
  - UNIQUE(item_type, item_id, direction)
  - 초기값: intervalDays=1, easeFactor=2.5, reviewCount=0, deleted=false, nextReviewDate=오늘
- `ReviewItemRepository`
- `ReviewItemService`:
  ```java
  // 단어/패턴 등록 시 RECOGNITION + RECALL 2개 생성
  public void createWordReviewItems(Long wordId)
  public void createPatternReviewItems(Long patternId)
  // 예문 등록 시 RECOGNITION만 1개 생성
  public void createSentenceReviewItem(Long sentenceId)
  ```

## Acceptance Criteria

```bash
docker compose up -d              # PostgreSQL 컨테이너 기동
cd backend && ./gradlew build     # 컴파일 + 테스트 통과
cd frontend && npm run build      # 빌드 성공
cd frontend && npm run lint       # ESLint 통과
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가?
   - ADR 기술 스택을 벗어나지 않았는가?
   - CLAUDE.md CRITICAL 규칙을 위반하지 않았는가?
3. 결과에 따라 `phases/0-mvp/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 비즈니스 로직(단어 CRUD, 패턴 CRUD 등)을 구현하지 마라. 이유: Step 1~5에서 TDD로 진행한다.
- ReviewItem에 SM-2 로직을 구현하지 마라. 이유: Step 4에서 구현한다.
- StudyRecordService에 대시보드 통계 로직을 넣지 마라. 이유: Step 5에서 구현한다.
- 프론트엔드에 페이지 컴포넌트를 만들지 마라. 이유: 각 Step에서 해당 페이지를 만든다. 여기서는 초기화 + lib/api.ts만.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

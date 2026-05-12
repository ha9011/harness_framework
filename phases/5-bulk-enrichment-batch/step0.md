# Step 0: bulk-enrichment-dto

## Step Contract

- Capability: 배열 기반 벌크 보강 DTO와 프롬프트 빌더 메서드 생성
- Layer: service
- Write Scope: `backend/src/main/java/com/english/word/`, `backend/src/test/java/com/english/word/`
- Out of Scope: `WordService.bulkCreate` 메서드 수정, GeminiClient 변경, frontend 코드, controller 코드
- Critical Gates: `cd backend && ./gradlew test --tests "com.english.word.WordServiceTest"` — 기존 테스트 전체 통과 + 신규 프롬프트 빌더 테스트 통과

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md` — ADR-010 (벌크 보강 배치 전략)
- `backend/src/main/java/com/english/word/WordService.java` — 현재 `buildEnrichmentPrompt` 메서드 (단건 프롬프트)
- `backend/src/main/java/com/english/word/WordEnrichment.java` — 현재 단건 응답 DTO
- `backend/src/main/java/com/english/generate/GenerateService.java` — `buildPrompt` 메서드 (배열 프롬프트 패턴 참고, 163~211행)
- `backend/src/main/java/com/english/config/GeminiClient.java` — `generateContent(String, Class<T>)` 메서드 시그니처 확인
- `backend/src/test/java/com/english/word/WordServiceTest.java` — 기존 테스트 구조 확인

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 1. `BulkWordEnrichment.java` 생성 (신규 파일)

`backend/src/main/java/com/english/word/BulkWordEnrichment.java`

Gemini API가 배열 형태로 반환할 벌크 보강 응답 DTO를 생성한다.

```java
// 시그니처만 제시. 구현은 에이전트 재량.
public class BulkWordEnrichment {
    List<Item> enrichments;  // 단어별 보강 결과 배열

    public static class Item {
        String word;           // 매핑용 원본 단어 (프롬프트에 보낸 단어와 대조)
        String partOfSpeech;
        String pronunciation;
        String synonyms;
        String tip;
    }
}
```

핵심 규칙:
- `word` 필드는 반드시 포함한다. 이유: Gemini 응답 순서가 입력 순서와 다를 수 있으므로, 단어명으로 매핑해야 한다.
- Lombok `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor` 사용 (기존 `WordEnrichment.java` 패턴 참고).
- Jackson 역직렬화가 가능해야 한다 (GeminiClient.parseResponse에서 ObjectMapper 사용).

### 2. `WordService.java`에 벌크 프롬프트 빌더 메서드 추가

`backend/src/main/java/com/english/word/WordService.java`

```java
// 시그니처만 제시. 구현은 에이전트 재량.
String buildBulkEnrichmentPrompt(List<WordCreateRequest> requests)
```

핵심 규칙:
- 프롬프트 구조: 단어 배열을 JSON으로 포함하고, 응답도 `BulkWordEnrichment` 구조의 JSON 배열로 요청한다.
- GenerateService.buildPrompt(163~211행)의 배열 패턴을 참고하되, 예문이 아닌 단어 보강 정보를 요청한다.
- 기존 `buildEnrichmentPrompt` (단건용)은 그대로 유지한다. 이유: 단건 등록(`create`)에서 계속 사용.
- 메서드 접근제한자는 `package-private` (테스트에서 직접 호출 가능하도록). 기존 `buildEnrichmentPrompt`가 private이므로, 벌크 프롬프트는 별도 메서드로 분리하여 테스트 가능하게 한다.

### 3. 테스트 추가

`backend/src/test/java/com/english/word/WordServiceTest.java`에 프롬프트 빌더 테스트를 추가한다.

```java
// 시그니처만 제시. 구현은 에이전트 재량.
@Nested
@DisplayName("벌크 보강 프롬프트")
class BulkEnrichmentPrompt {
    // 프롬프트에 모든 단어가 JSON 배열로 포함되는지 검증
    // 프롬프트에 응답 형식 지시가 포함되는지 검증
}
```

핵심 규칙:
- 기존 테스트는 절대 수정하지 않는다.
- 프롬프트 빌더만 단독 테스트한다 (GeminiClient mock 불필요).

## Acceptance Criteria

```bash
# Critical Gate: 기존 + 신규 테스트 통과
cd backend && ./gradlew test --tests "com.english.word.WordServiceTest"

# 보조 검증: 전체 테스트 회귀 없음
cd backend && ./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - BulkWordEnrichment DTO가 Jackson 역직렬화 가능한 구조인가?
   - buildBulkEnrichmentPrompt가 단어 배열을 JSON으로 포함하는 프롬프트를 생성하는가?
   - 기존 단건 테스트가 깨지지 않았는가?
3. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가?
   - ADR 기술 스택을 벗어나지 않았는가?
   - CLAUDE.md CRITICAL 규칙을 위반하지 않았는가?
   - Step Contract의 Write Scope 밖을 수정하지 않았는가?
   - `integration-hardening` 외 step에서 backend와 frontend를 동시에 수정하지 않았는가?
4. 결과에 따라 `phases/5-bulk-enrichment-batch/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- `WordService.bulkCreate` 메서드를 수정하지 마라. 이유: Step 1에서 배치 로직 적용 시 수정한다. 이 step은 DTO와 프롬프트 빌더만 담당한다.
- GeminiClient를 수정하지 마라. 이유: 기존 `generateContent(String, Class<T>)` 메서드로 충분하다.
- 기존 `buildEnrichmentPrompt` (단건용)을 삭제하거나 변경하지 마라. 이유: 단건 등록(`create`)에서 계속 사용한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다.
- `TODO`, `not implemented`, `stub`, 빈 배열/빈 객체, 고정 더미 반환으로 핵심 기능을 대체하고 completed 처리하지 마라. 이유: 미구현을 완료로 오판하는 것을 방지한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

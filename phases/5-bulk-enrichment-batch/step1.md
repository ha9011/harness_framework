# Step 1: batch-bulk-create

## Step Contract

- Capability: WordService.bulkCreate를 배치 방식으로 리팩터링하여 N번 API 호출을 ⌈N/25⌉번으로 감소
- Layer: service
- Write Scope: `backend/src/main/java/com/english/word/WordService.java`, `backend/src/test/java/com/english/word/WordServiceTest.java`
- Out of Scope: GeminiClient 변경, 단건 등록(`create`) 변경, controller 변경, frontend 코드
- Critical Gates: `cd backend && ./gradlew test --tests "com.english.word.WordServiceTest"` — 기존 벌크 테스트 수정 + 배치 분할/배치 실패 신규 테스트 통과

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md` — ADR-010 (벌크 보강 배치 전략)
- `backend/src/main/java/com/english/word/WordService.java` — Step 0에서 추가된 `buildBulkEnrichmentPrompt`, `BulkWordEnrichment` 확인
- `backend/src/main/java/com/english/word/BulkWordEnrichment.java` — Step 0에서 생성된 벌크 DTO
- `backend/src/main/java/com/english/config/GeminiClient.java` — `generateContent(String, Class<T>)` 재사용
- `backend/src/test/java/com/english/word/WordServiceTest.java` — Step 0에서 추가된 테스트 + 기존 벌크 테스트 확인

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 1. `WordService.bulkCreate` 리팩터링

`backend/src/main/java/com/english/word/WordService.java`

현재 `bulkCreate`는 for 루프에서 건바이건으로 `geminiClient.generateContent`를 호출한다. 이를 배치 방식으로 변경한다.

리팩터링 흐름 (시그니처 수준 지시, 구현은 에이전트 재량):

```
1. 중복 검사 (건바이건 유지)
   - for 루프로 각 단어 중복 검사 → 유효한 단어 리스트 분리, 중복은 skipped 카운트
   
2. 유효한 단어들을 Word 엔티티로 생성 (아직 보강 없이)
   - Map<String, Word> wordMap 구성 (단어명 → Word 엔티티, 매핑용)

3. 배치 분할 (25개씩)
   - List<List<WordCreateRequest>> batches = partition(validRequests, BATCH_SIZE)

4. 배치별 Gemini API 호출
   - for (batch : batches):
     - String prompt = buildBulkEnrichmentPrompt(batch)
     - BulkWordEnrichment result = geminiClient.generateContent(prompt, BulkWordEnrichment.class)
     - result.getEnrichments()에서 word 필드로 wordMap 매핑 → word.enrich() 호출
     - 배치 실패(Exception) → 해당 배치 전체 enrichmentFailed += batch.size()

5. 저장 + 학습기록 + 복습아이템 (건바이건 유지)
   - for (word : validWords): wordRepository.save(word), studyRecordService, reviewItemService
```

핵심 규칙:
- **배치 크기 상수**: `private static final int BULK_ENRICHMENT_BATCH_SIZE = 25;`
- **단어명 매핑**: Gemini 응답의 `word` 필드로 매핑한다. 이유: 응답 순서가 입력 순서와 다를 수 있다.
- **매핑 실패 처리**: Gemini 응답에 없는 단어는 미보강 저장한다. enrichmentFailed에 포함.
- **배치 실패 처리**: 배치 단위로 try-catch. 실패한 배치의 단어들은 미보강 저장. 나머지 배치는 계속 진행.
- **BulkCreateResponse**: 기존 응답 구조(`saved`, `skipped`, `enrichmentFailed`, `words`) 그대로 유지.
- **트랜잭션**: 기존 `@Transactional` 유지.

### 2. 테스트 수정 및 추가

`backend/src/test/java/com/english/word/WordServiceTest.java`

#### 기존 `BulkCreate` 테스트 수정

기존 `bulkCreateSuccess` 테스트는 `geminiClient.generateContent(contains("banana"), eq(WordEnrichment.class))` 형태로 건바이건 호출을 검증한다. 배치 방식으로 변경되었으므로 이 테스트의 mock 설정과 verify를 수정해야 한다.

⚠️ 테스트 변경 사유: ADR-010에 의해 벌크 등록의 API 호출 방식이 건바이건 → 배치로 변경됨. mock 대상이 `WordEnrichment.class` → `BulkWordEnrichment.class`로 변경.

#### 신규 테스트 추가

```java
// 시그니처만 제시. 구현은 에이전트 재량.

@Test
@DisplayName("배치 분할 — 55개 → 3배치 (25+25+5)")
void bulkCreateBatchPartition() { ... }

@Test  
@DisplayName("배치 실패 — 해당 배치만 미보강 저장")
void bulkCreateBatchFailure() { ... }

@Test
@DisplayName("Gemini 응답에서 단어명으로 매핑")
void bulkCreateWordMapping() { ... }
```

핵심 규칙:
- `bulkCreateBatchPartition`: 55개 요청 시 `geminiClient.generateContent`가 정확히 3회 호출되는지 verify.
- `bulkCreateBatchFailure`: 첫 배치 성공, 두 번째 배치 실패 시 → 첫 배치 단어는 보강됨, 두 번째 배치 단어는 미보강 저장, enrichmentFailed 카운트 정확성.
- `bulkCreateWordMapping`: Gemini 응답의 word 필드가 입력과 매핑되어 올바른 Word 엔티티에 enrich 되는지 검증.

## Acceptance Criteria

```bash
# Critical Gate: 벌크 테스트 수정 + 신규 배치 테스트 통과
cd backend && ./gradlew test --tests "com.english.word.WordServiceTest"

# 보조 검증: 전체 테스트 회귀 없음
cd backend && ./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - 배치 분할이 정확히 동작하는가? (55개 → 3배치)
   - 배치 실패 시 해당 배치만 미보강이고 나머지는 정상 보강되는가?
   - Gemini 응답의 word 필드로 올바르게 매핑되는가?
   - 기존 단건 등록 테스트가 깨지지 않았는가?
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

- GeminiClient를 수정하지 마라. 이유: 기존 `generateContent(String, Class<T>)` 메서드로 충분하다.
- 단건 등록(`create`) 메서드를 수정하지 마라. 이유: 이 step은 벌크 등록만 대상이다.
- Controller를 수정하지 마라. 이유: 서비스 내부 리팩터링이며 API 인터페이스는 변경 없다.
- 기존 단건 테스트(CreateWord 클래스)의 기대값을 변경하지 마라. 이유: 단건 등록은 변경 대상이 아니다.
- 기존 벌크 테스트 수정 시 summary에 "⚠️ 테스트 변경: ADR-010 배치 전환" 기록 필수. 이유: 기대값 변경의 근거를 명시한다.
- `TODO`, `not implemented`, `stub`, 빈 배열/빈 객체, 고정 더미 반환으로 핵심 기능을 대체하고 completed 처리하지 마라. 이유: 미구현을 완료로 오판하는 것을 방지한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

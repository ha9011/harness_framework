# Step 8: backend-generate-service

## Step Contract

- Capability: sentence generation service persistence and mapping
- Layer: service
- Write Scope: `backend/src/main/java/com/english/generate/`, `backend/src/main/java/com/english/review/`, `backend/src/test/java/com/english/generate/`
- Out of Scope: HTTP controllers, review selection algorithm, frontend files, real Gemini network calls
- Critical Gates: `cd backend && ./gradlew test --tests "*GenerateServiceTest"` and `cd backend && ./gradlew test --tests "*GeneratedSentenceMappingTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/DESIGN.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/generate/`
- `/backend/src/main/java/com/english/word/`
- `/backend/src/main/java/com/english/pattern/`
- `/backend/src/main/java/com/english/review/`

## 작업

예문 생성 service를 TDD로 구현한다.

필수 구현:
- `GenerateService.generate(userId, level, count)`는 사용자 소유 단어/패턴만 사용한다.
- 전체 생성은 단어 우선순위: important, WORD RECOGNITION review_count 낮음, 랜덤 순서를 따른다.
- Gemini 요청에는 최대 50개 단어 후보와 패턴 후보를 전달한다.
- `generateForWord(userId, wordId, level, count)`는 지정 단어 소유권을 검증하고 패턴 없이 생성한다.
- `generateForPattern(userId, patternId, level, count)`는 지정 패턴 소유권을 검증하고 단어 후보를 자동 선택한다.
- Gemini 응답의 wordIds/patternId는 DB에 존재하고 사용자 소유인 경우만 매핑한다.
- `generation_history`, `generated_sentences`, `generated_sentence_words`, `sentence_situations`를 저장한다.
- 예문마다 상황 5개를 저장한다.
- 생성된 문장마다 SENTENCE/RECOGNITION review_item을 생성한다.
- Gemini 실패 시 예문 생성은 에러로 반환하고 저장하지 않는다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*GenerateServiceTest"
cd backend && ./gradlew test --tests "*GeneratedSentenceMappingTest"
cd backend && ./gradlew test
```

## 검증 절차

1. fake GeminiClient로 생성 결과 저장 흐름을 검증한다.
2. 단어 우선순위와 최대 50개 제한을 검증한다.
3. invalid wordId/patternId 매핑 무시와 예문 저장 정책을 검증한다.
4. Step 8을 `completed`로 표시하고 summary에 GenerateService 핵심 저장 흐름을 적는다.

## 금지사항

- HTTP Controller를 구현하지 마라. 이유: Step 13에서 API 계약을 구현한다.
- 실제 Gemini 네트워크를 호출하지 마라. 이유: service 테스트는 fake client 기반이어야 한다.
- review selection/SM-2 알고리즘을 구현하지 마라. 이유: Step 9의 scope다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

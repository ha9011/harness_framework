# Step 7: backend-gemini-client

## Step Contract

- Capability: Gemini Vision/Text external client with structured JSON parsing and fallback
- Layer: external-client
- Write Scope: `backend/src/main/java/com/english/generate/`, `backend/src/test/java/com/english/generate/`, `backend/src/main/java/com/english/word/`, `backend/src/main/java/com/english/pattern/`
- Out of Scope: GenerateService persistence, word/pattern controllers, frontend files, real network-dependent tests
- Critical Gates: `cd backend && ./gradlew test --tests "*GeminiClientFakeServerTest"` and `cd backend && ./gradlew test --tests "*GeminiClientFallbackTest"` using a fake server test

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/DESIGN.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/generate/`

## 작업

Gemini API 연동을 외부 client로 분리해 구현한다.

필수 구현:
- `GeminiClient` interface를 정의한다.
- 메서드는 단어 보강, 단어 이미지 추출, 패턴 이미지 추출, 예문 생성 요청을 분리한다.
- 구현체는 `GEMINI_API_KEY` 환경 변수 또는 설정값으로 API key를 주입받는다.
- structured JSON output과 schema validation을 사용한다.
- parsing 실패와 5xx/429는 최대 2회 재시도한다.
- 단건/벌크 단어 보강 실패는 service가 fallback할 수 있도록 명확한 예외/결과 타입을 반환한다.
- 테스트는 fake server 또는 fake provider로 request body, 인증 정보 주입, response parsing, retry/fallback을 검증한다.
- 실제 Gemini 네트워크 호출은 테스트에 포함하지 않는다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*GeminiClientFakeServerTest"
cd backend && ./gradlew test --tests "*GeminiClientFallbackTest"
cd backend && ./gradlew test
```

## 검증 절차

1. fake server가 받은 request에 API key와 JSON schema 요청이 포함되는지 확인한다.
2. 정상 JSON 응답이 domain DTO로 파싱되는지 확인한다.
3. invalid JSON과 5xx 응답에서 retry/fallback이 동작하는지 확인한다.
4. Step 7을 `completed`로 표시하고 summary에 GeminiClient interface와 fake server 테스트명을 적는다.

## 금지사항

- 실제 Gemini API를 테스트에서 호출하지 마라. 이유: 외부 네트워크와 API key에 의존하면 하네스가 불안정해진다.
- service persistence 로직을 구현하지 마라. 이유: 예문 저장은 Step 8에서 다룬다.
- frontend 파일을 수정하지 마라. 이유: 이 step은 external-client 전용이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

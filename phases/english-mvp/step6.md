# Step 6: backend-learning-controller

## Step Contract

- Capability: word and pattern REST APIs
- Layer: controller
- Write Scope: `backend/src/main/java/com/english/word/`, `backend/src/main/java/com/english/pattern/`, `backend/src/main/java/com/english/config/`, `backend/src/test/java/com/english/word/`, `backend/src/test/java/com/english/pattern/`
- Out of Scope: service algorithm changes, Gemini client implementation, generate/review APIs, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*WordControllerTest"` and `cd backend && ./gradlew test --tests "*PatternControllerTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/word/`
- `/backend/src/main/java/com/english/pattern/`
- `/backend/src/main/java/com/english/auth/`

## 작업

단어/패턴 HTTP API를 구현한다.

필수 구현:
- `GET /api/words`: page, size, search, partOfSpeech, importantOnly, sort 지원.
- `GET /api/words/{id}`: 단어 상세와 해당 단어가 사용된 예문 목록 구조를 반환할 준비를 한다.
- `POST /api/words`: 단건 등록.
- `POST /api/words/bulk`: saved, skipped, enrichmentFailed 분리 응답 구조.
- `PUT /api/words/{id}`, `PATCH /api/words/{id}/important`, `DELETE /api/words/{id}`.
- `GET /api/patterns`, `GET /api/patterns/{id}`, `POST /api/patterns`, `PUT /api/patterns/{id}`, `DELETE /api/patterns/{id}`.
- 요청 DTO validation을 적용한다.
- 현재 인증 사용자 기준으로만 service를 호출한다.
- 타 사용자 리소스 접근은 403으로 반환한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*WordControllerTest"
cd backend && ./gradlew test --tests "*PatternControllerTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 모든 API가 인증 없이는 401을 반환하는지 확인한다.
2. 사용자별 리소스 접근 범위와 403 응답을 확인한다.
3. 페이지네이션 응답 구조가 API 설계서와 맞는지 확인한다.
4. Step 6을 `completed`로 표시하고 summary에 단어/패턴 API 범위를 적는다.

## 금지사항

- Gemini extract endpoint를 구현하지 마라. 이유: external-client step 이후 연결해야 한다.
- Generate/Review API를 구현하지 마라. 이유: 후속 controller step과 분리한다.
- service 테스트 기대값을 수정하지 마라. 이유: controller 구현으로 service 버그를 숨기면 안 된다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

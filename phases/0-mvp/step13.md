# Step 13: backend-generate-controller

## Step Contract

- Capability: sentence generation REST APIs
- Layer: controller
- Write Scope: `backend/src/main/java/com/english/generate/`, `backend/src/main/java/com/english/config/`, `backend/src/test/java/com/english/generate/`
- Out of Scope: GenerateService algorithm changes, review APIs, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*GenerateControllerTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/generate/`
- `/backend/src/main/java/com/english/auth/`

## 작업

예문 생성 HTTP API를 구현한다.

필수 구현:
- `POST /api/generate` body `{ level, count }`.
- `POST /api/generate/word` body `{ wordId, level, count }`.
- `POST /api/generate/pattern` body `{ patternId, level, count }`.
- `GET /api/generate/history?page=0&size=20`.
- level은 유아/초등/중등/고등만 허용한다.
- count는 전체 생성 10/20/30, 단어 상세 생성은 설계서에 맞춰 5/10 또는 service 허용값과 일치시킨다.
- NO_WORDS, NO_PATTERNS, AI_SERVICE_ERROR 등 에러 응답을 API 설계서 형식으로 반환한다.
- 현재 인증 사용자 기준으로만 생성/조회한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*GenerateControllerTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 각 generate endpoint의 요청 검증과 응답 구조를 확인한다.
2. 생성 이력 페이지네이션 구조를 확인한다.
3. 미인증 401과 타 사용자 resource 403이 지켜지는지 확인한다.
4. Step 13을 `completed`로 표시하고 summary에 generate API 목록을 적는다.

## 금지사항

- Review API를 구현하지 마라. 이유: Step 14의 scope다.
- GenerateService 테스트 기대값을 변경하지 마라. 이유: controller 구현으로 service 버그를 숨기면 안 된다.
- frontend 파일을 수정하지 마라. 이유: 이 step은 백엔드 controller 전용이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

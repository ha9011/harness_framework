# Step 3: backend-auth-controller

## Step Contract

- Capability: authentication HTTP API and security boundary
- Layer: controller
- Write Scope: `backend/src/main/java/com/english/auth/`, `backend/src/main/java/com/english/config/`, `backend/src/test/java/com/english/auth/`, `backend/src/test/java/com/english/config/`
- Out of Scope: learning APIs, DB schema beyond auth, frontend files, Gemini integration
- Critical Gates: `cd backend && ./gradlew test --tests "*AuthControllerTest"` and `cd backend && ./gradlew test --tests "*SecurityConfigTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/auth/AuthService.java`
- `/backend/src/main/java/com/english/auth/JwtProvider.java`

## 작업

인증 HTTP API와 보안 경계를 구현한다.

필수 구현:
- `POST /api/auth/signup`: 201 Created, 가입 즉시 JWT HttpOnly Cookie 발급.
- `POST /api/auth/login`: 200 OK, JWT HttpOnly Cookie 발급.
- `POST /api/auth/logout`: 204 No Content, Cookie 삭제.
- `GET /api/auth/me`: 현재 사용자 응답.
- Cookie 이름은 일관되게 하나로 정하고 Path는 `/api`, HttpOnly, SameSite Lax, Max-Age 86400을 사용한다.
- `/api/auth/signup`, `/api/auth/login`, `/api/health`는 인증 없이 접근 가능하다.
- 그 외 `/api/**`는 인증 필수다.
- 인증 실패는 `{ "error": "UNAUTHORIZED", "message": "..." }` 형태로 반환한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*AuthControllerTest"
cd backend && ./gradlew test --tests "*SecurityConfigTest"
cd backend && ./gradlew test
```

## 검증 절차

1. signup/login 응답의 Set-Cookie 속성을 MockMvc로 검증한다.
2. logout이 Max-Age 0 Cookie를 반환하는지 검증한다.
3. 미인증 `/api/auth/me`와 보호 API 요청이 401을 반환하는지 검증한다.
4. Step 3을 `completed`로 표시하고 summary에 인증 endpoint와 SecurityConfig 완료를 적는다.

## 금지사항

- 단어/패턴 API를 열거나 임시 허용하지 마라. 이유: `/api/auth/*` 외 API 인증 필수 규칙을 고정해야 한다.
- CORS origin을 wildcard로 설정하지 마라. 이유: Cookie credentials와 함께 사용할 수 없다.
- frontend 코드를 수정하지 마라. 이유: 이 step은 백엔드 controller layer만 담당한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

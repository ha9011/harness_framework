# Step 8: 전체 통합 검증

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 전체 상태를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/phases/1-auth/index.json` — 이전 Step들의 summary 확인

그리고 Step 0~7에서 생성/수정된 핵심 파일들을 빠르게 훑어보라.

## 작업

### 1. 백엔드 전체 테스트

```bash
cd backend && ./gradlew test
cd backend && ./gradlew integrationTest
```

두 명령 모두 통과해야 한다. 실패하는 테스트가 있으면 원인을 분석하고 수정하라.

### 2. 프론트엔드 빌드 + 린트

```bash
cd frontend && npm run build
cd frontend && npm run lint
```

두 명령 모두 통과해야 한다. 실패하면 원인을 분석하고 수정하라.

### 3. 보안 체크리스트 최종 확인

아래 항목을 코드에서 직접 확인하라:

- [ ] JWT secret이 `${JWT_SECRET}` 환경변수로 주입되는지 (application.yml)
- [ ] BCryptPasswordEncoder가 사용되는지 (SecurityConfig)
- [ ] 로그인 실패 메시지가 "이메일 또는 비밀번호가 올바르지 않습니다"인지 (AuthService)
- [ ] 모든 리소스 접근에서 userId 검증이 되는지 (WordService, PatternService 등의 findByIdAndUser)
- [ ] softDelete 쿼리에 user 조건이 포함되는지 (ReviewItemRepository)
- [ ] GlobalExceptionHandler의 일반 Exception이 마스킹되는지 ("서버 오류가 발생했습니다")
- [ ] CORS에 allowCredentials(true)가 설정되는지 (SecurityConfig)
- [ ] api.ts에 credentials: "include"가 있는지 (request + uploadRequest)
- [ ] AuthGuard가 loading 중 리다이렉트하지 않는지

체크리스트에서 누락된 항목이 있으면 수정하라.

### 4. 문서 정합성 확인 (읽기만)

CLAUDE.md의 JWT_SECRET 환경변수가 기재되어 있는지 확인하라. 누락되었으면 추가하라.

## Acceptance Criteria

```bash
cd backend && ./gradlew test && ./gradlew integrationTest && cd ../frontend && npm run build && npm run lint
```

- 백엔드 단위 테스트 전체 통과
- 백엔드 통합 테스트 전체 통과
- 프론트엔드 빌드 성공
- 프론트엔드 린트 통과

## 검증 절차

1. 위 AC 커맨드를 실행
2. 보안 체크리스트 9개 항목 확인
3. 실패 항목이 있으면 수정 후 재실행

## 금지사항

- 테스트를 통과시키기 위해 테스트 자체를 삭제하거나 @Disabled하지 마라. 이유: 근본 원인을 수정해야 한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

# Step 6: 통합 테스트 — 인증 + 데이터 격리

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 통합 테스트 전략, 데이터 격리 테스트
- `/docs/ARCHITECTURE.md` — 인증 흐름
- `/backend/src/test/java/com/english/integration/IntegrationTestBase.java` — 기존 통합 테스트 베이스
- `/backend/src/test/java/com/english/integration/` 하위 모든 통합 테스트 파일
- `/backend/src/main/java/com/english/auth/AuthController.java` — signup/login API
- `/backend/src/main/java/com/english/config/SecurityConfig.java` — 인증 설정

이전 Step에서 수정된 모든 Entity, Repository, Service, Controller 파일도 읽어라.

## 작업

### 1. IntegrationTestBase 인증 헬퍼 추가

`IntegrationTestBase.java`에 인증 헬퍼 메서드를 추가하라:

```java
protected HttpHeaders signupAndGetAuthHeaders(String email, String password, String nickname) {
    // 1. POST /api/auth/signup 호출
    // 2. 응답의 Set-Cookie 헤더에서 token Cookie 추출
    // 3. Cookie 헤더를 포함한 HttpHeaders 반환
    // 이후 restTemplate.exchange(..., new HttpEntity<>(body, headers), ...) 로 인증된 요청 가능
}

// 기본 테스트 사용자용 편의 메서드
protected HttpHeaders getDefaultAuthHeaders() {
    return signupAndGetAuthHeaders("test@test.com", "password123", "테스터");
}
```

TestRestTemplate은 기본적으로 Cookie를 유지하지 않으므로, 매 요청마다 Cookie 헤더를 직접 첨부해야 한다.

### 2. 기존 6개 통합 테스트 수정

모든 통합 테스트에서 API 호출 시 인증 헤더를 포함하도록 수정하라:

- **WordIntegrationTest**: 단어 등록/조회에 인증 헤더 추가
- **GenerateIntegrationTest**: 단어/패턴 등록 + 예문 생성에 인증 헤더 추가
- **ReviewIntegrationTest**: 단어 등록 + 복습에 인증 헤더 추가
- **SoftDeleteIntegrationTest**: 삭제 연쇄에 인증 헤더 추가
- **GeminiFallbackIntegrationTest**: Gemini 장애 시나리오에 인증 헤더 추가
- **E2EFlowTest**: 전체 흐름에 인증 헤더 추가

기존 테스트의 **검증 로직(assert)은 변경하지 마라**. 변경하는 것은 API 호출 시 인증 헤더 추가뿐이다.

### 3. AuthIntegrationTest 신규 작성

`backend/src/test/java/com/english/integration/AuthIntegrationTest.java`:

테스트 케이스:
- **E2E 흐름**: 회원가입 → 로그인 → me 조회 → 로그아웃 → me 조회 시 401
- **이메일 중복**: 동일 이메일로 두 번 가입 시 409
- **미인증 접근**: 인증 없이 GET /api/words → 401
- **미인증 접근**: 인증 없이 POST /api/words → 401

### 4. 데이터 격리 테스트

`AuthIntegrationTest.java`에 추가 또는 별도 파일:

- **사용자 A 단어 → 사용자 B 미조회**: A가 단어 등록 → B가 GET /api/words → A의 단어 미포함
- **동일 단어 독립 등록**: A가 "apple" 등록 → B가 "apple" 등록 → 둘 다 성공 (다른 ID)
- **사용자 A 삭제 → 사용자 B 영향 없음**: A가 단어 삭제 → B의 동일 단어 유지

## Acceptance Criteria

```bash
cd backend && ./gradlew test && cd backend && ./gradlew integrationTest
```

- 전체 단위 테스트 통과
- 전체 통합 테스트 통과 (기존 6개 + AuthIntegrationTest)

## 검증 절차

1. `cd backend && ./gradlew test` 실행 — 단위 테스트 통과
2. `cd backend && ./gradlew integrationTest` 실행 — 통합 테스트 통과
3. IntegrationTestBase에 인증 헬퍼가 추가되었는지 확인
4. 기존 6개 통합 테스트가 인증 헤더를 사용하는지 확인
5. 데이터 격리 테스트가 존재하는지 확인

## 금지사항

- 기존 통합 테스트의 assert 값을 변경하지 마라. 변경하는 것은 API 호출 방식(헤더 추가)뿐이다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하라.
- 프론트엔드 코드를 수정하지 마라. 이유: Step 7에서 처리한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

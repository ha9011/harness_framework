# Step 6: 통합 마무리

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — 특히 "Step 6: 통합 마무리"
- `/docs/ARCHITECTURE.md` — 전체 데이터 모델, 에러 처리 흐름
- `/docs/ADR.md` — 전체 ADR 검토
- `/docs/PRD.md` — 에러 처리/엣지케이스 섹션
- `/backend/src/main/java/com/english/` — 전체 도메인 코드
- `/backend/src/test/java/com/english/` — 기존 테스트 전체 확인
- `/frontend/app/` — 전체 프론트엔드 코드

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 1. TestContainers 통합 테스트

`backend/src/test/java/com/english/integration/`:

#### IntegrationTestBase

```java
// @SpringBootTest(webEnvironment = RANDOM_PORT)
// @Testcontainers
// PostgreSQLContainer 설정
// TestRestTemplate 주입
```

#### WordIntegrationTest

- 단어 등록 → DB 저장 확인 → review_items 2개 생성 확인 → study_record 생성 확인
- 단어 벌크 등록 → saved/skipped 카운트 확인

#### GenerateIntegrationTest

- 단어 등록 → 예문 생성 → sentence_situations 5개 확인 → generated_sentence_words 매핑 확인
- generation_history 기록 확인

#### ReviewIntegrationTest

- 카드 선정 → SM-2 제출 → next_review_date 변경 확인 → review_log 기록 확인
- 타입별 독립 선정 확인

#### SoftDeleteIntegrationTest

- 단어 삭제 → WORD review_items deleted=true 확인
- 해당 단어 포함 예문 유지 확인
- SENTENCE review_items 유지 확인
- 패턴 삭제 → 동일 검증

#### GeminiFallbackIntegrationTest (GeminiClient mock 사용)

- 단어 등록 시 Gemini 실패 → 보강 없이 저장 확인
- 예문 생성 시 Gemini 실패 → 502 에러 확인

### 2. E2E 흐름 검증 테스트

`backend/src/test/java/com/english/integration/E2EFlowTest.java`:

1. 단어 5개 등록
2. 패턴 2개 등록
3. 예문 10개 생성 (일반)
4. 복습 카드 조회 (WORD 탭)
5. SM-2 제출 (EASY/MEDIUM/HARD 각 1개)
6. 대시보드 조회 → 카운트 검증
7. 학습 기록 조회 → Day 1 확인

### 3. 프론트엔드 UX 정리

- 빈 상태(empty state) UI 검토: 단어 0개, 패턴 0개, 복습 0개 등
- 에러 토스트 표시 통일
- 로딩 상태 (skeleton/spinner) 추가
- 모바일 반응형 검증 (max-w-md 중앙 정렬)
- 하단 네비게이션 모든 페이지에서 표시 확인

### 4. 크로스 도메인 정합성 검증

- 모든 도메인의 soft delete가 연쇄적으로 올바르게 동작하는지 확인
- Polymorphic association (item_type + item_id) 검증: 존재하지 않는 item_id 참조 시 적절한 처리
- Gemini fallback 시나리오 전체 검증

## Acceptance Criteria

```bash
docker compose up -d                          # PostgreSQL 기동
cd backend && ./gradlew test                  # 전체 테스트 통과 (단위 + 통합)
cd frontend && npm run build                  # 빌드 성공
cd frontend && npm run lint                   # ESLint 통과
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가?
   - ADR 기술 스택을 벗어나지 않았는가?
   - CLAUDE.md CRITICAL 규칙을 위반하지 않았는가?
3. PRD의 "에러 처리 / 엣지케이스" 섹션을 하나씩 검증한다.
4. 결과에 따라 `phases/0-mvp/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 → `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- 새로운 기능을 추가하지 마라. 이유: 이 step은 기존 구현의 통합 검증만 한다.
- 기존 단위 테스트를 삭제하거나 @Disabled 처리하지 마라. 이유: 통합 테스트는 단위 테스트를 대체하지 않는다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

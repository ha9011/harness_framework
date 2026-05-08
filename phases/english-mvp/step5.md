# Step 5: backend-learning-service

## Step Contract

- Capability: word and pattern learning service behavior
- Layer: service
- Write Scope: `backend/src/main/java/com/english/word/`, `backend/src/main/java/com/english/pattern/`, `backend/src/main/java/com/english/study/`, `backend/src/main/java/com/english/review/`, `backend/src/test/java/com/english/word/`, `backend/src/test/java/com/english/pattern/`, `backend/src/test/java/com/english/study/`, `backend/src/test/java/com/english/review/`
- Out of Scope: HTTP controllers, Gemini client, sentence generation, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*WordServiceTest"` and `cd backend && ./gradlew test --tests "*PatternServiceTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/DESIGN.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/word/`
- `/backend/src/main/java/com/english/pattern/`
- `/backend/src/main/java/com/english/study/`
- `/backend/src/main/java/com/english/review/`

## 작업

단어/패턴 service 계층을 TDD로 구현한다.

필수 구현:
- WordService: 단건 저장, 벌크 저장, 수정, 중요 토글, soft delete, 검색/필터/페이지네이션.
- PatternService: 직접 저장, 교재 예문 순서 보존 저장, 수정, soft delete, 목록/상세 조회.
- 같은 사용자의 중복 단어/패턴은 앱 레벨에서 스킵 또는 conflict 처리한다.
- 다른 사용자는 같은 단어/패턴을 독립 등록할 수 있다.
- 단어/패턴 등록 시 해당 날짜 학습 기록과 review_items를 자동 생성한다.
- WORD/PATTERN review_items는 RECOGNITION과 RECALL 양방향을 생성한다.
- 단어 삭제 시 해당 단어 WORD review_items만 soft delete한다.
- 패턴 삭제 시 해당 패턴 PATTERN review_items만 soft delete한다.
- 예문과 SENTENCE review_items는 단어/패턴 삭제 시 유지한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*WordServiceTest"
cd backend && ./gradlew test --tests "*PatternServiceTest"
cd backend && ./gradlew test --tests "*StudyRecordServiceTest"
cd backend && ./gradlew test --tests "*ReviewItemCreationTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 단어/패턴 등록 시 학습 기록과 review_items가 생성되는지 확인한다.
2. 중복 처리와 사용자별 독립 등록을 확인한다.
3. soft delete 연쇄 범위가 PRD와 맞는지 확인한다.
4. Step 5를 `completed`로 표시하고 summary에 WordService/PatternService 핵심 동작을 적는다.

## 금지사항

- Controller를 구현하지 마라. 이유: HTTP 계약은 Step 6에서 다룬다.
- Gemini 보강/추출을 직접 호출하지 마라. 이유: 외부 연동은 Step 7로 분리한다.
- 단어/패턴 삭제 시 생성 예문을 삭제하지 마라. 이유: PRD의 학습 이력 보존 정책이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

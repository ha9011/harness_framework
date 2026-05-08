# Step 10: backend-study-service

## Step Contract

- Capability: study record creation and query service
- Layer: service
- Write Scope: `backend/src/main/java/com/english/study/`, `backend/src/test/java/com/english/study/`
- Out of Scope: HTTP controllers, word/pattern registration behavior changes unless tests reveal integration break, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*StudyRecordServiceTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/study/`
- `/backend/src/main/java/com/english/word/`
- `/backend/src/main/java/com/english/pattern/`

## 작업

학습 기록 service를 TDD로 구현한다.

필수 구현:
- 단어/패턴 등록 service에서 호출 가능한 `recordLearning(userId, itemType, itemId)`를 제공한다.
- 같은 사용자+날짜에는 study_records가 하나만 생성된다.
- day_number는 실제 학습한 날짜 수 기준으로 증가한다.
- 같은 날짜 같은 itemType/itemId는 중복 기록하지 않는다.
- `getStudyRecords(userId, page, size)`는 최신순 페이지네이션과 item 목록을 반환한다.
- item 표시 이름은 WORD는 word, PATTERN은 template을 사용한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*StudyRecordServiceTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 첫 학습일 day_number가 1인지 확인한다.
2. 쉬는 날이 있어도 다음 학습일 day_number가 연속 증가하는지 확인한다.
3. 같은 날 같은 항목 중복 방지가 동작하는지 확인한다.
4. Step 10을 `completed`로 표시하고 summary에 day_number와 조회 DTO 구현을 적는다.

## 금지사항

- Controller를 구현하지 마라. 이유: Step 15에서 HTTP API를 구현한다.
- 학습 기록을 물리 삭제하는 기능을 추가하지 마라. 이유: MVP 요구사항에 없다.
- frontend 파일을 수정하지 마라. 이유: 이 step은 백엔드 service 전용이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

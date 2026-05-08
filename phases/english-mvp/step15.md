# Step 15: backend-study-controller

## Step Contract

- Capability: study record REST API
- Layer: controller
- Write Scope: `backend/src/main/java/com/english/study/`, `backend/src/test/java/com/english/study/`
- Out of Scope: StudyRecordService algorithm changes, dashboard APIs, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*StudyRecordControllerTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/study/`
- `/backend/src/main/java/com/english/auth/`

## 작업

학습 기록 조회 HTTP API를 구현한다.

필수 구현:
- `GET /api/study-records?page=0&size=20`.
- 응답은 content, totalElements, totalPages, page, size 구조를 따른다.
- 각 record는 id, studyDate, dayNumber, items를 포함한다.
- items는 type, id, name을 포함한다.
- 최신순 정렬을 기본으로 한다.
- 현재 인증 사용자 학습 기록만 반환한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*StudyRecordControllerTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 페이지네이션 응답 구조를 확인한다.
2. 최신순 정렬과 item DTO를 확인한다.
3. 미인증 401과 사용자별 격리를 확인한다.
4. Step 15를 `completed`로 표시하고 summary에 study-records API 완료를 적는다.

## 금지사항

- 별도 POST 학습 기록 생성 API를 만들지 마라. 이유: 학습 기록은 등록 시 자동 생성된다.
- dashboard 통계를 구현하지 마라. 이유: Step 17의 scope다.
- frontend 파일을 수정하지 마라. 이유: 백엔드 controller step이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

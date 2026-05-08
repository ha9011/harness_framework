# Step 17: backend-dashboard-controller

## Step Contract

- Capability: dashboard REST API
- Layer: controller
- Write Scope: `backend/src/main/java/com/english/dashboard/`, `backend/src/test/java/com/english/dashboard/`
- Out of Scope: DashboardService aggregation changes, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*DashboardControllerTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/dashboard/`
- `/backend/src/main/java/com/english/auth/`

## 작업

홈 대시보드 HTTP API를 구현한다.

필수 구현:
- `GET /api/dashboard`.
- 응답은 `wordCount`, `patternCount`, `sentenceCount`, `streak`, `todayReviewRemaining`, `recentStudyRecords`를 포함한다.
- todayReviewRemaining은 word, pattern, sentence 키를 가진다.
- recentStudyRecords는 최대 5건이며 studyDate, dayNumber, patternName 또는 표시 가능한 이름을 포함한다.
- 현재 인증 사용자 데이터만 집계한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*DashboardControllerTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 대시보드 응답 구조가 API 설계서와 맞는지 확인한다.
2. 미인증 401을 확인한다.
3. 현재 사용자 외 데이터가 포함되지 않는지 확인한다.
4. Step 17을 `completed`로 표시하고 summary에 dashboard API 완료를 적는다.

## 금지사항

- dashboard 계산을 controller에 구현하지 마라. 이유: service layer 책임이다.
- frontend 파일을 수정하지 마라. 이유: 백엔드 controller step이다.
- 빈 데이터를 고정 더미 응답으로 반환하지 마라. 이유: 실제 집계 동작을 검증해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

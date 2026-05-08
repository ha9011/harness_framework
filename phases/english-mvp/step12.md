# Step 12: backend-dashboard-service

## Step Contract

- Capability: home dashboard aggregation service
- Layer: service
- Write Scope: `backend/src/main/java/com/english/dashboard/`, `backend/src/test/java/com/english/dashboard/`
- Out of Scope: HTTP controllers, settings mutation, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*DashboardServiceTest"` and `cd backend && ./gradlew test --tests "*StreakCalculatorTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/dashboard/`
- `/backend/src/main/java/com/english/review/`
- `/backend/src/main/java/com/english/study/`
- `/backend/src/main/java/com/english/setting/`

## 작업

홈 대시보드 집계 service를 TDD로 구현한다.

필수 구현:
- `wordCount`, `patternCount`, `sentenceCount`는 현재 사용자와 deleted=false 기준이다.
- `todayReviewRemaining`은 WORD/PATTERN/SENTENCE별 오늘 복습 대상 수를 반환한다.
- `recentStudyRecords`는 최신 학습 기록 최대 5건을 반환한다.
- `streak`는 review_logs의 날짜별 복습 여부를 기준으로 계산한다.
- 오늘 복습했으면 오늘부터, 오늘 안 했지만 어제 복습했으면 어제부터 역순 count한다.
- 모든 집계는 현재 사용자 데이터만 대상으로 한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*DashboardServiceTest"
cd backend && ./gradlew test --tests "*StreakCalculatorTest"
cd backend && ./gradlew test
```

## 검증 절차

1. deleted 데이터가 count에서 제외되는지 확인한다.
2. 타입별 오늘 남은 복습 수가 설정과 무관하게 실제 대상 수를 보여주는지 확인한다.
3. streak 계산의 오늘/어제 시작 케이스를 확인한다.
4. Step 12를 `completed`로 표시하고 summary에 dashboard 집계 완료를 적는다.

## 금지사항

- Controller를 구현하지 마라. 이유: Step 17에서 HTTP API를 구현한다.
- 다른 사용자의 로그를 streak에 포함하지 마라. 이유: 데이터 격리 위반이다.
- frontend 파일을 수정하지 마라. 이유: 백엔드 service step이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

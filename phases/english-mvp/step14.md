# Step 14: backend-review-controller

## Step Contract

- Capability: review REST APIs
- Layer: controller
- Write Scope: `backend/src/main/java/com/english/review/`, `backend/src/main/java/com/english/config/`, `backend/src/test/java/com/english/review/`
- Out of Scope: ReviewService algorithm changes, generate APIs, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*ReviewControllerTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/review/`
- `/backend/src/main/java/com/english/auth/`

## 작업

복습 HTTP API를 구현한다.

필수 구현:
- `GET /api/reviews/today?type=WORD|PATTERN|SENTENCE&exclude=1,2,3`.
- type은 필수이며 WORD/PATTERN/SENTENCE만 허용한다.
- exclude는 선택이며 콤마 구분 reviewItemId 목록으로 파싱한다.
- 응답은 카드 배열이며 itemType, direction, front, back 구조를 API 설계서와 맞춘다.
- `POST /api/reviews/{reviewItemId}` body `{ result }`.
- result는 EASY/MEDIUM/HARD만 허용한다.
- 응답은 nextReviewDate와 intervalDays를 포함한다.
- 현재 인증 사용자 소유 reviewItem만 처리한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*ReviewControllerTest"
cd backend && ./gradlew test
```

## 검증 절차

1. type별 카드 응답 구조를 확인한다.
2. exclude 파라미터 파싱과 결과 제외를 확인한다.
3. 타 사용자 reviewItem 응답 기록이 403인지 확인한다.
4. Step 14를 `completed`로 표시하고 summary에 review API 완료를 적는다.

## 금지사항

- 복습 알고리즘을 controller에 구현하지 마라. 이유: service layer 책임이다.
- SENTENCE RECALL 응답을 추가하지 마라. 이유: PRD와 불일치한다.
- frontend 파일을 수정하지 마라. 이유: 이 step은 백엔드 controller 전용이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

# Step 9: backend-review-service

## Step Contract

- Capability: review deck selection, card assembly, and SM-2 result processing
- Layer: service
- Write Scope: `backend/src/main/java/com/english/review/`, `backend/src/test/java/com/english/review/`
- Out of Scope: HTTP controllers, sentence generation, frontend files, DB migration changes unless mapping bug blocks tests
- Critical Gates: `cd backend && ./gradlew test --tests "*ReviewServiceTest"` and `cd backend && ./gradlew test --tests "*Sm2SchedulerTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/DESIGN.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/review/`
- `/backend/src/main/java/com/english/word/`
- `/backend/src/main/java/com/english/pattern/`
- `/backend/src/main/java/com/english/generate/`
- `/backend/src/main/java/com/english/setting/`

## 작업

복습 service를 TDD로 구현한다.

필수 구현:
- `getTodayReviews(userId, type, excludeIds)`는 user_settings.daily_review_count 기준으로 해당 type N개를 선정한다.
- 선정 조건은 `next_review_date <= today`, `deleted=false`, `item_type=type`, 현재 사용자 소유다.
- 정렬 우선순위는 HARD 먼저, last_reviewed_at 오래된 순, review_count 낮은 순이다.
- 선정 후 탭 내에서 셔플한다.
- WORD RECOGNITION/RECALL, PATTERN RECOGNITION/RECALL, SENTENCE RECOGNITION 카드 front/back DTO를 구성한다.
- SENTENCE front에는 5개 상황 중 하나를 랜덤으로 포함한다.
- `recordResult(userId, reviewItemId, result)`는 소유권을 검증하고 review_logs를 저장한다.
- SM-2 변형 규칙:
  - EASY: interval * ease_factor * 1.3, ease_factor + 0.15
  - MEDIUM: interval * ease_factor, ease_factor 유지
  - HARD: interval 1, ease_factor max(1.3, ease_factor - 0.2)
- next_review_date는 오늘 + 반올림 interval로 계산한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*ReviewServiceTest"
cd backend && ./gradlew test --tests "*Sm2SchedulerTest"
cd backend && ./gradlew test
```

## 검증 절차

1. type별 N개 독립 선정과 exclude 동작을 확인한다.
2. HARD 우선, 오래 안 본 순, review_count 낮은 순을 확인한다.
3. 카드 타입별 front/back 필드가 API 설계와 맞는지 확인한다.
4. Step 9를 `completed`로 표시하고 summary에 ReviewService와 SM-2 테스트 통과를 적는다.

## 금지사항

- Controller를 구현하지 마라. 이유: Step 14에서 HTTP API를 다룬다.
- SENTENCE에 RECALL direction을 만들지 마라. 이유: PRD에서 SENTENCE는 RECOGNITION만 허용한다.
- 타 사용자 reviewItem에 결과를 기록하지 마라. 이유: IDOR 방어 필수 조건이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

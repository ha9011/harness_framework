# Step 11: backend-settings-service

## Step Contract

- Capability: user settings service
- Layer: service
- Write Scope: `backend/src/main/java/com/english/setting/`, `backend/src/test/java/com/english/setting/`
- Out of Scope: HTTP controllers, dashboard aggregation, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*UserSettingServiceTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/setting/`
- `/backend/src/main/java/com/english/auth/`

## 작업

사용자 설정 service를 TDD로 구현한다.

필수 구현:
- `getSettings(userId)`는 설정이 없으면 기본 daily_review_count 10을 생성해서 반환한다.
- `updateSetting(userId, key, value)`는 `daily_review_count`만 허용한다.
- daily_review_count 값은 10, 20, 30만 허용한다.
- 설정은 사용자별로 격리한다.
- ReviewService가 사용할 수 있는 조회 메서드를 제공한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*UserSettingServiceTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 최초 조회 시 기본 설정이 생성되는지 확인한다.
2. 10/20/30 외 값이 거부되는지 확인한다.
3. 사용자별 설정 격리가 지켜지는지 확인한다.
4. Step 11을 `completed`로 표시하고 summary에 UserSettingService 동작을 적는다.

## 금지사항

- 알림, 백업, 내보내기 설정을 구현하지 마라. 이유: MVP 제외 사항이다.
- Controller를 구현하지 마라. 이유: Step 16에서 HTTP API를 구현한다.
- dashboard 통계를 계산하지 마라. 이유: Step 12의 scope다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

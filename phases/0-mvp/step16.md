# Step 16: backend-settings-controller

## Step Contract

- Capability: settings REST APIs
- Layer: controller
- Write Scope: `backend/src/main/java/com/english/setting/`, `backend/src/test/java/com/english/setting/`
- Out of Scope: UserSettingService algorithm changes, notification settings, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*UserSettingControllerTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/API설계서.md`
- `/backend/src/main/java/com/english/setting/`
- `/backend/src/main/java/com/english/auth/`

## 작업

설정 HTTP API를 구현한다.

필수 구현:
- `GET /api/settings`는 현재 사용자 설정 전체를 반환한다.
- `PUT /api/settings/{key}`는 body `{ value }`를 받아 설정을 변경한다.
- MVP에서 허용되는 key는 `daily_review_count`뿐이다.
- value는 문자열 또는 숫자로 들어와도 service validation을 통과한 정수 10/20/30으로 저장한다.
- 허용되지 않는 key/value는 400 에러 응답으로 반환한다.
- 현재 인증 사용자 설정만 조회/수정한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*UserSettingControllerTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 최초 GET에서 기본 설정 반환을 확인한다.
2. PUT daily_review_count 10/20/30 성공을 확인한다.
3. 잘못된 key/value 400 응답을 확인한다.
4. Step 16을 `completed`로 표시하고 summary에 settings API 완료를 적는다.

## 금지사항

- 알림/백업/내보내기 설정을 구현하지 마라. 이유: MVP 제외 사항이다.
- service validation을 controller에서 우회하지 마라. 이유: 비즈니스 규칙은 service에 있어야 한다.
- frontend 파일을 수정하지 마라. 이유: 백엔드 controller step이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

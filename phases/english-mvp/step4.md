# Step 4: backend-schema

## Step Contract

- Capability: learning database schema and JPA entity mappings
- Layer: domain
- Write Scope: `backend/src/main/resources/db/migration/`, `backend/src/main/java/com/english/word/`, `backend/src/main/java/com/english/pattern/`, `backend/src/main/java/com/english/generate/`, `backend/src/main/java/com/english/review/`, `backend/src/main/java/com/english/study/`, `backend/src/main/java/com/english/setting/`, `backend/src/test/java/com/english/`
- Out of Scope: service business logic, HTTP controllers, Gemini HTTP client implementation, frontend files
- Critical Gates: `cd backend && ./gradlew test --tests "*SchemaMigrationTest"` and `cd backend && ./gradlew test --tests "*EntityMappingTest"`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/DESIGN.md`
- `/API설계서.md`
- `/backend/src/main/resources/db/migration/`
- `/backend/src/main/java/com/english/auth/User.java`

## 작업

영어 패턴 학습기 MVP의 DB schema와 JPA Entity를 구현한다.

필수 구현:
- Step 1의 users migration은 중복 작성하지 않는다.
- 다음 테이블 migration을 작성한다: `words`, `patterns`, `pattern_examples`, `generation_history`, `generated_sentences`, `generated_sentence_words`, `sentence_situations`, `study_records`, `study_record_items`, `review_items`, `review_logs`, `user_settings`.
- 모든 사용자 소유 테이블은 `user_id` FK를 가진다.
- soft delete 대상은 `deleted boolean default false`를 가진다.
- `study_record_items`와 `review_items`의 polymorphic association은 DB FK 없이 애플리케이션 검증 전제로 mapping한다.
- `review_items`는 SENTENCE direction이 RECOGNITION만 가능하도록 service에서 검증할 수 있는 enum을 둔다.
- 필요한 index를 migration에 포함한다: deleted, next_review_date, item_type/last_result, generated_sentence_words.word_id, review_logs.reviewed_at.
- JPA Entity와 Repository 기본 interface를 만든다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test --tests "*SchemaMigrationTest"
cd backend && ./gradlew test --tests "*EntityMappingTest"
cd backend && ./gradlew test
```

## 검증 절차

1. 빈 PostgreSQL에서 모든 Flyway migration이 순서대로 통과하는지 확인한다.
2. 각 Entity가 persist/read 가능한지 mapping 테스트로 확인한다.
3. user_id FK와 주요 index가 존재하는지 테스트하거나 migration 검토로 확인한다.
4. Step 4를 `completed`로 표시하고 summary에 migration 범위와 주요 Entity를 적는다.

## 금지사항

- Service 로직을 구현하지 마라. 이유: DB/domain mapping만 완료해야 한다.
- DB unique로 words/patterns 중복을 강제하지 마라. 이유: soft delete 재등록과 사용자별 중복 정책은 앱 레벨 검증이다.
- users migration을 다시 만들지 마라. 이유: Step 1과 중복된다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.

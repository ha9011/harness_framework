# Step 1: User Entity + Repository (TDD)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/backend/src/main/java/com/english/word/Word.java` — Entity 패턴 참고 (soft delete, 생성자, Lombok)
- `/backend/src/main/java/com/english/word/WordRepository.java` — Repository 패턴 참고

## 작업

### 1. User Entity

`backend/src/main/java/com/english/auth/User.java` 생성:

- 필드: `id` (Long, PK, GeneratedValue), `email` (String, UNIQUE, NOT NULL), `password` (String, NOT NULL), `nickname` (String, NOT NULL), `createdAt` (LocalDateTime, NOT NULL)
- `@Entity`, `@Table(name = "users")`
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` + `@Getter` (기존 Entity 패턴과 동일)
- 생성자: `User(String email, String password, String nickname)` — createdAt은 생성자에서 `LocalDateTime.now()` 할당
- email은 DB 레벨 UNIQUE 유지 (사용자 이메일은 글로벌 고유)

### 2. UserRepository

`backend/src/main/java/com/english/auth/UserRepository.java` 생성:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### 3. 테스트

TDD 원칙에 따라 **테스트를 먼저 작성**하라.

`backend/src/test/java/com/english/auth/UserRepositoryTest.java`:
- `@DataJpaTest` 사용 (JPA 계층만 로드)
- 테스트 케이스:
  - User 생성 후 저장 → findByEmail로 조회 성공
  - existsByEmail: 존재하는 이메일 → true
  - existsByEmail: 존재하지 않는 이메일 → false
  - 중복 이메일 저장 시 예외 발생

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

- 전체 테스트 통과 (기존 + 신규)
- User 엔티티가 users 테이블에 매핑됨

## 검증 절차

1. `cd backend && ./gradlew test` 실행
2. UserRepositoryTest의 4개 테스트가 통과하는지 확인
3. 기존 단위 테스트가 영향받지 않았는지 확인
4. User 엔티티가 기존 Entity 패턴(@Getter, @NoArgsConstructor)을 따르는지 확인

## 금지사항

- User 엔티티에 soft delete(deleted 필드)를 추가하지 마라. 이유: 회원 탈퇴 기능은 이번 Phase 범위 밖이다.
- UserService나 AuthService를 이 단계에서 만들지 마라. 이유: Step 3에서 작성한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

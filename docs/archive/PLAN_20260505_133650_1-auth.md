# PLAN: 회원가입/로그인 + 사용자별 데이터 격리 (Phase 1-auth)

## 작업 목표
Spring Security + JWT(HttpOnly Cookie) 기반 회원가입/로그인 구현. 기존 7개 Entity에 user_id FK 추가하여 사용자별 데이터 완전 격리.

## 구현할 기능
1. 회원가입 (이메일 + 비밀번호 + 닉네임) — 가입 즉시 자동 로그인
2. 로그인 (이메일 + 비밀번호) — JWT를 HttpOnly Cookie로 발급
3. 로그아웃 — Cookie 삭제
4. 인증 상태 확인 (GET /api/auth/me)
5. 미인증 시 기존 API 접근 차단 (401)
6. 사용자별 데이터 격리 — 7개 Entity에 user_id FK
7. 프론트엔드 로그인/회원가입 페이지 + AuthGuard + 홈에 닉네임 표시

## 기술적 제약사항
- JWT Access Token만 사용 (Refresh Token 없음, 24시간 만료)
- 기존 DB 데이터 삭제 후 새로 시작
- CorsConfig.java → SecurityConfig로 흡수 (allowCredentials: true)
- user_id FK 추가 대상 (7개): Word, Pattern, GeneratedSentence, GenerationHistory, ReviewItem, StudyRecord, UserSetting
- user_id 불필요 (부모 통해 접근): PatternExample, SentenceSituation, GeneratedSentenceWord, StudyRecordItem
- ReviewLog: reviewItemId가 단순 Long (NOT @ManyToOne) → `@ManyToOne ReviewItem reviewItem`으로 변경하여 JPQL JOIN 가능하게 함
- UNIQUE 제약: DB 레벨 `@Column(unique=true)` 제거 → 앱 레벨에서만 중복 검증 (`existsByUserAndWordAndDeletedFalse`). soft delete된 단어 재등록 시 DB UNIQUE 충돌 방지. Pattern, ReviewItem도 동일
- IDOR 방어: 모든 findById 후 userId 일치 검증 필수 (getDetail, delete, toggleImportant, submitResult 등)
- softDelete: `softDeleteByItemTypeAndItemId`에 userId 조건 추가 필수 (크로스유저 삭제 방지)
- 로그인 실패 응답: "이메일 또는 비밀번호가 올바르지 않습니다" (구분 금지)

## 보안 체크리스트
- [ ] JWT secret: 환경변수 `${JWT_SECRET}` (application.yml 하드코딩 금지)
- [ ] BCrypt strength: 기본 10 사용
- [ ] 로그인 에러: 이메일/비밀번호 구분 없는 동일 메시지
- [ ] IDOR: 모든 리소스 접근 시 userId 검증
- [ ] softDelete: userId 포함 쿼리
- [ ] GlobalExceptionHandler: 스택 트레이스 미노출 (일반 메시지 반환)
- [ ] CORS: allowCredentials(true) + 정확한 origin 지정 (와일드카드 금지)

## 프론트엔드 엣지케이스
- [ ] api.ts 401 응답 → 자동 /login 리다이렉트 (전역 핸들러)
- [ ] 로그아웃 시 모든 페이지 상태 초기화 (이전 사용자 데이터 잔존 방지)
- [ ] AuthGuard 로딩 중 깜빡임 방지 (authChecked=false 동안 로딩 UI 표시, /login 리다이렉트 방지)
- [ ] 비밀번호 검증: onBlur 시점에서 에러 표시 (입력 중 에러 방지)
- [ ] 비밀번호 확인: confirmPassword가 비어있으면 에러 미표시

## 변경 범위 상세

### Entity 생성자 변경 (50곳)
| Entity | 테스트 내 생성 횟수 | 주요 파일 |
|--------|-------------------|----------|
| Word | 16곳 | WordServiceTest(12), ReviewServiceTest(3), GenerateServiceTest(1) |
| Pattern | 8곳 | PatternServiceTest(6), ReviewServiceTest(1), GenerateServiceTest(1) |
| GeneratedSentence | 8곳 | ReviewServiceTest(8) |
| StudyRecord | 13곳 | WordServiceTest(6), PatternServiceTest(3), StudyRecordServiceTest(2), DashboardServiceTest(2) |
| UserSetting | 3곳 | SettingServiceTest(3) |
| ReviewItem | 1곳 | ReviewServiceTest(1) |
| GenerationHistory | 1곳 | GenerateServiceTest(1) |

### Repository 메서드 변경 (22개 메서드)
| Repository | 메서드 수 | 주요 변경 |
|-----------|----------|----------|
| WordRepository | 6 | findAllWithFilters @Query에 user 조건 추가 |
| PatternRepository | 4 | JPA 네이밍에 User 추가 |
| ReviewItemRepository | 4 | @Query 4개에 user 조건 추가 |
| StudyRecordRepository | 4 | findMaxDayNumber @Query에 user 조건 추가 |
| GeneratedSentenceRepository | 1 | @Query JOIN에 user 조건 추가 |
| GenerationHistoryRepository | 1 | JPA 네이밍에 User 추가 |
| ReviewLogRepository | 1 | @ManyToOne 변경 후 reviewItem.user 직접 JOIN |
| UserSettingRepository | 1 | findByUser 추가 |

### Service 메서드 변경 (25개 메서드, 8개 Service)
- 모든 public 메서드에 User 파라미터 추가
- userId 전달 방식: Controller에서 `@AuthenticationPrincipal` → Service에 User 객체 전달
- 특별 주의: SettingService.getOrCreateSetting()의 `findAll().get(0)` → `findByUser()` 변경
- 특별 주의: DashboardService의 `generatedSentenceRepository.count()` → `countByUser()` 변경
- 특별 주의: ReviewService.submitResult()의 `findById()` → 소유권 검증 추가

### 테스트 수정 (22개 파일)
- 단위 테스트 (16개): Entity 생성자 + Service 시그니처 변경 반영
- 통합 테스트 (6개): IntegrationTestBase에 인증 헬퍼 추가, 모든 API 호출에 인증 Cookie 첨부
- Controller 테스트 (7개): standaloneSetup → Service mock 시그니처 변경만 반영 (Security 필터 미적용)

## 테스트 전략
- 기존 테스트 영향: 전체 수정 필요
- **주의: Step 5~7은 상호 의존** — Repository 시그니처 변경 → Service 컴파일 에러 → 중간 빌드 불가. 각 Step에서 해당 계층의 테스트도 함께 수정
- 신규 테스트: JwtProviderTest, AuthServiceTest, AuthControllerTest, AuthIntegrationTest

## Phase/Step 초안

### Step 0: 의존성 및 인프라 설정
- 작업:
  - `build.gradle.kts`에 Spring Security + jjwt 의존성 추가
  - `application.yml`에 jwt.secret, jwt.expiration 설정 추가
- 산출물: `./gradlew build` 성공
- 핵심 파일:
  - `backend/build.gradle.kts`
  - `backend/src/main/resources/application.yml`

### Step 1: User Entity + Repository (TDD)
- 작업:
  - User Entity (id, email UNIQUE, password, nickname, createdAt)
  - UserRepository (findByEmail, existsByEmail)
  - 테스트 작성
- 산출물: `./gradlew test` 성공
- 핵심 파일:
  - `backend/src/main/java/com/english/auth/User.java`
  - `backend/src/main/java/com/english/auth/UserRepository.java`

### Step 2: JwtProvider (TDD)
- 작업:
  - JwtProvider (generateToken, validateToken, getEmailFromToken)
  - 테스트: 생성/검증/추출, 만료, 변조
- 산출물: `./gradlew test` 성공

### Step 3: AuthService + AuthController (TDD)
- 작업:
  - AuthService (signup: BCrypt 해시 + 저장, login: 인증 + JWT)
    - 로그인 실패: "이메일 또는 비밀번호가 올바르지 않습니다" (이메일/비밀번호 구분 금지)
    - SignupRequest: `@Size(min=8) password` (서버 검증)
  - AuthController (POST signup/login/logout, GET me)
  - DTO: SignupRequest, LoginRequest, AuthResponse
  - GlobalExceptionHandler에 인증 예외 추가 + 일반 Exception 메시지 마스킹
- 산출물: `./gradlew test` 성공

### Step 4: SecurityConfig + JwtAuthenticationFilter
- 작업:
  - SecurityConfig (FilterChain, BCryptPasswordEncoder, CORS 통합)
  - JwtAuthenticationFilter (Cookie → JWT → SecurityContext → UserDetails에 User 객체 포함)
  - CorsConfig.java를 SecurityConfig로 흡수
  - 인증 불필요: POST /api/auth/signup, /api/auth/login
  - 인증 필요: 나머지 모든 /api/**
- 산출물: `./gradlew test` 성공 (기존 단위 테스트는 standaloneSetup이므로 영향 없음)

### Step 5: Entity + Repository + Service + Controller userId 적용 (TDD)
> 도메인 단위로 정리하되, **Step 5 전체가 하나의 단위**. 중간 substep에서는 빌드 불가 (callee 시그니처 변경 → caller 컴파일 에러). 5g 완료 시 전체 단위 테스트 통과를 목표로 한다. 아래 순서는 작업 가이드이며 각 substep 완료 시점의 테스트 통과를 보장하지 않음

#### Step 5a: StudyRecord + Setting 도메인 userId 적용 (의존 없음, 최우선)
- Entity: StudyRecord, UserSetting에 `@ManyToOne User user` 추가
- Repository: StudyRecordRepository 4개, UserSettingRepository 1개 수정
  - **dayNumber 수정**: `findMaxDayNumber()` 쿼리에 user 필터 추가 (없으면 사용자별 dayNumber 꼬임)
- Service: StudyRecordService 3개, SettingService 2개 수정
  - **SettingService 버그 수정**: `findAll().get(0)` → `findByUser()`
- Test: StudyRecordServiceTest (2곳), StudyRecordControllerTest, SettingServiceTest (3곳), SettingControllerTest
- 산출물: 5g에서 일괄 검증

#### Step 5b: ReviewItemService userId 적용 (의존 없음)
- Entity: ReviewItem에 `@ManyToOne User user` 추가
  - **UNIQUE 변경**: DB `@UniqueConstraint(item_type, item_id, direction)` 제거 → 앱 레벨 검증
- Repository: ReviewItemRepository 4개 수정 (softDelete 쿼리에 userId 조건 추가)
- Service: ReviewItemService 3개 메서드에 User 파라미터 추가
- Test: 관련 단위 테스트 수정
- 산출물: 5g에서 일괄 검증

#### Step 5c: Word 도메인 userId 적용 (StudyRecordService + ReviewItemService에 의존 → 5a, 5b 완료 후)
- Entity: Word에 `@ManyToOne User user` 추가
  - **UNIQUE 변경**: `@Column(unique=true)` 제거 → 앱 레벨 중복 검증만 사용 (soft delete 재등록 충돌 방지)
- Repository: WordRepository 6개 메서드에 User 파라미터 추가
- Service: WordService 6개 메서드에 User 파라미터 추가
  - **IDOR 방어**: getDetail, delete, toggleImportant에서 findByIdAndUser로 소유권 검증
  - **softDelete 수정**: `softDeleteByItemTypeAndItemId` → `softDeleteByUserAndItemTypeAndItemId`
- Test: WordServiceTest (12곳 Entity 생성 + mock 수정), WordControllerTest (Service mock 수정)
- 산출물: 5g에서 일괄 검증

#### Step 5d: Pattern 도메인 userId 적용 (동일 의존)
- Entity: Pattern에 `@ManyToOne User user` 추가
  - **UNIQUE 변경**: `@Column(unique=true)` 제거 → 앱 레벨 중복 검증만 사용
- Repository: PatternRepository 4개 메서드 수정
- Service: PatternService 4개 메서드 수정
  - **IDOR 방어**: getDetail, delete에서 findByIdAndUser로 소유권 검증
  - **softDelete 수정**: userId 조건 추가
- Test: PatternServiceTest (6곳), PatternControllerTest
- 산출물: 5g에서 일괄 검증

#### Step 5e: Generate 도메인 userId 적용 (Word/Pattern Repository에 의존 → 5c, 5d 완료 후)
- Entity: GeneratedSentence, GenerationHistory에 `@ManyToOne User user` 추가
- Repository: GeneratedSentenceRepository 1개 + `countByUser()` 추가, GenerationHistoryRepository 1개 수정
- Service: GenerateService 4개 메서드 수정
  - **IDOR 방어**: `generateByWord()` — findByIdAndDeletedFalse → findByIdAndUserAndDeletedFalse (소유권 검증)
  - **IDOR 방어**: `generateByPattern()` — 동일하게 소유권 검증
  - **크로스유저 매핑 방지**: `callGeminiAndSave()` 내부 `existsById(wordId)` → `existsByIdAndUser(wordId, user)`
- Test: GenerateServiceTest (3곳 Entity 생성 수정), GenerateControllerTest
- 산출물: 5g에서 일괄 검증

#### Step 5f: Review 나머지 userId 적용 (Setting, Word, Pattern, GeneratedSentence에 의존 → 5a~5e 완료 후)
- Entity: ReviewLog의 `Long reviewItemId` → `@ManyToOne ReviewItem reviewItem`으로 변경 (JPQL JOIN 지원)
- Repository: ReviewLogRepository 1개 수정 (`WHERE r.reviewItem.user = :user` JOIN 쿼리)
- Service: ReviewService 2개 메서드 수정
  - **소유권 검증 추가**: submitResult()에서 findById 후 user 일치 확인
- Test: ReviewServiceTest (12곳 Entity 생성 수정), ReviewControllerTest
- 산출물: 5g에서 일괄 검증

#### Step 5g: Dashboard 도메인 userId 적용 (모든 Repository에 의존 → 최후순위)
- Service: DashboardService 수정
  - `generatedSentenceRepository.count()` → `countByUser()` 변경
  - **학습기록 혼합 방지**: `findTop5ByOrderByCreatedAtDesc()` → `findTop5ByUserOrderByCreatedAtDesc(user)`
- Test: DashboardServiceTest (4곳 Entity 생성 수정), DashboardControllerTest
- 산출물: `./gradlew test` 전체 단위 테스트 통과

### Step 6: IntegrationTestBase 인증 + 통합 테스트
- 작업:
  - IntegrationTestBase에 인증 헬퍼 추가:
    - `signupAndGetCookie()` — 테스트용 사용자 생성 + JWT Cookie 반환
    - 모든 TestRestTemplate 요청에 Cookie 자동 첨부
  - 기존 6개 통합 테스트 수정 (인증 Cookie 사용)
  - AuthIntegrationTest 신규: 가입 → 로그인 → me → 로그아웃 E2E
  - 데이터 격리 테스트: 사용자 A 데이터가 사용자 B에게 안 보이는지 확인
- 산출물: `./gradlew test` + `./gradlew integrationTest` 성공
- 핵심 파일:
  - `backend/src/test/java/com/english/integration/IntegrationTestBase.java`
  - 6개 기존 통합 테스트 파일

### Step 7: 프론트엔드 — 인증 UI + 목업 + 카드 스타일
- 작업:
  - `lib/api.ts` 수정:
    - `credentials: "include"` 추가 (request + uploadRequest 모두)
    - 401 응답 시 `window.dispatchEvent(new Event('unauthorized'))` → /login 자동 리다이렉트
  - `lib/auth-context.tsx` 생성:
    - AuthProvider: 초기 로딩 시 GET /api/auth/me로 인증 확인
    - useAuth(): { user, loading, login, signup, logout }
    - logout: Cookie 삭제 API 호출 + user=null + /login 리다이렉트
    - 'unauthorized' 이벤트 리스너: JWT 만료 시 자동 로그아웃
  - `app/login/page.tsx` 생성 (에러 메시지: "이메일 또는 비밀번호가 올바르지 않습니다")
  - `app/signup/page.tsx` 생성 (비밀번호 확인 포함, 검증은 onBlur 시점)
  - `app/components/AuthGuard.tsx` 생성:
    - loading=true 동안 로딩 UI 표시 (깜빡임 방지, /login 리다이렉트 방지)
    - loading=false + user=null → /login 리다이렉트
  - `app/layout.tsx`에 AuthProvider 래핑 + 로그인/회원가입 시 BottomNav 숨김
  - `app/page.tsx`에 "닉네임님 안녕하세요" 표시
  - 모든 페이지 useEffect: user 변경 시 데이터 재로드 (로그아웃 → 재로그인 시 이전 데이터 방지)
  - **FlipCard 모던 스타일 적용** (`app/components/FlipCard.tsx`):
    - 앞면: `bg-raised` + 강화 shadow
    - 뒷면: `bg-primary-soft` 배경 (#E8D5BE)
    - 참고: `design/screens-review.jsx` 53~56줄
  - **로그인/회원가입 목업** (`design/screens-auth.jsx` — 이미 생성됨, 참고용):
    - Login, Signup 컴포넌트의 레이아웃/카피/아이콘을 프론트엔드 구현 시 참조
    - `design/app.jsx`에 login/signup 네비게이션 추가
- 산출물: `npm run build` + `npm run lint` 성공
- 핵심 파일:
  - `frontend/app/components/FlipCard.tsx`
  - `design/screens-review.jsx` (모던 스타일 참고)
  - `design/screens-auth.jsx` (신규)
  - `design/app.jsx` (네비게이션 추가)

### Step 8: 전체 통합 검증
- 작업:
  - 백엔드 전체 테스트 통과 확인
  - 프론트엔드 빌드 + 린트 통과 확인
  - 수동 E2E: 회원가입 → 단어 등록 → 로그아웃 → 다른 계정 가입 → 데이터 격리 확인
- 산출물: 전체 테스트 통과

## 미결 사항
- 없음

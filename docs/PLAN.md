# PLAN: 영어 패턴 학습기 MVP 구축

## 작업 목표
Next.js 프론트엔드와 Spring Boot 백엔드로 인증, 단어/패턴 등록, Gemini 예문 생성, 간격 반복 복습, 대시보드를 갖춘 영어 패턴 학습기 MVP를 구축한다.

## 구현할 기능
1. 회원가입, 로그인, 로그아웃, 현재 사용자 조회
2. 사용자별 단어/패턴 CRUD와 soft delete
3. 단어/패턴 등록 시 학습 기록과 복습 카드 자동 생성
4. Gemini Vision/Text 기반 단어 추출, 패턴 추출, 예문 생성
5. 예문 생성 이력, 예문-단어 매핑, 예문별 상황 5개 저장
6. 단어/패턴/문장 탭별 간격 반복 복습
7. 홈 대시보드, 학습 기록, 사용자 설정
8. Cozy Cafe & Coffee Tree 모바일 UI

## 기술적 제약사항
- 백엔드 코드는 반드시 `backend/` 아래에 작성한다.
- 프론트엔드 코드는 반드시 `frontend/` 아래에 작성한다.
- 루트 디렉토리에 직접 소스 코드를 두지 않는다.
- 새 기능은 테스트를 먼저 작성하고 구현한다.
- DB 스키마 변경은 Flyway migration으로 관리한다.
- JPA Entity는 Flyway migration과 일치해야 한다.
- 외부 Gemini 연동은 클라이언트 인터페이스로 분리하고, 서비스 테스트에서는 fake 구현을 사용한다.
- 실제 Gemini 네트워크 호출을 필수 테스트로 만들지 않는다.
- 업로드 이미지는 추출 요청에만 사용하고 저장하지 않는다.
- 모든 사용자 데이터 접근은 현재 로그인 사용자의 `user_id`로 제한한다.
- `integration-hardening`을 제외한 step에서는 `backend/`와 `frontend/`를 동시에 수정하지 않는다.

## 테스트 전략
- 기존 테스트 영향: 현재 앱 코드가 없으므로 신규 테스트 중심으로 작성한다.
- 신규 백엔드 테스트:
  - AuthService/AuthController: 가입, 로그인, 실패 메시지, Cookie 발급, me, logout
  - Security: 미인증 401, 타 사용자 리소스 403
  - Word/Pattern Service: 중복 검사, soft delete, 학습 기록 생성, review_items 생성
  - GeminiClient: fake server/client 기반 request 구성, schema parsing, retry/fallback
  - GenerateService: 단어 우선순위, 패턴 조합, wordId 검증, 상황 5개 저장
  - ReviewService: 타입별 선정, exclude, HARD 우선, SM-2 계산
  - Dashboard/Settings/Study: 통계, streak, 설정 반영
- 신규 프론트엔드 테스트:
  - 인증 폼 검증과 리다이렉트
  - 단어/패턴 등록 폼과 API error 표시
  - 예문 생성 로딩/결과/빈 상태
  - 복습 탭, 카드 flip, 응답 버튼, 완료 후 다시보기/추가복습
  - 설정 변경
- 테스트 수정이 필요한 Step: 기존 앱 테스트가 없으므로 해당 없음.

## Phase/Step 초안

### Step 0: backend-scaffold
- Layer: domain
- 작업: Spring Boot Gradle 프로젝트를 `backend/`에 생성하고 PostgreSQL, Flyway, JPA, Security, Validation, Testcontainers 기반을 구성한다.
- 산출물: 실행 가능한 백엔드 skeleton, health endpoint, 공통 에러 응답, 기본 테스트.
- 검증: `cd backend && ./gradlew test`가 PostgreSQL Testcontainers와 기본 context load를 통과한다.

### Step 1: backend-auth-domain
- Layer: domain
- 작업: users migration, User Entity/Repository, JWT 설정 값, 인증 DTO를 TDD로 구현한다.
- 산출물: User Entity, UserRepository, 인증 관련 DTO, users migration 연동 테스트.
- 검증: 이메일 unique, BCrypt 저장 필드, users migration 테스트 통과.

### Step 2: backend-auth-service
- Layer: service
- 작업: AuthService, JwtProvider, JwtAuthenticationFilter에서 회원가입, 로그인, 토큰 검증, 현재 사용자 조회 로직을 TDD로 구현한다.
- 산출물: 가입/로그인 서비스, JWT 발급/검증, 인증 사용자 추출 로직.
- 검증: 회원가입 자동 로그인, 로그인 실패 통합 메시지, JWT 만료/검증 테스트 통과.

### Step 3: backend-auth-controller
- Layer: controller
- 작업: AuthController와 SecurityConfig를 구현하고 인증 예외 응답을 HTTP 계약에 맞춘다.
- 산출물: `/api/auth/signup`, `/api/auth/login`, `/api/auth/logout`, `/api/auth/me`.
- 검증: HttpOnly Cookie, logout Cookie 삭제, me, 미인증 401 MockMvc 테스트 통과.

### Step 4: backend-schema
- Layer: domain
- 작업: words, patterns, pattern_examples, generation_history, generated_sentences, generated_sentence_words, sentence_situations, study_records, study_record_items, review_items, review_logs, user_settings migration과 Entity를 작성한다. users migration은 Step 1에서 이미 생성했으므로 중복 작성하지 않는다.
- 산출물: Flyway migration, JPA Entity, Repository 기본 쿼리.
- 검증: migration이 빈 PostgreSQL에서 적용되고 Entity mapping 테스트가 통과한다.

### Step 5: backend-learning-service
- Layer: service
- 작업: 단어/패턴 CRUD, 벌크 단어 저장, 중요 토글, 검색/필터/페이지네이션, soft delete, 학습 기록/review_items 자동 생성을 TDD로 구현한다.
- 산출물: WordService, PatternService, StudyRecordService, ReviewItem 생성 도우미.
- 검증: 중복 스킵, 사용자별 독립 등록, soft delete 연쇄 처리, 학습 기록 생성 테스트 통과.

### Step 6: backend-learning-controller
- Layer: controller
- 작업: 단어/패턴 HTTP API와 요청 검증, 페이지네이션 응답, 사용자 소유권 실패 응답을 구현한다.
- 산출물: `/api/words`, `/api/patterns` API.
- 검증: 단어/패턴 CRUD, 벌크 저장 응답, 중요 토글, 타 사용자 접근 403 MockMvc 테스트 통과.

### Step 7: backend-gemini-client
- Layer: external-client
- 작업: Gemini Vision/Text client 인터페이스와 구현체, structured JSON schema, retry, fallback 정책을 구현한다.
- 산출물: 단어 보강, 단어 이미지 추출, 패턴 이미지 추출, 예문 생성 client 메서드.
- 검증: 실제 네트워크 없이 fake provider/fake server로 request 구성, API key 주입, JSON parsing, 실패 재시도 테스트 통과.

### Step 8: backend-generate-service
- Layer: service
- 작업: 예문 생성, 단어/패턴 상세 전용 생성, 생성 이력 저장, 예문-단어 매핑, 상황 5개 저장, SENTENCE review_items 생성을 구현한다.
- 산출물: GenerateService와 생성 관련 Repository 쿼리.
- 검증: 단어 우선순위, wordId 검증, 상황 5개 저장, SENTENCE review item 생성 테스트 통과.

### Step 9: backend-review-service
- Layer: service
- 작업: 타입별 오늘 복습 카드 선정, exclude 추가 복습, 카드 앞/뒤 구성, SM-2 응답 처리를 구현한다.
- 산출물: ReviewService, SM-2 계산 로직, 카드 DTO assembler.
- 검증: 타입별 복습 선정, HARD 우선, exclude, WORD/PATTERN 양방향, SENTENCE 인식 카드, SM-2 계산 테스트 통과.

### Step 10: backend-study-service
- Layer: service
- 작업: 학습 기록 생성/조회, day_number 계산, 날짜별 학습 항목 조회를 구현한다.
- 산출물: StudyRecordService 조회 기능과 학습 기록 DTO assembler.
- 검증: day_number, 날짜 역순 조회, 등록 항목 중복 방지 테스트 통과.

### Step 11: backend-settings-service
- Layer: service
- 작업: 사용자 설정 조회/변경과 기본 설정 lazy creation을 구현한다.
- 산출물: UserSettingService.
- 검증: 최초 조회 시 기본 daily_review_count 생성, 10/20/30 validation, 사용자별 설정 격리 테스트 통과.

### Step 12: backend-dashboard-service
- Layer: service
- 작업: 홈 대시보드 통계, 오늘 남은 복습 수, 최근 학습 기록, streak 계산을 구현한다.
- 산출물: DashboardService.
- 검증: wordCount, patternCount, sentenceCount, todayReviewRemaining, recentStudyRecords, streak 계산 테스트 통과.

### Step 13: backend-generate-controller
- Layer: controller
- 작업: 예문 생성과 생성 이력 HTTP API, 요청 검증, 에러 응답을 구현한다.
- 산출물: `/api/generate`, `/api/generate/word`, `/api/generate/pattern`, `/api/generate/history`.
- 검증: Generate API MockMvc 테스트 통과.

### Step 14: backend-review-controller
- Layer: controller
- 작업: 오늘 복습 카드 조회와 복습 결과 기록 HTTP API, 요청 검증, 에러 응답을 구현한다.
- 산출물: `/api/reviews/today`, `/api/reviews/{id}`.
- 검증: Review API MockMvc 테스트 통과.

### Step 15: backend-study-controller
- Layer: controller
- 작업: 학습 기록 조회 HTTP API, 페이지네이션 응답, 인증 사용자 범위 제한을 구현한다.
- 산출물: `/api/study-records`.
- 검증: Study API MockMvc 테스트 통과.

### Step 16: backend-settings-controller
- Layer: controller
- 작업: 설정 조회/변경 HTTP API, 허용 key/value validation, 인증 사용자 범위 제한을 구현한다.
- 산출물: `/api/settings`, `/api/settings/{key}`.
- 검증: Settings API MockMvc 테스트 통과.

### Step 17: backend-dashboard-controller
- Layer: controller
- 작업: 홈 대시보드 HTTP API와 응답 DTO를 구현한다.
- 산출물: `/api/dashboard`.
- 검증: Dashboard API MockMvc 테스트 통과.

### Step 18: frontend-scaffold
- Layer: frontend-view
- 작업: Next.js App Router 프로젝트를 `frontend/`에 생성하고 TypeScript strict, Tailwind CSS, shadcn/ui, lucide-react, TanStack Query, React Hook Form, Zod, Vitest, Testing Library, MSW를 구성한다.
- 산출물: 앱 shell, Cozy Cafe design tokens, API client, query provider, 기본 layout.
- 검증: `npm run test`, `npm run lint`, `npm run build` 통과.

### Step 19: frontend-auth-shell
- Layer: frontend-view
- 작업: 로그인/회원가입 화면, AuthProvider, 보호 라우트, 로그아웃, 미로그인 리다이렉트를 구현한다.
- 산출물: `/login`, `/signup`, 인증 상태 기반 app shell.
- 검증: MSW 기반 signup/login/me/logout 흐름과 리다이렉트 테스트 통과.

### Step 20: frontend-home-dashboard
- Layer: frontend-view
- 작업: 홈 대시보드를 Cozy Cafe & Coffee Tree UI로 구현한다.
- 산출물: `/`.
- 검증: dashboard loading/error/empty/data state, 복습 시작 이동, 최근 학습 목록 표시 테스트 통과.

### Step 21: frontend-history-screen
- Layer: frontend-view
- 작업: 날짜별 학습 기록 화면을 구현한다.
- 산출물: `/history`.
- 검증: history pagination, Day N 표시, 등록 단어/패턴 chip 표시 테스트 통과.

### Step 22: frontend-settings-screen
- Layer: frontend-view
- 작업: 하루 복습 개수 설정 화면을 구현한다.
- 산출물: `/settings`.
- 검증: settings 조회, 10/20/30 선택, 저장 mutation, validation error 표시 테스트 통과.

### Step 23: frontend-word-screens
- Layer: frontend-view
- 작업: 단어 목록/등록/상세 화면과 단건, JSON, 이미지 추출 확인 흐름을 구현한다.
- 산출물: `/words`, `/words/[id]`.
- 검증: 단어 등록 폼 검증, 벌크 결과 표시, 검색/필터, 중요 토글, 상세 예문 추가 진입 테스트 통과.

### Step 24: frontend-pattern-screens
- Layer: frontend-view
- 작업: 패턴 목록/등록/상세 화면과 직접 입력, 이미지 추출 확인 흐름을 구현한다.
- 산출물: `/patterns`, `/patterns/[id]`.
- 검증: 패턴 등록 폼 검증, 교재 예문 표시, 패턴 예문 생성 진입 테스트 통과.

### Step 25: frontend-generate-screen
- Layer: frontend-view
- 작업: 난이도/개수 기반 예문 생성 화면과 생성 결과 아코디언을 구현한다.
- 산출물: `/generate`.
- 검증: 난이도/개수 선택, Gemini 로딩/결과/에러, 상황/해석/태그 표시 테스트 통과.

### Step 26: frontend-review-screen
- Layer: frontend-view
- 작업: 단어/패턴/문장 탭별 복습 플립 카드 화면을 구현한다.
- 산출물: `/review`, 완료 후 처음부터 다시, 추가 복습 UI.
- 검증: 카드 flip, 탭별 덱, 진행률, EASY/MEDIUM/HARD mutation, 다시보기/추가복습 테스트 통과.

### Step 27: integration-hardening
- Layer: integration-hardening
- 작업: 프론트/백엔드 API 계약, CORS Cookie, 에러 메시지, 빈 상태, 모바일 레이아웃, 루트 npm script를 검증하고 필요한 최소 조정을 한다.
- 산출물: 통합 실행 가능한 MVP.
- 검증: `npm run test`, `npm run lint`, `npm run build`와 핵심 수동 시나리오가 통과한다.

## 미결 사항
- 실제 Gemini API 호출 검증은 `GEMINI_API_KEY`가 있는 환경에서만 수동 확인한다.
- 배포 대상은 아직 확정하지 않는다.
- 이메일 인증, 비밀번호 재설정, 알림 기능은 MVP에서 제외한다.

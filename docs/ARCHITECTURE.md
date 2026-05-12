# 아키텍처

## 디렉토리 구조
```
harness_framework/
├── docker-compose.yml          # PostgreSQL 16
├── backend/                    # Spring Boot + JPA
│   └── src/main/java/com/english/
│       ├── auth/               # 인증 (회원가입/로그인/JWT)
│       ├── word/               # 단어 CRUD + Gemini 보강
│       ├── pattern/            # 패턴 CRUD + 이미지 추출
│       ├── generate/           # 예문 생성 + GeminiClient
│       ├── review/             # 복습 (SM-2 + 카드 선정)
│       ├── study/              # 학습 기록
│       ├── dashboard/          # 대시보드 통계
│       ├── setting/            # 사용자 설정
│       └── config/             # SecurityConfig, GlobalExceptionHandler
├── frontend/                   # Next.js (App Router)
│   └── app/
│       ├── page.tsx            # 홈 대시보드
│       ├── login/              # 로그인 페이지
│       ├── signup/             # 회원가입 페이지
│       ├── words/              # 단어 목록 + 상세
│       ├── patterns/           # 패턴 목록 + 상세
│       ├── generate/           # 예문 생성
│       ├── review/             # 복습 카드 ([단어][패턴][문장] 탭)
│       ├── history/            # 학습 기록
│       ├── settings/           # 설정
│       ├── components/         # 공통 컴포넌트 (AuthGuard 포함)
│       └── lib/
│           ├── api.ts          # 백엔드 API 호출 (credentials: include)
│           ├── auth-context.tsx # 인증 상태 Context (AuthProvider, useAuth)
│           └── saved-email.ts  # 이메일 저장 localStorage 헬퍼
├── design/                     # 디자인 목업 (JSX + HTML)
├── docs/                       # 프로젝트 문서
├── phases/                     # harness 실행 메타데이터
└── harness/                    # harness 프레임워크
```

## 패턴
- **백엔드**: 도메인별 패키지 (auth/, word/, pattern/, generate/, review/, study/, setting/)
  - Entity → Repository → Service → Controller → DTO 계층
  - TDD: ServiceTest → Service, ControllerTest → Controller 순서
  - 인증: JwtAuthenticationFilter가 Cookie에서 JWT 추출 → SecurityContext 설정
- **프론트엔드**: Next.js App Router
  - 페이지 컴포넌트: Server Component 기본
  - 인터랙션 필요한 곳(카드 플립, 탭 전환): Client Component ("use client")
  - API 호출: lib/api.ts에서 fetch 래핑 (credentials: "include"로 Cookie 자동 전송)
  - 인증 상태: React Context (AuthProvider) → useAuth() hook으로 접근
  - 라우트 보호: AuthGuard 컴포넌트 — 미로그인 시 /login으로 리다이렉트

## 데이터 흐름
```
[프론트엔드]                    [백엔드]                    [외부]
사용자 입력                                               
  ↓                                                      
Next.js Client Component                                  
  ↓ fetch(credentials: include)                          
  → → → →  JwtAuthenticationFilter (Cookie → JWT 검증)    
              ↓ SecurityContext 설정                      
              ↓ Spring Boot Controller                   
              ↓ Service                                  
              ↓ (Gemini 필요시) → → GeminiClient → → Gemini API
              ↓                           ← ← JSON 응답
              ↓ Repository                                
              ↓ JPA → PostgreSQL                         
              ↓                                          
           ← ← ← ← JSON 응답                            
  ↓                                                      
UI 업데이트                                               
```

### 인증 흐름
```
[회원가입/로그인]
POST /api/auth/signup or /api/auth/login
  → AuthController → AuthService → UserRepository
  → JWT 생성 (JwtProvider)
  ← Set-Cookie: token=<jwt>; HttpOnly; Path=/api; SameSite=Lax; Max-Age=604800

[인증된 요청]
GET/POST /api/** (Cookie 자동 전송)
  → JwtAuthenticationFilter: Cookie에서 token 추출 → JWT 검증 → SecurityContext 설정
  → Controller 진입

[미인증 요청]
GET/POST /api/** (Cookie 없음)
  → JwtAuthenticationFilter: 토큰 없음 → SecurityContext 미설정
  → 401 Unauthorized
```

## 환경 설정
```
# 환경 변수 (backend 실행 전 필수)
GEMINI_API_KEY=your-api-key-here

# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/english_app
    username: app
    password: app1234
  jpa:
    hibernate:
      ddl-auto: update    # 개발: Entity 기반 자동 스키마 생성
    show-sql: true

gemini:
  api-key: ${GEMINI_API_KEY}

# JWT (SecurityConfig에서 관리)
jwt:
  secret: ${JWT_SECRET:dev-secret-key-minimum-32-characters}
  expiration: 604800000  # 7일

# CORS (SecurityConfig에서 관리, CorsConfig.java → 흡수)
허용 origin: http://localhost:3000
허용 methods: GET, POST, PUT, PATCH, DELETE
allowCredentials: true  # HttpOnly Cookie 전송 필수
```

## 데이터 모델 (13개 테이블)
> 상세 스키마는 DESIGN.md 참조. 여기는 관계 요약만.

```
users (email UNIQUE, password BCrypt, nickname)
  │
  ├── words ──┬── generated_sentence_words ──── generated_sentences ──┬── sentence_situations (5개/예문)
  │           │                                        │              │
  │           │                                  generation_history    │
  │           │                                        │              │
  ├── patterns ──── pattern_examples (교재 예문 10개)    │              │
  │                                                    │              │
  ├── study_records ──── study_record_items (WORD/PATTERN)             │
  │                                                                    │
  ├── review_items ──── review_logs ← SM-2 결과 기록                    │
  │     ↑ (WORD/PATTERN: 양방향, SENTENCE: RECOGNITION만)              │
  │     └── 자동 생성 (단어/패턴/예문 등록 시)                            │
  │                                                                    │
  └── user_settings (daily_review_count 등)

user_id FK 추가 대상 (7개): words, patterns, generated_sentences,
  generation_history, review_items, study_records, user_settings
자식 Entity (부모를 통해 접근, user_id 불필요): pattern_examples,
  sentence_situations, generated_sentence_words, study_record_items
review_logs: reviewItemId를 @ManyToOne ReviewItem으로 변경 (streak JPQL JOIN 지원)

UNIQUE 제약 변경:
  words: DB @Column(unique=true) 제거 → 앱 레벨 중복 검증 (soft delete 재등록 호환)
  patterns: 동일하게 DB UNIQUE 제거 → 앱 레벨 검증
  review_items: DB UNIQUE 제거 → 앱 레벨 검증

⚠️ review_items, study_record_items는 Polymorphic Association (item_type + item_id)
   → DB FK 없음, Service에서 item_id 유효성 검증 + userId 소유권 검증 필수
```

## 에러 처리 흐름
```
[프론트엔드]                [백엔드]                           [외부]

사용자 요청
  ↓ fetch()
  → → → →  Controller
              ↓ Service
              ↓ → → GeminiClient → → Gemini API
              ↓                      ← ← 실패 (timeout/500)
              ↓ GeminiClient: 재시도 1회(1초 후) → 재시도 2회(3초 후) → 실패 확정
              ↓
              ├─ 단어 보강 실패 → 보강 없이 저장 (word+meaning만)
              ├─ 이미지 추출 실패 → 502 + AI_SERVICE_ERROR
              └─ 예문 생성 실패 → 502 + AI_SERVICE_ERROR
              ↓
           ← ← ← ← JSON 에러 응답
  ↓
프론트: 에러 토스트 표시 + 재시도 안내

GlobalExceptionHandler:
  - DuplicateException → 409 DUPLICATE
  - AuthenticationException → 401 UNAUTHORIZED
  - ForbiddenException → 403 FORBIDDEN (IDOR — 다른 사용자 리소스 접근)
  - EmptyRequestException → 400 EMPTY_REQUEST
  - NoWordsException → 400 NO_WORDS
  - NoPatternsException → 400 NO_PATTERNS
  - InvalidImageException → 400 INVALID_IMAGE_FORMAT
  - GeminiException → 502 AI_SERVICE_ERROR
  - 기타 → 500 INTERNAL_ERROR
```

## 상태 관리
- **서버 상태**: Spring Boot + JPA (PostgreSQL). 모든 비즈니스 데이터는 DB에 저장
- **인증 상태**: React Context (AuthProvider) → useAuth() hook
  - 초기 로딩 시 GET /api/auth/me로 인증 상태 확인 (Cookie 자동 전송)
  - user: { id, email, nickname } | null
  - AuthGuard: user가 null이면 /login으로 리다이렉트
- **클라이언트 상태**: React useState/useReducer
  - 카드 플립 상태, 탭 선택, 폼 입력 등 UI 상태만 클라이언트에서 관리
  - "처음부터 다시" 복습 시 기존 카드 데이터를 클라이언트에서 재사용 (API 재호출 X)
- **localStorage**: 이메일 저장 (saved-email.ts 헬퍼). 로그인 페이지에서 체크박스로 제어

# 아키텍처

## 디렉토리 구조
```
harness_framework/
├── docker-compose.yml          # 개발용: PostgreSQL 16
├── docker-compose.prod.yml     # 운영용: postgres + backend + frontend + nginx 4 서비스
├── .env.example                # 운영 시크릿 템플릿 (실제 .env는 미니PC에만 존재)
├── nginx/
│   └── conf.d/default.conf     # 리버스 프록시: /api/* → backend, / → frontend
├── .github/workflows/
│   └── deploy.yml              # main push → GHCR 빌드 + SSH 배포
├── backend/                    # Spring Boot + JPA
│   ├── Dockerfile              # multi-stage: gradle 빌더 + JRE 런타임
│   ├── .dockerignore
│   └── src/main/
│       ├── resources/
│       │   ├── application.yml
│       │   ├── application-local.yml
│       │   ├── application-prod.yml   # 운영 profile (환경변수만 받음, 기본값 없음)
│       │   └── logback-spring.xml     # 프로파일별 로깅 설정 (local/prod/default)
│       └── java/com/english/
│           ├── auth/               # 인증 (회원가입/로그인/JWT)
│           ├── word/               # 단어 CRUD + Gemini 보강
│           ├── pattern/            # 패턴 CRUD + 이미지 추출
│           ├── generate/           # 예문 생성 + GeminiClient
│           ├── review/             # 복습 (SM-2 + 카드 선정)
│           ├── study/              # 학습 기록
│           ├── dashboard/          # 대시보드 통계
│           ├── setting/            # 사용자 설정
│           └── config/             # SecurityConfig, GlobalExceptionHandler, MdcLoggingFilter
├── frontend/                   # Next.js (App Router)
│   ├── Dockerfile              # multi-stage: deps → builder → runner (Next standalone)
│   ├── .dockerignore
│   ├── next.config.ts          # output: "standalone"
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
│       ├── components/         # 공통 컴포넌트 (AuthGuard, CremaLoader, CoffeeSpinner 포함)
│       └── lib/
│           ├── api.ts          # 백엔드 API 호출 (BASE_URL="/api" 상대경로, credentials: include)
│           ├── auth-context.tsx # 인증 상태 Context (AuthProvider, useAuth)
│           └── saved-email.ts  # 이메일 저장 localStorage 헬퍼
├── design/                     # 디자인 목업 (JSX + HTML)
├── docs/                       # 프로젝트 문서 (DEPLOYMENT.md 포함)
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
  → → → →  MdcLoggingFilter (traceId 세팅 + 요청 시작 로그)
              ↓                                          
              JwtAuthenticationFilter (Cookie → JWT 검증)  
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
  → MdcLoggingFilter: traceId 세팅 + 요청 시작 로그
  → JwtAuthenticationFilter: Cookie에서 token 추출 → JWT 검증 → SecurityContext 설정
  → Controller 진입
  ← MdcLoggingFilter: 응답 완료 로그 (status + 처리시간) + MDC.clear()

[미인증 요청]
GET/POST /api/** (Cookie 없음)
  → MdcLoggingFilter: traceId 세팅 + 요청 시작 로그
  → JwtAuthenticationFilter: 토큰 없음 → SecurityContext 미설정
  → 401 Unauthorized (SecurityConfig authenticationEntryPoint에서 직접 응답)
  ← MdcLoggingFilter: 응답 완료 로그 (401 + 처리시간) + MDC.clear()
```

## 로깅 구조
```
logback-spring.xml (프로파일별 통합 관리)
├── local 프로파일
│   ├── 컬러 콘솔 출력 (파일 X)
│   ├── SQL 쿼리 (org.hibernate.SQL=DEBUG)
│   └── 바인드 파라미터 (org.hibernate.orm.jdbc.bind=TRACE)
├── prod 프로파일
│   ├── JSON 콘솔 (LogstashEncoder → ELK/CloudWatch)
│   ├── logs/app.log (전체 로그, 30일 롤링, 3GB 상한)
│   ├── logs/error.log (ERROR 이상, 90일 보관)
│   ├── SQL 쿼리 + 바인드 파라미터 출력
│   └── 프레임워크 로그 WARN (노이즈 차단)
└── default 프로파일
    └── 컬러 콘솔 INFO

요청 추적 흐름:
  MdcLoggingFilter (@Order HIGHEST_PRECEDENCE)
    → traceId(UUID 8자리) MDC 세팅
    → 요청 시작 로그
    → Security 필터 체인 → JwtAuthenticationFilter → Controller
    → 응답 완료 로그 (status + 처리시간)
    → MDC.clear()
```

## 환경 설정
```
# 환경 변수 (backend 실행 전 필수)
GEMINI_API_KEY=your-api-key-here

# backend/src/main/resources/application.yml (개발 공통)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/english_app
    username: app
    password: app1234
  jpa:
    hibernate:
      ddl-auto: validate    # 개발: Entity 기반 자동 스키마 검증
    # show-sql 제거: logback의 org.hibernate.SQL 로거로 대체

gemini:
  api-key: ${GEMINI_API_KEY}

# JWT (SecurityConfig에서 관리)
jwt:
  secret: ${JWT_SECRET:dev-secret-key-that-is-at-least-32-characters-long}
  expiration: 604800000  # 7일

# CORS (SecurityConfig에서 관리, CorsConfig.java → 흡수)
허용 origin: http://localhost:3000
허용 methods: GET, POST, PUT, PATCH, DELETE
allowCredentials: true  # HttpOnly Cookie 전송 필수

# backend/src/main/resources/application-prod.yml (운영 profile, 환경변수만 받음)
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}             # jdbc:postgresql://postgres:5432/english_app
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
server:
  forward-headers-strategy: framework         # Nginx X-Forwarded-* 신뢰 (HTTPS 인식)
  tomcat:
    remoteip:
      remote-ip-header: X-Forwarded-For
      protocol-header: X-Forwarded-Proto
gemini:
  api-key: ${GEMINI_API_KEY}
  model: ${GEMINI_MODEL:gemini-2.5-flash}
jwt:
  secret: ${JWT_SECRET}                       # 기본값 없음 (fail-fast)
  expiration: 604800000
```

## 배포 토폴로지 (운영)
```
[사용자 브라우저]
  │ HTTPS
  ▼
[Cloudflare Proxy] — Flexible SSL 모드 (브라우저↔CF: HTTPS, CF↔origin: HTTP)
  │ HTTP, X-Forwarded-Proto: https
  ▼
[공유기 80 포트포워딩]
  │
  ▼
[미니PC: docker-compose.prod.yml]
  ┌─────────────────────────────────────────────────────────────┐
  │ network: internal (도커 브리지)                              │
  │                                                             │
  │  nginx:80 (외부 노출)                                       │
  │    ├─ location /api/  → proxy_pass http://backend:8080;     │
  │    └─ location /      → proxy_pass http://frontend:3000;    │
  │                                                             │
  │  backend (Spring Boot, SPRING_PROFILES_ACTIVE=prod)         │
  │    └─ /opt/harness/logs:/app/logs (호스트 바인드 마운트)     │
  │                                                             │
  │  frontend (Next.js standalone, NODE_ENV=production)         │
  │                                                             │
  │  postgres (PostgreSQL 16, pgdata named volume)              │
  │    └─ healthcheck: pg_isready                               │
  └─────────────────────────────────────────────────────────────┘
       ▲
       │ docker login + pull
       ▼
  ghcr.io/ha9011/harness_framework-{backend,frontend}:latest|<sha>
```

## CI/CD 흐름 (GitHub Actions)
```
[개발자] git push origin main
  │
  ▼
[GitHub Actions: .github/workflows/deploy.yml]
  │
  ├─ job: build-backend (병렬)
  │    docker buildx build → push GHCR :latest + :<sha>
  │    cache: type=gha, scope=backend
  │
  ├─ job: build-frontend (병렬)
  │    docker buildx build → push GHCR :latest + :<sha>
  │    cache: type=gha, scope=frontend
  │
  └─ job: deploy (needs: [build-backend, build-frontend])
       appleboy/ssh-action: secrets {SSH_HOST, SSH_USER, SSH_PRIVATE_KEY, SSH_PORT}
       script:
         cd /opt/harness
         git pull --ff-only
         docker compose -f docker-compose.prod.yml pull
         docker compose -f docker-compose.prod.yml up -d
         docker image prune -f
```

## 운영 시크릿
```
미니PC /opt/harness/.env (chmod 600, gitignore)
├── POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD   # 새로 생성 (openssl rand)
├── GEMINI_API_KEY                                    # 운영용 신규 발급
├── GEMINI_MODEL=gemini-2.5-flash
└── JWT_SECRET                                        # 새로 생성 (openssl rand)

GitHub Repository Secrets
├── SSH_HOST       # 미니PC 공인 IP 또는 도메인
├── SSH_USER       # 미니PC 로그인 사용자
├── SSH_PORT       # 22 (또는 변경한 포트)
└── SSH_PRIVATE_KEY  # ~/.ssh/gha_deploy 개인키 전체

GHCR 인증
└── 패키지 가시성 public 권장 (또는 미니PC에서 PAT로 docker login ghcr.io)
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

GlobalExceptionHandler (로깅 레벨):
  4xx — log.warn + 스택트레이스 (클라이언트 오류, 소스 위치 추적용):
  - DuplicateException → 409 DUPLICATE
  - AuthenticationException → 401 UNAUTHORIZED
  - ForbiddenException → 403 FORBIDDEN (IDOR — 다른 사용자 리소스 접근)
  - EmptyRequestException → 400 EMPTY_REQUEST
  - NoWordsException → 400 NO_WORDS
  - NoPatternsException → 400 NO_PATTERNS
  - NotFoundException → 404 NOT_FOUND
  - InvalidImageException → 400 INVALID_IMAGE_FORMAT
  - MethodArgumentNotValidException → 400 BAD_REQUEST
  5xx — log.error + 스택트레이스 (서버 장애, 즉시 확인):
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

# Architecture Decision Records

## 철학
MVP 속도 최우선. TDD 기반 단계적 구현. 설계 문서(DESIGN.md)가 단일 진실 소스(SSOT).

---

### ADR-001: Next.js App Router + Spring Boot 분리 아키텍처
**결정**: 프론트엔드는 Next.js (App Router), 백엔드는 Spring Boot + JPA로 분리
**이유**: 프론트/백 독립 개발 가능. Spring Boot의 JPA + PostgreSQL 생태계가 성숙. Next.js의 App Router가 모바일 반응형에 적합
**트레이드오프**: 모노리스 대비 배포 복잡도 증가. CORS 설정 필요

### ADR-002: PostgreSQL 16 + Docker
**결정**: PostgreSQL 16을 Docker 컨테이너로 운영
**이유**: 로컬 개발 환경 일관성. docker-compose up 한 줄로 DB 기동. TestContainers로 통합 테스트 지원
**트레이드오프**: Docker 의존성 추가. SQLite 대비 초기 세팅 비용

### ADR-003: Gemini API (Vision + Text Generation)
**결정**: AI 기능 전체를 Google Gemini API로 통일
**이유**: Vision API(이미지 추출) + Text Generation(예문 생성/보강)을 하나의 API로 해결. response_mime_type으로 JSON 응답 강제 가능
**트레이드오프**: Gemini 의존도 높음. 장애 시 핵심 기능 마비 → fallback 정책 수립 (보강 실패 시 저장, 초기 1회 + 재시도 2회 = 총 3회, exponential backoff 1초→3초)

### ADR-004: Polymorphic Association (review_items, study_record_items)
**결정**: item_type + item_id 패턴으로 다형성 구현. DB FK 대신 애플리케이션 레벨 검증
**이유**: WORD/PATTERN/SENTENCE 3가지 타입을 하나의 테이블로 관리하여 복습 로직 단순화
**트레이드오프**: DB 레벨 FK 제약 불가 → 고아 레코드 위험. MVP에서 허용, 장기적으로 테이블 분리 검토

### ADR-005: SM-2 기반 커스텀 간격 반복
**결정**: 원본 SM-2(6단계)를 3단계(EASY/MEDIUM/HARD)로 단순화
**이유**: 사용자에게 6단계 선택은 부담. 3단계(기억남/애매/모름)가 직관적
**트레이드오프**: 원본 SM-2 대비 정밀도 감소. "SM-2"라 명칭하지만 커스텀 변형임을 문서화

### ADR-006: 복습 탭 분리 (단어/패턴/문장 독립)
**결정**: 복습 화면을 [단어][패턴][문장] 탭으로 분리. 각 탭에서 독립적으로 N개씩 선정
**이유**: 사용자가 원하는 타입만 골라서 복습 가능. 타입 간 간섭 없음
**트레이드오프**: 전체 섞기 대비 UX 복잡도 약간 증가

### ADR-007: JWT + HttpOnly Cookie 인증
**결정**: 인증 방식으로 JWT를 HttpOnly Cookie에 저장. Access Token만 사용 (7일 만료)
**이유**: SPA + REST API 구조에 자연스러운 stateless 인증. HttpOnly Cookie는 localStorage 대비 XSS 공격 시 토큰 탈취 불가. 브라우저가 Cookie를 자동 전송하므로 프론트엔드 토큰 관리 불필요. `credentials: "include"` 추가만으로 동작
**트레이드오프**: Refresh Token 미사용 → 7일 후 재로그인 필요. CSRF 위험은 SameSite=Lax + API-only 경로로 완화. Session 방식 대비 서버 상태 불필요 (Redis/DB 세션 저장소 없음). 개인 학습 앱이므로 7일 토큰 유효기간은 편의성 대비 보안 리스크 허용 가능

### ADR-009: 복습 카드 조회 N+1 해소 (LIMIT + IN 배치 조회)
**결정**: `getTodayCards()` API에서 (1) Repository 레벨 LIMIT으로 필요한 ReviewItem만 조회하고 (2) 관련 엔티티(Word, Pattern, GeneratedSentence, 예문)를 IN 쿼리로 배치 조회
**이유**: 기존 구현은 ReviewItem을 전체 조회 후 건바이건으로 Word/Pattern/GeneratedSentence를 개별 조회 (N+1 문제). 단어 30개 기준 62개 쿼리 발생 → 4개 고정 쿼리로 감소
**트레이드오프**: 서비스 코드 복잡도 소폭 증가 (Map 기반 매핑). 하지만 쿼리 수가 O(N)에서 O(1)로 변경되어 성능 개선이 압도적

### ADR-010: 단어 벌크 보강 배치 호출 (N→배치)
**결정**: WordService.bulkCreate에서 Gemini API를 건바이건(N회) 대신 25개 단위 배치(⌈N/25⌉회)로 호출. 프롬프트와 응답 DTO를 배열 구조로 변경
**이유**: 300개 단어 등록 시 300번 API 호출 → API 한도 초과 문제. GenerateService에서 이미 검증된 배열 프롬프트 패턴 재사용. 25개 배치는 응답 토큰 제한 내에서 안정적 (단어당 ~4필드, 25개 = ~100 JSON 필드)
**트레이드오프**: 배치 내 일부 단어만 보강 실패해도 배치 전체가 미보강 저장됨 (개별 실패 격리 불가). 단, API 호출 95% 절감 이득이 압도적. 배치 크기는 상수로 관리하여 필요 시 조정 가능

### ADR-011: logback-spring.xml 기반 로깅 통합 관리
**결정**: application.yml의 `show-sql`/`logging.level` 대신 `logback-spring.xml`에서 프로파일별 로깅을 통합 관리. `logstash-logback-encoder`로 prod JSON 출력
**이유**: `show-sql`은 `System.out.println`으로 출력하여 logback을 경유하지 않음 → traceId 미포함, 파일 롤링 미적용. `logback-spring.xml`은 프로파일별 appender 분리, 파일 롤링, JSON 포맷 등 application.yml로 불가능한 설정을 지원. 로깅 설정이 두 곳에 분산되면 혼란 → 단일 파일로 통합
**트레이드오프**: logback XML 설정 학습 비용. 하지만 실무 표준이며 한 번 구성하면 변경 드묾

### ADR-012: MDC traceId + Filter 기반 요청 추적
**결정**: `OncePerRequestFilter`로 요청마다 UUID 8자리 traceId를 MDC에 세팅. AOP 미사용
**이유**: HTTP 요청의 시작~끝을 감싸려면 Filter가 자연스럽다. AOP는 메서드 단위로 컨트롤러 진입 전(Security 처리 등)의 로그를 놓침. 실무에서 traceId는 Filter가 표준. traceId로 동시 요청 환경에서 한 요청의 전체 흐름을 `grep`으로 추적 가능
**트레이드오프**: 분산 시스템에서는 Spring Cloud Sleuth/Micrometer Tracing이 표준이지만, 단일 서버 환경에서는 커스텀 MDC Filter가 가볍고 충분

### ADR-013: 4xx WARN + 스택트레이스, 5xx ERROR + 스택트레이스
**결정**: GlobalExceptionHandler에서 4xx는 `log.warn` + 스택트레이스, 5xx는 `log.error` + 스택트레이스로 로깅
**이유**: 4xx는 클라이언트 오류(서버 정상)이므로 WARN. 5xx는 서버 장애이므로 ERROR. 운영 알림(Slack/PagerDuty)은 보통 ERROR에 연동하므로 4xx를 ERROR로 찍으면 알림 폭탄. 단, 4xx도 스택트레이스를 포함하여 소스 위치(클래스, 라인번호) 추적이 가능하도록 함
**트레이드오프**: 4xx에 스택트레이스를 붙이면 로그 용량이 증가하지만, 디버깅 편의성이 우선. 실무에서는 4xx 스택트레이스를 생략하는 경우가 많으나 학습/디버깅 목적으로 포함

### ADR-008: 전체 Entity에 user_id FK 추가 (사용자별 데이터 격리)
**결정**: Word, Pattern, GeneratedSentence, GenerationHistory, ReviewItem, StudyRecord, UserSetting 7개 Entity에 user_id FK 추가. 자식 Entity(PatternExample, SentenceSituation, GeneratedSentenceWord, StudyRecordItem)는 부모를 통해 접근하므로 미추가. ReviewLog는 reviewItemId를 @ManyToOne으로 변경하여 JPQL JOIN 지원
**이유**: 다중 사용자 지원 시 데이터 격리 필수. 인증과 동시에 구현하여 일관된 아키텍처 유지
**트레이드오프**: 변경 범위 큼 (7 Entity + 8 Repository + 8 Service + 7 Controller + 테스트 전체). 기존 DB 데이터 삭제 후 재시작 (`docker compose down -v && docker compose up`, ddl-auto: update 유지). DB UNIQUE 제약은 제거하고 앱 레벨 검증으로 전환 (soft delete 재등록 호환)

### ADR-014: 운영 리버스 프록시 Nginx + 단일 도메인 경로 분기
**결정**: 운영 환경에서 Nginx 1.27을 리버스 프록시로 사용. 사용자 도메인 하나로 받아 `/api/*` → backend:8080, 그 외 → frontend:3000으로 분기. backend/frontend는 외부 노출하지 않고 Nginx만 80 노출
**이유**: 단일 도메인 + 경로 분기는 브라우저 same-origin이 보장되어 CORS preflight가 발생하지 않고, JWT 쿠키 SameSite/Secure 정책이 가장 단순. Caddy 대신 Nginx를 선택한 것은 사용자 운영 도구 친숙도(기존 인프라 표준과 정합). `location /api/ { proxy_pass http://backend:8080; }`처럼 trailing slash 없는 proxy_pass로 prefix(`/api`)를 보존하여 백엔드 `@RequestMapping("/api/...")` 매핑과 직결
**트레이드오프**: Caddy 대비 자동 HTTPS 기능이 없지만 현재 CF Flexible 모드에서는 origin에 인증서가 필요 없어 무영향. `client_max_body_size 10M`을 명시하지 않으면 nginx 기본 1MB에서 파일 업로드가 413으로 막힘 → 명시 필수. Next.js HMR/WebSocket 케이스 대비 `Upgrade`/`Connection` 헤더 포함

### ADR-015: Cloudflare Flexible SSL 채택 + 후속 Full(strict) 전환 권장
**결정**: 초기 운영은 Cloudflare Flexible SSL 모드(브라우저↔CF는 HTTPS, CF↔미니PC는 HTTP)로 시작. 포트포워딩은 공유기 80 + 22만 사용. Nginx는 origin에서 `listen 80;`만 처리
**이유**: 사용자가 이미 Cloudflare에 도메인을 등록하고 Flexible로 설정 완료. 도메인 노출의 즉시성과 운영 단순성을 우선. Let's Encrypt 자동화나 Origin Certificate 발급 단계를 첫 배포 범위에서 제외하여 진입 장벽 최소화
**트레이드오프**: CF↔origin이 평문이라 origin 공인 IP에 직접 접근 시 중간자 공격 위험. 운영 안정화 후 Full(strict) + Origin Certificate 또는 Cloudflare Tunnel(`cloudflared` 컨테이너)로 전환을 후속 권장. 동적 공인 IP 환경에서는 DDNS 필수(IP가 바뀌면 A 레코드 수동 갱신 비현실적)

### ADR-016: GHCR + GitHub Actions SSH 배포 (appleboy/ssh-action)
**결정**: GitHub Actions가 백엔드/프론트엔드 도커 이미지를 GHCR(`ghcr.io/ha9011/harness_framework-{backend,frontend}`)에 빌드/push한 뒤, `appleboy/ssh-action@v1.2.0`으로 미니PC에 SSH 접속해 `git pull && docker compose -f docker-compose.prod.yml pull && up -d` 실행. 이미지 태그는 `latest` + `${{ github.sha }}` 동시 푸시
**이유**: Self-hosted runner는 미니PC 빌드 부하/보안 부담. Watchtower는 폴링 지연과 강제 재배포 어려움. SSH 배포는 명시적 트리거와 즉시 반영을 보장하면서 단일 액션으로 깔끔. 사용자가 이미 22번 포트포워딩이 되어 있어 추가 인프라 불필요. `webfactory/ssh-agent` 대비 `appleboy/ssh-action`은 원격 스크립트 블록을 한 곳에 두어 가독성 우월
**트레이드오프**: GitHub Actions 러너의 IP가 광범위해 origin의 22 포트 화이트리스트가 비현실적 → SSH key-only + fail2ban 강화를 후속 권장. GHCR 패키지는 첫 push 시 private이라 미니PC에서 `unauthorized: denied`가 날 수 있음 → public 가시성 변경(단순) 또는 미니PC에서 PAT로 `docker login`. SSH_PRIVATE_KEY를 GitHub Secrets에 저장하므로 키 노출 시 즉시 회전 절차 필요

### ADR-017: 운영 같은 origin 채택 → Frontend `/api` 상대경로 + Spring `prod` profile
**결정**: Frontend의 `lib/api.ts` `BASE_URL`을 `"/api"` 상대경로로 변경. Spring Boot `application-prod.yml` 신규 추가하여 DB URL/계정/JWT/Gemini 키를 환경변수로만 받음(기본값 없음). 기존 `application-local.yml`은 유지
**이유**: 단일 도메인 + 경로 분기 구조에서는 절대 URL이 불필요. dev 환경에서도 `next.config.ts` rewrites로 같은 패턴을 흉내낼 수 있어 dev/prod 코드 차이 제거. prod profile에 기본값을 두지 않는 것은 환경변수 누락 시 부팅 실패(fail-fast)로 만들기 위함 — 평문 dev 시크릿이 운영에 새는 사고 방지
**트레이드오프**: dev 시 frontend → backend 직접 호출 흐름이 바뀌므로 `next.config.ts`의 dev rewrites가 정상 동작하는지 검증 필요(Next.js 16의 rewrites 동작 변경 가능성 — `frontend/AGENTS.md` 지시대로 `node_modules/next/dist/docs/`에서 확인). Spring `forward-headers-strategy: framework` 필수(Nginx가 보낸 `X-Forwarded-Proto`를 신뢰해 redirect 시 https 유지)

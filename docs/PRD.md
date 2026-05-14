# PRD: 영어 패턴 학습기

## 목표
영어 단어×패턴을 AI(Gemini)로 조합해 실생활 예문을 생성하고, SM-2 간격 반복으로 기억을 정착시키는 웹 기반 학습 도구.

## 사용자
영어 패턴 교재로 공부하는 학습자. 교재 사진이나 유튜브 캡쳐로 단어/패턴을 빠르게 등록하고, 출퇴근길 스마트폰으로 복습한다.

## 핵심 기능
1. **단어 등록** — 직접 입력, JSON 벌크, 이미지 업로드(Gemini Vision 추출). AI 자동 보강(품사/발음/유의어/팁). 중요 체크(⭐)
2. **패턴 등록** — 직접 입력 또는 교재 사진 업로드 → AI 추출 (패턴+설명+예문 10개+해석)
3. **예문 생성** — 난이도(유아/초등/중등/고등) + 개수(10/20/30) → 단어×패턴 조합. 예문마다 감정이입 상황 5개 생성. 단어 지정/패턴 지정 생성도 가능
4. **복습 (간격 반복)** — [단어][패턴][문장] 탭 분리. SM-2 기반 커스텀 알고리즘. 양방향 카드(인식+회상). 처음부터 다시(읽기 전용) + 추가 복습
5. **학습 기록** — 단어/패턴 등록 시 자동 생성. 날짜별 Day N 기록
6. **홈 대시보드** — 타입별 복습 남은 개수 + 커피나무 성장(streak 연동) + 누적 통계 + 최근 학습
7. **설정** — 하루 복습 개수(10/20/30), 타입별 각 N개씩 = 총 3N장

## 에러 처리 / 엣지케이스
- **Gemini 실패**: 단건 보강 실패 → 보강 없이 저장. 벌크 보강 실패 → 부분 성공 (saved/skipped/enrichmentFailed). 재시도 2회
- **빈 상태**: 단어/패턴 0개 → 예문 생성 시 400 에러 + 안내 메시지. 복습 0개 → "복습할 카드가 없습니다"
- **중복 등록**: 단어/패턴 중복은 앱 레벨에서 검증 (DB UNIQUE 제거 — soft delete 재등록 호환). 사용자별 독립 검증 → 다른 사용자는 같은 단어/패턴 등록 가능. 409 Conflict. 벌크 시 skipped 처리
- **이미지 추출**: 형식 오류 400, 결과 없음 200+빈값, Gemini 오류 502
- **soft delete 연쇄**: 단어/패턴 삭제 시 예문은 유지, 해당 사용자의 WORD/PATTERN review_items만 삭제. SENTENCE 카드는 유지
- **SM-2 이중 적용 방지**: "처음부터 다시"는 읽기 전용(SM-2 미적용)
- **인증 에러**: 로그인 실패 시 "이메일 또는 비밀번호가 올바르지 않습니다" (이메일/비밀번호 구분 금지 — 정보 노출 방지)
- **JWT 만료**: 7일 후 401 → 프론트엔드 자동 로그인 페이지 리다이렉트
- **IDOR 방어**: 모든 리소스 접근 시 userId 검증. 다른 사용자의 단어/패턴/복습카드 접근 불가 (403)

## 기능 10: 단어 벌크 보강 배치 최적화 (Phase 4-review-n-plus-one)
- **문제**: 단어 벌크 등록 시 N개 단어 = N번 Gemini API 호출 (for 루프). 300개 등록 → 300번 호출으로 API 한도 초과
- **해결**: 프롬프트를 배열 구조로 변경하여 20~30개 단위 배치 호출. 300개 → 10~15회로 감소 (95% 절감)
- **배치 크기**: 기본 25개. 프롬프트에 단어 배열 포함, 응답도 배열로 수신
- **에러 처리**: 배치 실패 시 해당 배치 전체 미보강 저장 (기존 3회 재시도 정책 유지). 나머지 배치는 계속 진행
- **단건 등록**: 기존 방식 유지 (변경 없음)
- **범위**: WordService.bulkCreate만 변경. PatternService(API 호출 없음), GenerateService(이미 배열 구조)는 변경 불필요

## 기능 11: 백엔드 실무 로깅 구성
- **목표**: 쿼리 로그(SQL + 바인드 파라미터), 요청/응답 로그, 에러 로그를 실무 수준으로 구성. 운영 배포 대비
- **로컬 환경**: 컬러 콘솔 출력. SQL + 파라미터 + traceId. 파일 저장 안 함
- **prod 환경**: JSON 콘솔(ELK/CloudWatch 수집) + 파일 롤링(30일) + 에러 전용 파일(90일). SQL + 파라미터 포함
- **요청 추적**: 모든 요청에 traceId(UUID 8자리) 부여. 요청 시작/완료(method, URI, status, 처리시간) 로깅
- **에러 로깅**: 4xx → WARN + 스택트레이스(소스 위치 추적), 5xx → ERROR + 스택트레이스
- **엣지케이스**:
  - Spring Security 401 (JWT 없음/만료): SecurityConfig authenticationEntryPoint에서 직접 응답 → GlobalExceptionHandler 미경유 → WARN 스택트레이스 없음. 단, MdcLoggingFilter 완료 로그에서 401 status + 처리시간은 확인 가능
  - Application 코드의 AuthenticationException (로그인 실패 등): GlobalExceptionHandler 경유 → WARN + 스택트레이스 정상 출력
  - prod SQL 로그: 벌크 등록(300건) 시 SQL+파라미터 로그 대량 발생. 파일 롤링(100MB/파일, 3GB 상한)으로 용량 관리
- **테스트 영향**: 테스트는 profile 미지정 → logback default 프로파일 (INFO 콘솔, SQL OFF). MdcLoggingFilter가 @Component로 테스트에서도 동작하나 해 없음
- **제외**: Actuator/Prometheus(별도 작업), 요청 body 로깅(민감정보), AOP(Filter로 충분)

## 기능 13: 미니PC 도커 배포 + GitHub Actions CI/CD (Phase 8-deploy)
- **목표**: 영어 학습기를 사용자 미니PC에 도커로 배포하고 GitHub Actions로 자동 배포 파이프라인을 구성. 외부 도메인 + HTTPS로 실제 운영 환경처럼 사용
- **인프라 구성**: PostgreSQL 16 + Spring Boot + Next.js + Nginx 리버스 프록시 4개 컨테이너. 모두 도커 네트워크 내부 통신, Nginx만 80 외부 노출
- **도메인 구조**: 단일 도메인 + 경로 분기. `/api/*` → backend:8080, `/` → frontend:3000. 같은 origin이므로 브라우저 CORS preflight 없음. Frontend API URL은 `/api` 상대경로로 변경
- **HTTPS**: 사용자 도메인이 Cloudflare 등록, **Flexible SSL 모드** 사용(브라우저↔CF는 HTTPS, CF↔미니PC는 HTTP). 후속 단계에서 Full(strict) 또는 Cloudflare Tunnel로 보안 강화 권장
- **CI/CD**: main 브랜치 push → GitHub Actions가 backend/frontend 이미지를 GHCR(`ghcr.io/ha9011/harness_framework-{backend,frontend}`)에 빌드/push → SSH(`appleboy/ssh-action`)로 미니PC 접속해 `git pull && docker compose pull && up -d`
- **운영 profile**: Spring `prod` profile 신규 추가. DB URL/계정/JWT/Gemini API 키를 모두 환경변수로만 받음(기본값 없음). 기존 `local` profile은 그대로 유지
- **DB 이전**: 로컬 PostgreSQL을 `pg_dump`로 1회 덤프 후 미니PC 운영 컨테이너에 `pg_restore`로 복원. `ddl-auto: validate`이므로 복원 후에 backend 컨테이너 부팅
- **시크릿 관리**: 미니PC `/opt/harness/.env`에 평문 보관(`chmod 600`). compose가 자동 로드. 운영 DB 비밀번호/JWT secret은 `openssl rand`로 새로 생성. Gemini API 키도 운영용 신규 발급
- **로그 처리**: backend 컨테이너의 `/app/logs`를 미니PC `/opt/harness/logs`에 바인드 마운트 → 호스트에서 `tail -f` 가능. 기존 logback-spring.xml의 `prod` 프로파일 블록(JSON 콘솔 + 파일 롤링)을 그대로 활용
- **배포 인증 흐름**: GitHub Actions가 `~/.ssh/gha_deploy` 전용 키로 미니PC 22번 포트 접속(공유기 포트포워딩 기존). GHCR 패키지는 public 가시성 권장(또는 미니PC에서 PAT로 `docker login`)
- **엣지케이스**:
  - 첫 빌드 후 GHCR 패키지가 private이면 미니PC에서 `unauthorized: denied` → public 가시성 변경 또는 PAT 로그인
  - 파일 업로드 413 → nginx `client_max_body_size 10M` (Spring multipart 10MB와 정합)
  - Cloudflare가 `X-Forwarded-Proto: https`를 보내므로 Spring `forward-headers-strategy: framework`로 원래 scheme 인식
  - `ddl-auto: validate` 상태에서 빈 DB로 부팅 시 실패 → DB 복원 후 backend 컨테이너 기동 순서 준수
  - 동적 공인 IP 환경에서는 DDNS 필수
- **테스트 영향**: 인프라 구성 변경이므로 기존 테스트 영향 없음. 빌드 검증은 `docker build`로, 통합 검증은 `docker compose -f docker-compose.prod.yml up`으로 수행(JUnit/MockMvc와 무관)
- **범위 제외**: HTTPS Full(strict) 전환, Cloudflare Tunnel, 모니터링/알림, 자동 백업, SSH key-only 강화는 후속 phase 또는 운영 매뉴얼 권장사항으로 분리

## 기능 12: 카페 테마 로딩 컴포넌트
- **목표**: API 대기 시 카페 테마에 맞는 감성적 로딩 애니메이션 제공. 기존 "불러오는 중..." 텍스트를 시각적 컴포넌트로 교체
- **CremaLoader (큰 로딩)**: 머그잔 탑뷰 SVG + 크레마 회전 애니메이션. AI 호출 등 오래 걸리는 요청에 사용. 상황별 메시지 표시 ("예문을 만들고 있어요...", "단어를 등록하고 있어요..." 등)
- **CoffeeSpinner (미니 스피너)**: 사이드뷰 커피잔 SVG 회전. 일반 페이지 초기 로딩에 사용
- **적용 대상**:
  - CremaLoader: 예문 생성, 단어 등록(단건/벌크), 패턴 등록, 이미지 추출
  - CoffeeSpinner: 대시보드, 단어/패턴 목록·상세, 복습, 학습기록, 설정, 생성 이력
- **제약**: 외부 라이브러리 없이 SVG + CSS 애니메이션으로 구현. 기존 테마 색상 재사용

## MVP 이후 기능

### 기능 8: 회원가입/로그인 (Phase 1-auth)
- **회원가입**: 이메일(UNIQUE) + 비밀번호(최소 8글자, BCrypt) + 닉네임. 가입 즉시 자동 로그인
- **로그인**: 이메일 + 비밀번호. 성공 시 JWT를 HttpOnly Cookie로 발급 (7일). "이메일 저장" 체크박스 — localStorage에 이메일 기억, 다음 방문 시 자동 입력
- **로그아웃**: Cookie 삭제
- **인증 상태 확인**: GET /api/auth/me — 현재 로그인 사용자 정보 반환
- **프론트엔드**: 로그인/회원가입 페이지 + 홈에 "닉네임님 안녕하세요" 표시 + 미로그인 시 로그인 페이지로 리다이렉트
- **데이터 격리**: 전체 Entity에 user_id FK 추가. 사용자별 독립 데이터 (단어, 패턴, 예문, 복습, 학습기록, 설정 모두 분리). 기존 DB 데이터는 삭제 후 새로 시작

### 기능 9: 로그아웃 버튼 + Hydration 수정 (Phase 2-login-convenience)
- **로그아웃 버튼 (홈 우상단)**: 인사 헤더 영역 우상단에 로그아웃 아이콘 버튼. 누르면 로그아웃 실행 (기존 AuthContext.logout 활용)
- **로그아웃 버튼 (설정 페이지)**: 설정 카드 아래에 로그아웃 버튼 추가. Ghost 스타일, 텍스트 "로그아웃"
- **Hydration 에러 수정**: layout.tsx의 `<body>`에 `suppressHydrationWarning` 추가. 브라우저 확장이 주입하는 속성으로 인한 hydration mismatch 경고 제거

### MVP 제외 사항 (다음 Phase 후보)
- 닉네임/비밀번호 변경
- HTTPS Full(strict) 전환 또는 Cloudflare Tunnel (운영 보안 강화)
- 운영 모니터링/알림 (Actuator, Slack/PagerDuty 연동)
- 자동 백업 cron (pg_dump 스케줄 + 외부 스토리지 동기화)
- TTS/STT 말하기 기능 (Web Speech API)
- 퀴즈/빈칸 테스트
- 즐겨찾기
- 이미지 묘사 학습 (TOEIC Speaking/OPIC 대비)

## 디자인
- **테마**: Cozy Cafe & Coffee Tree — 따뜻한 카페 분위기 + 커피나무 성장 게이미피케이션
- **색상**: 크림 화이트(#FAF6F0), 라떼 브라운(#A67C52), 세이지 그린(#7A8F6B), 모카 브라운(#3D2E22)
- **Mobile-First**: 한 손으로 복습 가능한 반응형 UI
- **카드 플립 애니메이션** + 부드러운 Soft Shadow

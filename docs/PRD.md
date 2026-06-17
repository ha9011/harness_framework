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
- **시크릿 관리**: 미니PC `/home/hadong/work/project/english/.env`에 평문 보관(`chmod 600`). compose가 자동 로드. 운영 DB 비밀번호/JWT secret은 `openssl rand`로 새로 생성. Gemini API 키도 운영용 신규 발급
- **로그 처리**: backend 컨테이너의 `/app/logs`를 미니PC `/home/hadong/work/project/english/logs`에 바인드 마운트 → 호스트에서 `tail -f` 가능. 기존 logback-spring.xml의 `prod` 프로파일 블록(JSON 콘솔 + 파일 롤링)을 그대로 활용
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

### 기능 14: 운영 로그인 세션 유지 + 모바일 입력 줌 수정 (Phase 9-auth-mobile-fix)
- **배경**: 운영(HTTPS 도메인, iOS Safari) 환경에서 (1) 로그인 후 앱을 백그라운드로 보냈다 복귀하면 로그아웃되고, (2) 로그인 입력 필드에 포커스하면 화면이 줌인되어 메인페이지까지 확대 상태가 유지됨
- **세션 유지 (쿠키 Secure)**:
  - **원인 (유력, 운영 재현으로 검증 필요)**: JWT 쿠키가 `httpOnly` + `SameSite=Lax` + `Max-Age=604800`(7일)로 발급되나 `Secure` 속성이 없음. non-Secure 쿠키도 HTTPS에서 저장·전송 자체는 되지만, iOS Safari는 HTTPS 사이트의 non-Secure 쿠키를 백그라운드 복귀(탭 메모리 해제 후 재로드) 시 신뢰성 있게 유지하지 않는 사례가 많음 → 복귀 시 `GET /api/auth/me`가 쿠키 없이 호출되어 401 → 로그아웃. **정적 분석만으로 단일 확정 원인은 단언 불가**하나, `Secure`는 운영 HTTPS에서 어차피 적용해야 할 올바른 수정이므로 1순위로 적용 후 운영 iOS에서 재현·검증
  - **해결**: 쿠키에 `Secure` 속성 추가. 단, Cloudflare Flexible SSL이라 origin(Spring)은 평문 HTTP로 요청을 받으므로 요청 scheme로 자동 판단하지 않고 **profile 설정값으로 명시 제어** (`app.cookie.secure`: prod=true, local/default=false). 로컬 HTTP 개발(localhost)은 Secure=false 유지하여 정상 동작. set/clear 쿠키 모두 동일 헬퍼(`createTokenCookie`)를 사용하므로 로그아웃 쿠키도 같은 속성(prod에서 Secure 포함)으로 발급되어 정상 삭제됨
  - **불변**: SameSite=Lax, Max-Age=7일, HttpOnly, Path=/api는 변경 없음. JWT 만료 정책(7일)도 변경 없음
- **iOS 입력 줌 방지**:
  - **원인**: 입력 필드 폰트가 `text-sm`(14px)로 16px 미만 → iOS Safari가 input 포커스 시 자동 줌인. 이 줌이 페이지 이동 후에도 유지되어 메인페이지가 확대되어 보임. 해당 input은 login/signup뿐 아니라 words/page, WordAddModal, PatternAddModal 등 **총 5개 파일**에 분포
  - **해결**: 모바일(터치) 환경에서 form control(input/textarea/select) 폰트를 16px 이상으로 보장. 5개 파일을 개별 수정하는 대신 **globals.css 전역 규칙 1곳**으로 일괄 적용(미디어쿼리/coarse pointer 한정 → 데스크톱 디자인 보존). 접근성을 위해 `user-scalable=no`(핀치 줌 비활성화)는 사용하지 않음
  - **⚠️ Tailwind 특이성 주의**: element selector `input`(특이성 0,0,1)은 Tailwind 유틸 클래스 `text-sm`(0,1,0)을 **이기지 못함** → 단순 `input { font-size: 16px }`는 무효. `@media (pointer: coarse) { input, textarea, select { font-size: 16px !important } }`처럼 **`!important`** 또는 더 높은 특이성으로 적용해야 실제로 덮어씀
  - **viewport (선택)**: layout.tsx에 `width=device-width, initial-scale=1` viewport export를 명시 가능하나, Next.js 기본 viewport와 동일하여 **줌 해결의 핵심은 아님**(실제 수정은 16px 폰트). 명시적 선언 목적의 선택 작업
- **엣지케이스**:
  - 로컬 HTTP 개발에서 Secure=true면 쿠키 미전송 → 로그인 깨짐. 반드시 profile별로 분기(local/default=false)
  - `user-scalable=no`/`maximum-scale=1`로 줌을 막는 방식은 접근성 위배 + iOS 일부 버전이 무시 → 채택 안 함. 16px 폰트 방식으로 해결
  - 인앱 브라우저(카카오 등)/PWA standalone은 쿠키 유지 정책이 더 제한적일 수 있어 별도 확인 필요(범위 외)
- **테스트 영향**: 기존 AuthControllerTest/AuthIntegrationTest는 Set-Cookie의 `Max-Age=604800`/`Max-Age=0`만 `containsString`으로 단언 → Secure 추가로 깨지지 않음. 신규: `app.cookie.secure=true`일 때 Set-Cookie에 `Secure` 포함을 단언하는 테스트 추가. 프론트 입력 폰트/viewport는 시각적 변경이라 단위테스트 대신 빌드/수동 확인
- **범위**: 백엔드 AuthController 쿠키 생성 + application-prod.yml/application-local.yml 설정. 프론트 globals.css(모바일 input 폰트) + layout.tsx(viewport export). 그 외 인증 로직/JWT 만료(7일)는 변경 없음

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

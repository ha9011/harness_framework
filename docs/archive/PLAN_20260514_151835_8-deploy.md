# PLAN: 8-deploy — 미니PC 도커 배포 + GitHub Actions CI/CD

## 작업 목표
영어 학습기를 사용자 미니PC에 도커로 컨테이너화하고, main 브랜치 push 시 GitHub Actions가 GHCR 빌드 → SSH 배포까지 자동화하는 운영 파이프라인을 구축한다.

## 구현할 기능
1. 백엔드/프론트엔드 도커 이미지화 (multi-stage Dockerfile)
2. Frontend same-origin 전환 (`/api` 상대경로) + Next.js standalone 출력
3. Spring Boot `prod` profile 신규 추가 (환경변수만 받는 fail-fast 구성)
4. 운영 docker-compose.prod.yml 작성 (postgres + backend + frontend + nginx)
5. Nginx 리버스 프록시 설정 (`/api/*` → backend, `/` → frontend, prefix 보존)
6. `.env.example` + `.gitignore` `.env` 추가
7. GitHub Actions workflow (`deploy.yml`) — GHCR 빌드 + SSH 배포
8. `docs/DEPLOYMENT.md` 운영 매뉴얼 작성

## 기술적 제약사항
- 리버스 프록시는 **Nginx 1.27**(Caddy 아님). 단일 도메인 + 경로 분기 구조.
- 실제 코드베이스 환경: **Java 17 (toolchain) + Spring Boot 3.5.0 / Next.js 16.2.4 + React 19.2.4**. Dockerfile 베이스 이미지는 이 버전과 정합해야 한다(`eclipse-temurin:17-jdk-jammy`, `node:20-alpine`).
- `backend/build.gradle.kts`에 **`net.logstash.logback:logstash-logback-encoder:8.0`** 이미 존재 → prod profile의 JSON 콘솔 로깅이 추가 의존성 없이 동작.
- Cloudflare **Flexible SSL** 모드 사용 — origin은 80 HTTP만 listen. Caddy/Let's Encrypt/Origin Cert 모두 이번 phase 범위 밖.
- GitHub Actions가 미니PC에 접속할 SSH 키는 별도 생성(`gha_deploy`), 본인 키와 분리.
- GHCR 이미지명: `ghcr.io/ha9011/harness_framework-{backend,frontend}:{latest,<sha>}` — 첫 push 후 패키지 가시성 public 권장.
- 백엔드 JPA `ddl-auto: validate`이므로 미니PC 첫 부팅 전에 로컬 `pg_dump`를 `pg_restore`로 복원해야 한다.
- 운영 시크릿은 미니PC `/opt/harness/.env`에만 존재. 절대 깃에 커밋 금지(`.gitignore` 추가).
- `application-prod.yml`은 기본값을 두지 않음 — `JWT_SECRET`/`SPRING_DATASOURCE_*`/`GEMINI_API_KEY` 누락 시 부팅 실패(fail-fast)로 dev 시크릿 유입 방지.
- Nginx `client_max_body_size 10M;` 명시 필수(Spring multipart 10MB 한도와 정합). 누락 시 파일 업로드 413.
- Spring `forward-headers-strategy: framework` 필수(Nginx의 `X-Forwarded-Proto: https`를 신뢰해 redirect 시 https 유지).
- 컨트롤러 매핑이 모두 `/api/...`로 시작하므로 Nginx는 prefix를 **보존**해야 한다. `location /api/ { proxy_pass http://backend:8080; }` (trailing slash 없음) 패턴.
- Next.js 16 standalone 산출물 경로/엔트리는 `frontend/node_modules/next/dist/docs/`에서 사전 확인 필수(`frontend/AGENTS.md` 지시).
- `application-local.yml`에 노출되어 있는 Gemini 키는 **운영 재사용 금지** — 새 키 발급.
- `SecurityConfig`의 CORS allowedOrigins(`http://localhost:3000` 하드코딩)는 same-origin 운영에서 preflight가 발생하지 않으므로 이번 phase에서 손대지 않는다.

## 테스트 전략
- **기존 백엔드 테스트 영향**: 없음. 인프라 구성 변경(Dockerfile/compose/nginx/workflow/profile yml)만 진행하고 비즈니스 로직/엔티티/컨트롤러 시그니처는 그대로다. 기존 JUnit/MockMvc/TestContainers는 영향 없이 통과해야 한다. 운영 profile(`prod`)은 테스트에서 활성화되지 않으므로 Spring 컨텍스트에 영향 없음.
- **기존 프론트엔드 테스트 영향**: 없음. `cd frontend && npm run test` (Vitest)도 영향 없이 통과해야 한다. `lib/api.ts`의 `BASE_URL` 변경(`/api` 상대경로)은 Vitest jsdom 환경에서도 fetch 모킹 패턴에 영향 없음.
- **신규 테스트**: 해당 없음. 인프라 검증은 단위 테스트가 아닌 `docker build` 성공과 `docker compose up` 시뮬레이션으로 확인한다.
- **테스트 수정이 필요한 Step**: 없음. 만약 Step 진행 중 기존 테스트가 깨지면 인프라 변경이 코드 동작을 바꾼 것이므로 **즉시 중단하고 원인 분석**(ADR-017의 `BASE_URL` 변경, Spring profile 추가가 테스트 컨텍스트에 영향 주는지 검토).
- **인프라 검증 명령**:
  - `cd backend && ./gradlew test` 기존 테스트 통과 유지
  - `cd frontend && npm run test` 기존 Vitest 통과 유지
  - `docker build -t harness-backend ./backend` 성공
  - `docker build -t harness-frontend ./frontend` 성공
  - `docker compose -f docker-compose.prod.yml up`으로 4 컨테이너 기동, `curl http://localhost/` 200, `curl http://localhost/api/auth/me` 401 확인

## Phase/Step 초안

### Step 0: 백엔드/프론트엔드 도커 이미지화
- **작업**:
  - `backend/Dockerfile` 작성 (multi-stage: `eclipse-temurin:17-jdk-jammy` 빌더 + `17-jre-jammy` 런타임, non-root user, `/app/logs` 디렉토리, `JAVA_OPTS` 지원)
  - `backend/.dockerignore` (`build/`, `.gradle/`, `application-local.yml`, `logs/`, `src/test/` 등 제외)
  - `frontend/Dockerfile` 작성 (multi-stage: deps → builder → runner, Next standalone 산출물 복사, non-root)
  - `frontend/.dockerignore` (`node_modules/`, `.next/`, `.env*` 등 제외)
  - `frontend/next.config.ts`에 `output: "standalone"` 추가 (Next 16 standalone 경로/엔트리 사전 확인 후 적용)
- **산출물**:
  - `docker build -t harness-backend ./backend` 성공 (이미지 크기 합리적 — JRE 베이스)
  - `docker build -t harness-frontend ./frontend` 성공
  - 두 이미지 모두 `docker run`으로 단독 실행 가능 (정상 부팅 — 백엔드는 환경변수 없으면 실패하는 것이 정상)

### Step 1: Same-origin 전환 + 운영 profile + 시크릿 템플릿
- **작업**:
  - `frontend/lib/api.ts:1` 한 줄 수정: `BASE_URL`을 `"http://localhost:8080/api"` → `"/api"`
  - `backend/src/main/resources/application-prod.yml` 신규 작성 (환경변수만, 기본값 없음, `forward-headers-strategy: framework` 포함)
  - 리포지토리 루트에 `.env.example` 작성 (`POSTGRES_*`, `GEMINI_*`, `JWT_SECRET`)
  - `.gitignore`에 `.env` 추가
  - dev 환경에서 frontend가 backend를 호출하는 흐름이 깨지지 않는지 확인 — 필요 시 `next.config.ts`에 dev rewrites 추가(`/api/:path*` → `http://localhost:8080/api/:path*`)
- **산출물**:
  - 기존 백엔드 테스트 전체 통과 (`cd backend && ./gradlew test`)
  - 로컬 dev (`docker compose up -d` + `cd backend && ./gradlew bootRun` + `cd frontend && npm run dev`) 시점에 회원가입/로그인 e2e 동작
  - `.env.example`은 커밋되고 `.env`는 gitignore되어 있음

### Step 2: 운영 컴포즈 + Nginx + 로컬 시뮬레이션
- **작업**:
  - `docker-compose.prod.yml` 작성 (postgres + backend + frontend + nginx 4 서비스, nginx만 80 노출, postgres healthcheck, `/opt/harness/logs:/app/logs` 바인드 마운트)
  - `nginx/conf.d/default.conf` 작성 (`listen 80; server_name _;`, `/api/` → backend, `/` → frontend, `client_max_body_size 10M`, gzip, WebSocket 헤더, `X-Forwarded-*`)
  - 로컬에서 `docker-compose.prod.override.yml`(임시, 비커밋)로 image 대신 `build:` 지정해 시뮬레이션
- **산출물**:
  - `docker compose -f docker-compose.prod.yml --env-file .env.local-test up --build`로 4 컨테이너 기동
  - `curl -i http://localhost/` → 200 + Next.js HTML
  - `curl -i http://localhost/api/auth/me` → 401 + JSON 에러 (Nginx 라우팅 정상)
  - 파일 업로드 경로(`/api/words`)에 10MB 미만 multipart 시 200(또는 비즈니스 응답), 413 안 남
  - 종료 후 `docker compose -f docker-compose.prod.yml down -v`로 정리

### Step 3: GitHub Actions workflow + 운영 매뉴얼
- **작업**:
  - `.github/workflows/deploy.yml` 작성 (main push + workflow_dispatch 트리거, build-backend/build-frontend 병렬, deploy job은 needs로 묶음, GHCR 인증 `GITHUB_TOKEN`, `appleboy/ssh-action@v1.2.0`로 미니PC 접속)
  - `docs/DEPLOYMENT.md` 작성 — `/Users/hadong/.claude/plans/deep-giggling-yao.md`의 A~G 단계별 매뉴얼을 발췌해 운영 매뉴얼화 (Docker 설치 / SSH 키 생성 / 디렉토리 / .env / DB 덤프 복원 / GitHub Secrets / 첫 배포 / 검증 / 롤백 / 자주 마주칠 문제)
- **산출물**:
  - workflow 파일은 push 없이 정적 검증 가능 — GitHub UI의 "Actions" 탭에서 syntax 에러 없음 확인 (또는 `yamllint`)
  - `docs/DEPLOYMENT.md`는 사용자가 그대로 따라하면 첫 배포까지 도달 가능한 수준
  - **실제 첫 배포는 Phase 종료 후 사용자가 수동으로 진행** (미니PC SSH 키 등록, GitHub Secrets 입력, GHCR 패키지 가시성 변경 등 외부 작업 포함)

## 미결 사항
- **운영 도메인 이름**이 plan/문서에 명시되지 않음 — Cloudflare DNS A 레코드, `SSH_HOST` 값은 사용자가 실제 운영 시점에 입력. 코드/설정 어디에도 도메인을 하드코딩하지 않음.
- **미니PC OS 버전** (Ubuntu LTS 버전, Debian 등)에 따라 Docker 설치 명령이 미세하게 다를 수 있음 — `docs/DEPLOYMENT.md`에는 Ubuntu/Debian 공통 절차로 작성.
- **공인 IP가 동적인지 정적인지** 확인 필요 — 동적이면 DDNS 권장(매뉴얼에 명시).
- **GHCR 패키지 첫 push 후 가시성 결정** — public 권장이지만 사용자가 private 선호 시 미니PC에서 PAT로 `docker login` 추가 단계 필요.
- **후속 권장 phase**: Cloudflare Full(strict) 또는 Tunnel 전환, 백업 cron, SSH 강화, Actuator 도입은 이번 범위 밖. PRD "MVP 제외 사항"에 후보로 등재 완료.

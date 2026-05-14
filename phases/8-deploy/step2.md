# Step 2: prod-compose-nginx

## Step Contract

- Capability: 운영 docker-compose 오케스트레이션(postgres + backend + frontend + nginx 4 서비스) + Nginx 리버스 프록시 설정(`/api/*` → backend:8080, `/` → frontend:3000) + 시크릿 템플릿 + `.env` gitignore
- Layer: integration-hardening
- Write Scope: `docker-compose.prod.yml`, `nginx/conf.d/default.conf`, `.env.example`, `.gitignore`
- Out of Scope: backend/ 코드, frontend/ 코드, CI workflow, 운영 매뉴얼 문서, Dockerfile 수정(Step 0/1에서 완료)
- Critical Gates: 임시 `.env.local-test`(테스트용 시크릿)와 `docker-compose.prod.override.yml`(이미지 대신 `build:` 사용)을 만들어 `docker compose -f docker-compose.prod.yml -f docker-compose.prod.override.yml --env-file .env.local-test config` syntax 통과 + `up -d --build`로 4 컨테이너 기동 + `curl -s -o /dev/null -w "%{http_code}" http://localhost/` 200/304/404(nginx→frontend 라우팅) + `curl -s -o /dev/null -w "%{http_code}" http://localhost/api/auth/me` 401 또는 502(nginx `/api/` prefix 보존으로 backend 도달, 404 절대 아님) + 검증 후 `down -v`로 정리 + 임시 파일(`.env.local-test`, `docker-compose.prod.override.yml`, `_local_logs/`) 모두 삭제 + `git check-ignore .env` 성공

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Phase 8-deploy 전체 계획
- `/docs/ADR.md` — ADR-014(Nginx prefix 보존 패턴), ADR-015(Flexible SSL → origin HTTP), ADR-017(forward-headers-strategy)
- `/docs/ARCHITECTURE.md` — 배포 토폴로지 다이어그램, `application-prod.yml` 환경변수 명세
- `/Users/hadong/.claude/plans/deep-giggling-yao.md` — 환경변수 격납 흐름 섹션(`docker-compose.prod.yml`의 `${VAR}` placeholder 패턴 + `.env` 자동 로드 메커니즘)
- `/docker-compose.yml` — 개발용 postgres 설정(이미지 버전, 볼륨명, env 이름 참고)
- 이전 step 산출물:
  - `backend/Dockerfile` — 8080 노출, non-root, `/app/logs` 볼륨 마운트 포인트
  - `backend/src/main/resources/application-prod.yml` — 환경변수 이름(`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `JWT_SECRET`)
  - `frontend/Dockerfile` — 3000 노출, non-root
- `/.gitignore` — 기존 패턴 확인(`application-local.yml` 등 이미 등록되어 있음). `.env` 추가 여부 확인
- `/backend/src/main/java/com/english/auth/AuthController.java` (또는 다른 컨트롤러) — 매핑이 `/api/auth/...` 형태인지 재확인(prefix 보존 필수)

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 1. `docker-compose.prod.yml` (리포지토리 루트, 신규)

4개 서비스를 단일 내부 네트워크에 두고 nginx만 외부 노출.

```yaml
services:
  postgres:
    image: postgres:16
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks: [internal]

  backend:
    image: ghcr.io/ha9011/harness_framework-backend:latest
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      GEMINI_MODEL: ${GEMINI_MODEL:-gemini-2.5-flash}
      JWT_SECRET: ${JWT_SECRET}
      TZ: Asia/Seoul
      JAVA_OPTS: "-Xms256m -Xmx512m"
    volumes:
      - /opt/harness/logs:/app/logs
    networks: [internal]

  frontend:
    image: ghcr.io/ha9011/harness_framework-frontend:latest
    restart: unless-stopped
    depends_on:
      - backend
    environment:
      NODE_ENV: production
      TZ: Asia/Seoul
    networks: [internal]

  nginx:
    image: nginx:1.27-alpine
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
    depends_on:
      - frontend
      - backend
    networks: [internal]

networks:
  internal:

volumes:
  pgdata:
```

핵심 규칙:
- **포트 노출은 nginx 80만**. backend/frontend/postgres는 모두 도커 내부 네트워크 내에서만 통신.
- 모든 `${VAR}`는 **placeholder만**. 실제 값 직접 작성 절대 금지(시크릿이 깃에 커밋된다).
- `healthcheck`의 `$${POSTGRES_USER}`는 compose의 변수 escape — `$`를 두 번 써서 컨테이너 셸에서 환경변수 그대로 평가하게 만든다. 한 번만 쓰면 compose가 호스트에서 치환을 시도하다 빈 값이 된다.
- backend의 `volumes: - /opt/harness/logs:/app/logs`는 **호스트 절대경로 바인드 마운트**. 미니PC 운영 시 이 디렉토리가 사전에 존재(B-2 단계에서 생성). 로컬 시뮬레이션 시에는 일시적으로 다른 경로 사용 가능하나, **이 step의 커밋본은 미니PC 기준 절대경로 유지**.
- `image: ghcr.io/ha9011/harness_framework-{backend,frontend}:latest` — 첫 배포 전에는 GHCR에 이미지가 없다. 로컬 시뮬레이션은 이미지 대신 `build:` 절을 임시로 추가하는 override 파일을 별도로 만든 후 검증한다(아래 검증 절차 참고). **override 파일은 커밋하지 않음**.
- `restart: unless-stopped` — 미니PC 재부팅 시 자동 복귀.

### 2. `nginx/conf.d/default.conf` (신규)

Cloudflare Flexible SSL이므로 origin은 HTTP 80만 listen.

```nginx
server {
    listen 80;
    server_name _;

    client_max_body_size 10M;

    gzip on;
    gzip_proxied any;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://frontend:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

핵심 규칙(ADR-014):
- `proxy_pass http://backend:8080;` — **트레일링 슬래시 없음**. `/api/foo` 요청이 backend에 `/api/foo` 그대로 전달(prefix 보존). `proxy_pass http://backend:8080/;`처럼 슬래시를 붙이면 `/foo`로 잘려 backend가 404를 낸다.
- `client_max_body_size 10M;` — Spring `multipart.max-file-size: 10MB`와 정합. 누락 시 파일 업로드 413.
- `X-Forwarded-Proto $scheme;` — Cloudflare가 `$scheme`를 https로 보내므로 backend가 `forward-headers-strategy: framework`로 원래 scheme 인식.

### 3. `.env.example` (리포지토리 루트, 신규)

운영 시크릿 템플릿 — 실제 값은 미니PC `/opt/harness/.env`에만 존재.

```
POSTGRES_DB=english_app
POSTGRES_USER=app
POSTGRES_PASSWORD=__CHANGE_ME__
GEMINI_API_KEY=__CHANGE_ME__
GEMINI_MODEL=gemini-2.5-flash
JWT_SECRET=__AT_LEAST_32_CHARS_RANDOM__
```

### 4. `.gitignore` 한 줄 추가

기존 내용 보존하고 적절한 위치(예: `application-local.yml` 근처)에 `.env` 추가:

```
.env
```

기존 항목 삭제/순서 변경 금지.

### 5. 로컬 시뮬레이션 검증 (커밋 대상 외부)

이 검증은 step 완료 판단용이며, 사용한 파일은 **커밋하지 않는다**. 임시로 다음 두 파일을 만들어 검증:

`.env.local-test` (임시, gitignore 대상):
```
POSTGRES_DB=english_app
POSTGRES_USER=app
POSTGRES_PASSWORD=testpwd1234
GEMINI_API_KEY=dummy-test-key
GEMINI_MODEL=gemini-2.5-flash
JWT_SECRET=test-jwt-secret-at-least-32-characters-long-12345
```

`docker-compose.prod.override.yml` (임시, gitignore 대상): 이미지 대신 로컬 빌드 사용 + 호스트 로그 디렉토리 변경:
```yaml
services:
  backend:
    build: ./backend
    image: harness-backend:local
    volumes:
      - ./_local_logs:/app/logs
  frontend:
    build: ./frontend
    image: harness-frontend:local
```

`./_local_logs` 디렉토리 사전 생성:
```bash
mkdir -p ./_local_logs
```

검증 명령:
```bash
docker compose -f docker-compose.prod.yml -f docker-compose.prod.override.yml --env-file .env.local-test config | head -40
docker compose -f docker-compose.prod.yml -f docker-compose.prod.override.yml --env-file .env.local-test up -d --build
sleep 30
docker compose -f docker-compose.prod.yml -f docker-compose.prod.override.yml --env-file .env.local-test ps
curl -s -o /dev/null -w "/: %{http_code}\n" http://localhost/
curl -s -o /dev/null -w "/api/auth/me: %{http_code}\n" http://localhost/api/auth/me
docker compose -f docker-compose.prod.yml -f docker-compose.prod.override.yml --env-file .env.local-test down -v
rm -rf ./_local_logs .env.local-test docker-compose.prod.override.yml
```

기대 결과:
- `config` 명령이 syntax 에러 없이 4 서비스를 출력
- `ps`에서 postgres healthy, frontend/nginx running, backend는 restarting(빈 DB + ddl-auto:validate → 의도된 실패)
- `curl /` → 200 (또는 304/404 — nginx → frontend 라우팅 성립)
- `curl /api/auth/me` → 502(backend restarting) 또는 401(backend 일시적으로 떠 있는 경우). **둘 다 정상** — 핵심은 nginx가 `/api/`를 backend로 보내고 있다는 점.

## Acceptance Criteria

```bash
docker compose -f docker-compose.prod.yml --env-file <임시 env> config > /dev/null
# 위 5번 절차의 시뮬레이션 + 정리까지 한 번에 통과
git check-ignore .env || (echo ".env가 gitignore에 없음" && exit 1)
```

- compose syntax 에러 없음
- 로컬 시뮬레이션에서 nginx 라우팅 동작 확인 (curl /, curl /api/auth/me 결과 위 기준)
- `.gitignore`에 `.env` 등록 확인 (`git check-ignore .env` 성공)
- 임시 파일 정리 완료 (`.env.local-test`, `docker-compose.prod.override.yml`, `./_local_logs/` 모두 삭제)

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - `docker compose ... config`로 환경변수 치환이 모두 풀렸는지 확인 (placeholder가 빈 문자열로 안 가도록)
   - nginx가 `/api/` prefix를 보존해서 backend에 전달하는지 (`curl /api/auth/me`가 backend로 도달해 502/401을 받는지) — 만약 404가 나오면 prefix가 잘려나간 것 → `proxy_pass` 슬래시 검토
   - 임시 파일이 커밋 대상에 섞이지 않았는지 (`git status`에 `_local_logs/`, `.env.local-test`, `docker-compose.prod.override.yml`이 없어야 함)
3. 아키텍처 체크리스트를 확인한다:
   - ADR-014 prefix 보존 패턴(`location /api/ { proxy_pass http://backend:8080; }`)을 따랐는가?
   - ADR-015 Flexible SSL 가정(`listen 80;`만)을 따랐는가?
   - CLAUDE.md CRITICAL 규칙(루트에 소스 코드 직접 두지 말 것)을 위반하지 않았는가? — `nginx/`는 설정 디렉토리라 OK, 루트에 `.env.example`/`docker-compose.prod.yml`은 인프라 정의라 OK
   - Step Contract의 Write Scope 밖(backend/, frontend/, .github/, docs/)을 수정하지 않았는가?
4. 결과에 따라 `phases/8-deploy/index.json`의 해당 step을 업데이트한다.

## 금지사항

- `backend/`, `frontend/` 내부 코드를 수정하지 마라. 이유: 이 step의 Write Scope 밖. 코드 변경은 Step 0/1에서 완료.
- `docker-compose.prod.yml`의 `environment` 섹션에 시크릿 값을 직접 평문으로 쓰지 마라. 이유: 깃 커밋 대상 파일에 시크릿 노출. **반드시 `${VAR}` placeholder만 사용**.
- `nginx/conf.d/default.conf`의 `proxy_pass http://backend:8080;`에 트레일링 슬래시를 붙이지 마라. 이유: `/api/foo` → backend에 `/foo`로 도달해 404 발생. ADR-014 위반.
- `client_max_body_size`를 누락하지 마라. 이유: nginx 기본 1MB에서 파일 업로드 413.
- `volumes: - /opt/harness/logs:/app/logs`를 커밋본에서 다른 경로로 바꾸지 마라. 이유: 운영 환경 기준. 로컬 검증은 override 파일로 분리.
- 임시 검증 파일(`.env.local-test`, `docker-compose.prod.override.yml`, `_local_logs/`)을 검증 후 삭제하지 않고 남기지 마라. 이유: 시크릿/임시 인프라 파일 커밋 위험.
- `.gitignore` 기존 항목을 삭제하거나 순서를 바꾸지 마라. 이유: scope 이탈 + 다른 기능 영향.
- `TODO`, `stub` 등으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

# Step 1: frontend-docker

## Step Contract

- Capability: Frontend Docker 이미지화(Next.js 16 standalone) + 같은 origin 전환(`lib/api.ts` BASE_URL을 `/api` 상대경로로)
- Layer: integration-hardening
- Write Scope: `frontend/Dockerfile`, `frontend/.dockerignore`, `frontend/next.config.ts`, `frontend/lib/api.ts`
- Out of Scope: backend 코드 일체, docker-compose, nginx 설정, CI/CD workflow, 기존 페이지/컴포넌트 수정
- Critical Gates: `cd frontend && npm run build` 성공 + `ls frontend/.next/standalone/server.js` 존재(Next standalone 산출물 검증) + `cd frontend && npm run test` 기존 Vitest 통과(`BASE_URL` 변경 회귀 없음) + `docker build -t harness-frontend ./frontend` 성공 + `docker run --rm -d -p 3001:3000 --name hf-test harness-frontend && sleep 5 && curl -s -o /dev/null -w "%{http_code}" http://localhost:3001/ && docker rm -f hf-test` 응답 200/304/404 중 하나(서버 기동 증거, 5xx 아님)

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Phase 8-deploy 전체 계획
- `/docs/ADR.md` — ADR-014(Nginx 경로 분기), ADR-017(same-origin `/api` 상대경로)
- `/docs/ARCHITECTURE.md` — Frontend 디렉토리 구조, 배포 토폴로지
- `/frontend/AGENTS.md` — **CRITICAL: Next.js 16의 standalone 출력 경로/엔트리는 통상 지식과 다를 수 있다. 작업 전 `frontend/node_modules/next/dist/docs/`를 검색해 정확한 standalone 산출물 경로(`server.js` 엔트리 포함)를 확인하라.**
- `/frontend/package.json` — Next 16.2.4, React 19.2.4, `npm run build`/`npm run test` 스크립트 확인
- `/frontend/next.config.ts` — 현재 빈 설정
- `/frontend/lib/api.ts` — 현재 `BASE_URL = "http://localhost:8080/api"` 하드코딩. `credentials: "include"`로 쿠키 전송. 두 곳(`request`, `uploadRequest`)에서 `BASE_URL` 사용
- `/frontend/CLAUDE.md` — `@AGENTS.md` 위임 확인
- 이전 step 산출물: `backend/Dockerfile`, `backend/.dockerignore`, `backend/src/main/resources/application-prod.yml` (이 step은 frontend만 다루지만 운영 토폴로지 전체를 이해하기 위해 참고)

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 1. `frontend/next.config.ts` 수정

**사전 확인 필수**: `frontend/node_modules/next/dist/docs/`에서 standalone 출력 가이드를 찾아 Next 16에서 산출물 경로(`.next/standalone/`)와 엔트리(`server.js`)가 통상 패턴과 동일한지 검증. 다르면 `frontend/Dockerfile`의 `COPY` 경로를 그에 맞게 조정.

수정 결과(예시):

```ts
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
};

export default nextConfig;
```

핵심:
- `output: "standalone"`만 추가. 다른 옵션 무단 추가 금지.
- dev 환경 호환성을 위한 `rewrites`는 **이 step에서 추가하지 않음**. dev에서 frontend(3000) → backend(8080) 호출 흐름은 `BASE_URL = "/api"` + 별도 proxy 없이 깨질 수 있다. 이는 의도된 trade-off(운영 same-origin 우선). dev 시 사용자가 별도 nginx 또는 vite proxy를 띄울지 여부는 후속 결정.

### 2. `frontend/lib/api.ts` 한 줄 수정

`BASE_URL` 한 줄만 변경:

```ts
// 변경 전
const BASE_URL = "http://localhost:8080/api";
// 변경 후
const BASE_URL = "/api";
```

그 외 모든 코드(`request`, `uploadRequest`, `ApiError` 등)는 **건드리지 않는다**. `credentials: "include"`도 유지.

### 3. `frontend/Dockerfile` (multi-stage)

베이스: `node:20-alpine`. Next 16은 Node 18.18+ 지원이나 LTS 20 채택이 안정.

구조:

```dockerfile
# Stage 1: deps (의존성 캐시 최대화)
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# Stage 2: builder
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# Stage 3: runner (Next standalone)
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production \
    PORT=3000 \
    HOSTNAME=0.0.0.0
RUN addgroup -S nodejs && adduser -S nextjs -G nodejs
# standalone 산출물 복사 — Next 16 실제 경로 사전 검증 결과에 맞춤
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public
USER nextjs
EXPOSE 3000
CMD ["node", "server.js"]
```

핵심 규칙:
- **non-root user**(nextjs) 사용
- `HOSTNAME=0.0.0.0` 필수 — Next standalone은 기본적으로 localhost만 listen. 컨테이너 외부 접근을 위해 필요.
- `npm ci` 사용 (package-lock.json 기준 reproducible install)
- 빌드 타임에 `NEXT_PUBLIC_*` 환경변수가 inline되는데, **이 step에서는 사용하지 않음**. `BASE_URL = "/api"` 상대경로라 빌드 타임 env 불필요.

### 4. `frontend/.dockerignore`

빌드 컨텍스트 최소화:

```
node_modules
.next
.git
.gitignore
.idea
coverage
.env*
README.md
*.log
.DS_Store
```

### 5. 빌드 검증

순서가 중요하다 — 먼저 npm-level 검증으로 standalone 출력이 생성되는지 본 뒤 docker build:

```bash
cd frontend && npm run build
# 빌드 후 .next/standalone/server.js 파일이 생성되는지 확인
ls -la .next/standalone/ | head -5
```

`server.js`가 없으면 Next 16에서 standalone 엔트리 이름이 다를 수 있다 → `frontend/node_modules/next/dist/docs/`의 standalone 문서를 다시 확인하고 Dockerfile `CMD`를 그에 맞춰 수정.

docker 빌드:
```bash
docker build -t harness-frontend ./frontend
```

런타임 검증:
```bash
docker run --rm -d -p 3001:3000 --name hf-test harness-frontend
sleep 5
curl -i http://localhost:3001/  # 200 OK + Next.js HTML 또는 404 (어쨌든 서버 응답)
docker rm -f hf-test
```

Vitest 회귀 검증:
```bash
cd frontend && npm run test
# 기존 테스트 전체 통과해야 함
```

## Acceptance Criteria

```bash
cd frontend && npm run build
ls -la frontend/.next/standalone/server.js
cd frontend && npm run test
docker build -t harness-frontend ./frontend
docker run --rm -d -p 3001:3000 --name hf-test harness-frontend
sleep 5
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3001/
docker rm -f hf-test
```

- `npm run build` 성공 + `.next/standalone/server.js` 존재
- 기존 Vitest 회귀 없음
- `docker build` 성공
- 컨테이너 실행 후 `curl`이 5xx 외 응답을 반환 (200, 304, 404 등 — 서버가 떠 있다는 증거)

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - `BASE_URL = "/api"` 변경 후 Vitest 회귀 없는지 확인 — 기존 테스트가 fetch 모킹 시 절대경로를 가정하지 않는지 검사
   - standalone 산출물이 실제로 컨테이너 안에서 부팅하는지 확인 (`curl` 응답)
   - Next 16 standalone 엔트리가 통상과 다르면 즉시 Dockerfile 조정
3. 아키텍처 체크리스트를 확인한다:
   - frontend/AGENTS.md 지시(Next 16 문서 우선 참조)를 따랐는가?
   - ADR-017(BASE_URL `/api` 상대경로) 결정과 일치하는가?
   - CLAUDE.md CRITICAL 규칙(frontend/ 폴더 외부 작성 금지)을 위반하지 않았는가?
   - Step Contract의 Write Scope 밖(backend/, 루트 인프라 파일 등)을 수정하지 않았는가?
   - backend 코드를 동시에 수정하지 않았는가? (이 step은 frontend만)
4. 결과에 따라 `phases/8-deploy/index.json`의 해당 step을 업데이트한다.

## 금지사항

- backend 코드(`backend/`)를 수정하지 마라. 이유: 이 step은 frontend 한정. 룰 6 위반.
- `lib/api.ts`의 `BASE_URL` 외 다른 부분(`request`, `uploadRequest`, ApiError 등)을 리팩토링하지 마라. 이유: 회귀 위험 + scope 이탈.
- `next.config.ts`에 `output: "standalone"` 외 다른 옵션을 추가하지 마라. 이유: scope 이탈 + dev 동작 변경 위험.
- Dockerfile에서 `USER root` 그대로 두지 마라. 이유: 보안.
- `HOSTNAME` 환경변수를 빠뜨리지 마라. 이유: Next standalone이 localhost만 listen하면 컨테이너 외부 접근 불가.
- `frontend/node_modules/next/dist/docs/`를 읽지 않고 `server.js` 경로를 가정하지 마라. 이유: Next 16 변경 가능성 — AGENTS.md 지시 위반.
- `package.json` 의존성을 추가/변경/제거하지 마라. 이유: scope 이탈. Next/React 버전 그대로 사용.
- Vitest 기존 테스트의 expect/assert를 변경하지 마라. 이유: 회귀를 숨긴다.
- `TODO`, `stub` 등으로 핵심 기능을 대체하고 completed 처리하지 마라.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

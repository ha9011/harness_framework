# Step 3: ci-deploy-and-manual

## Step Contract

- Capability: GitHub Actions workflow(main push → GHCR 빌드/push → SSH 배포) + 운영 배포 매뉴얼 문서
- Layer: integration-hardening
- Write Scope: `.github/workflows/deploy.yml`, `docs/DEPLOYMENT.md`
- Out of Scope: backend/ 코드, frontend/ 코드, docker-compose.prod.yml, nginx 설정, Dockerfile (모두 Step 0/1/2에서 완료)
- Critical Gates: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy.yml'))"` YAML syntax 무에러 + `grep -q "appleboy/ssh-action" .github/workflows/deploy.yml && grep -q "ghcr.io" .github/workflows/deploy.yml && grep -q "build-backend:" .github/workflows/deploy.yml && grep -q "build-frontend:" .github/workflows/deploy.yml` workflow 핵심 구조 존재 + `for s in A B C D E F G; do grep -q "^## $s\." docs/DEPLOYMENT.md || exit 1; done` DEPLOYMENT.md A~G 7개 섹션 모두 존재

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md` — Phase 8-deploy 전체 계획, Step 3의 산출물 정의
- `/docs/ADR.md` — ADR-016(GHCR + appleboy/ssh-action 선택 근거)
- `/docs/ARCHITECTURE.md` — CI/CD 흐름 다이어그램, 운영 시크릿 명세
- `/Users/hadong/.claude/plans/deep-giggling-yao.md` — 단계별 매뉴얼(A~G) 원본. DEPLOYMENT.md는 이 내용을 운영 매뉴얼 형식으로 정리한다.
- 이전 step 산출물:
  - `docker-compose.prod.yml` — deploy job의 `docker compose -f docker-compose.prod.yml pull && up -d` 명령 정합
  - `.env.example` — 미니PC 시크릿 작성 가이드 작성 시 참고
  - `nginx/conf.d/default.conf` — 매뉴얼의 검증 단계(curl 응답)와 정합
- `/.github/` 디렉토리 존재 여부 (없으면 생성)

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

### 1. `.github/workflows/deploy.yml` (신규)

```yaml
name: Build and Deploy

on:
  push:
    branches: [main]
  workflow_dispatch:

env:
  REGISTRY: ghcr.io
  IMAGE_BACKEND: ${{ github.repository }}-backend
  IMAGE_FRONTEND: ${{ github.repository }}-frontend

jobs:
  build-backend:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: ./backend
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_BACKEND }}:latest
            ${{ env.REGISTRY }}/${{ env.IMAGE_BACKEND }}:${{ github.sha }}
          cache-from: type=gha,scope=backend
          cache-to: type=gha,scope=backend,mode=max

  build-frontend:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: ./frontend
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_FRONTEND }}:latest
            ${{ env.REGISTRY }}/${{ env.IMAGE_FRONTEND }}:${{ github.sha }}
          cache-from: type=gha,scope=frontend
          cache-to: type=gha,scope=frontend,mode=max

  deploy:
    needs: [build-backend, build-frontend]
    runs-on: ubuntu-latest
    steps:
      - name: SSH and redeploy
        uses: appleboy/ssh-action@v1.2.0
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          port: ${{ secrets.SSH_PORT || 22 }}
          script: |
            set -e
            cd /opt/harness
            git pull --ff-only
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d
            docker image prune -f
```

핵심 규칙:
- `permissions.packages: write`는 build job에만 부여(deploy는 packages 권한 불필요).
- `${{ secrets.SSH_PORT || 22 }}` — Secrets에 SSH_PORT를 등록하지 않은 경우 22 기본값.
- `cache-from/cache-to: type=gha`로 GitHub Actions 캐시 활용(빌드 속도 ↑).
- `image prune -f`로 미사용 이미지 정리(디스크 누적 방지). `-a` 옵션은 쓰지 않음(중요한 이미지 실수로 삭제 방지).

### 2. `docs/DEPLOYMENT.md` (신규)

`/Users/hadong/.claude/plans/deep-giggling-yao.md`의 "단계별 매뉴얼 (A~G)" 섹션을 운영 매뉴얼 형식으로 정리. 다음 7개 섹션을 **반드시** 포함:

- `## A. 로컬에서 코드 작업` — A는 이미 phase 8-deploy로 완료된 사항이므로 "이 단계는 phase 8-deploy 머지로 완료됨"이라고 짧게 명시하고 다음으로
- `## B. 미니PC 사전 셋업` — Docker 설치(apt 명령어), `/opt/harness` 디렉토리, deploy 전용 SSH 키 생성/등록, 리포지토리 클론, `.env` 작성, `openssl rand` 명령어
- `## C. GitHub 사전 셋업` — Secrets 등록(`SSH_HOST`, `SSH_USER`, `SSH_PORT`, `SSH_PRIVATE_KEY`), Workflow permissions Read and write
- `## D. DB 이전` — 로컬 `pg_dump`, `scp`로 미니PC 복사, `postgres` 컨테이너만 먼저 기동, `pg_restore`, 복원 검증
- `## E. 첫 푸시 + 첫 배포` — main에 푸시, Actions 진행 추적, GHCR 가시성 public 변경(또는 PAT login), 재실행
- `## F. 외부 접근 검증` — Cloudflare DNS A 레코드 확인, 브라우저 e2e, 로그 확인
- `## G. 일상 운영` — 일반 배포 흐름, 수동 재배포, 롤백, 자주 쓰는 docker compose 명령, 자주 마주칠 문제

각 섹션 내부에는 plan 파일의 명령어 블록을 그대로 옮긴다. `[local]`, `[minipc]`, `[github web]`, `[cloudflare web]` 표기 규약 유지.

문서 맨 앞에는 다음을 포함:
- `# 운영 배포 매뉴얼` 제목
- 한 줄 요약: 무엇을 위한 매뉴얼인지
- 전체 흐름 한 줄: `B → C → D → E → F → G` 순서
- 미니PC OS: Ubuntu/Debian 가정 명시
- 표기 규약 설명

문서 맨 뒤에는 후속 권장사항(Cloudflare Full strict 전환, SSH 강화, 백업 cron 등)을 짧게 정리한 섹션 추가.

분량은 길어도 무방하나, 사용자가 한 번에 스캔하기 어렵지 않게 섹션 헤더로 구분.

### 3. 검증

YAML syntax:
```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy.yml'))"
```

선택 검증(설치되어 있을 때만):
```bash
which actionlint && actionlint .github/workflows/deploy.yml || echo "actionlint not installed, skipping"
```

구조 검증:
```bash
grep -c "build-backend:" .github/workflows/deploy.yml
grep -c "build-frontend:" .github/workflows/deploy.yml
grep -c "deploy:" .github/workflows/deploy.yml
grep -c "appleboy/ssh-action" .github/workflows/deploy.yml
grep -c "ghcr.io" .github/workflows/deploy.yml
```

DEPLOYMENT.md 섹션 검증:
```bash
for s in A B C D E F G; do
  grep -q "^## $s\." docs/DEPLOYMENT.md || (echo "missing section $s"; exit 1)
done
echo "All sections present"
```

## Acceptance Criteria

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy.yml'))"
test "$(grep -c 'appleboy/ssh-action' .github/workflows/deploy.yml)" -ge 1
test "$(grep -c 'ghcr.io' .github/workflows/deploy.yml)" -ge 1
for s in A B C D E F G; do
  grep -q "^## $s\." docs/DEPLOYMENT.md || (echo "missing section $s"; exit 1)
done
echo "Step 3 AC passed"
```

- YAML syntax 검증 통과
- workflow에 필수 키워드(`appleboy/ssh-action`, `ghcr.io`, `build-backend`, `build-frontend`, `deploy`) 존재
- DEPLOYMENT.md에 A~G 7개 섹션 모두 존재
- 매뉴얼의 명령어 블록이 plan 파일과 정합

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - `yaml.safe_load`가 syntax 에러 없이 통과하는지 (잘못된 indent/tab은 즉시 catch됨)
   - DEPLOYMENT.md가 단순 헤더 나열이 아니라 실제 실행 가능한 명령어 블록을 포함하는지 (수동 검토)
   - workflow의 `permissions: packages: write`가 build job에만 부여되어 있는지(deploy job에는 없음 — 최소 권한 원칙)
3. 아키텍처 체크리스트를 확인한다:
   - ADR-016(GHCR + appleboy/ssh-action) 결정과 일치하는가?
   - 이전 step 산출물(`docker-compose.prod.yml`)의 파일명/경로와 workflow의 `docker compose -f docker-compose.prod.yml ...`가 일치하는가?
   - Step Contract의 Write Scope 밖(backend/, frontend/, 루트 인프라 파일)을 수정하지 않았는가?
   - backend/frontend 코드를 동시에 수정하지 않았는가? (이 step은 문서/CI 한정)
4. 결과에 따라 `phases/8-deploy/index.json`의 해당 step을 업데이트한다.

## 금지사항

- backend/ 또는 frontend/ 코드를 수정하지 마라. 이유: 이 step의 Write Scope 밖.
- workflow의 `permissions`에 `packages: write`를 deploy job에도 추가하지 마라. 이유: 최소 권한 원칙. deploy는 GHCR push가 필요 없다.
- workflow에 시크릿 값(SSH key 본문, GHCR token 등)을 평문으로 박지 마라. 이유: 시크릿 노출. 반드시 `${{ secrets.* }}` 또는 자동 발급 `${{ secrets.GITHUB_TOKEN }}` 사용.
- DEPLOYMENT.md에 실제 도메인/IP/사용자명을 박지 마라. 이유: `<your-domain>`, `<minipc-user>`, `<minipc-host>` 등 placeholder로 두어야 한다. 운영 정보 유출 방지.
- DEPLOYMENT.md에 실제 시크릿 값을 예시로도 적지 마라. 이유: 시크릿 노출. `__CHANGE_ME__` 또는 `<openssl rand ...>` 표기.
- workflow trigger를 main 외 브랜치로 확장하지 마라. 이유: scope 이탈. main push만 자동 배포 대상.
- `docker image prune -af` (강제 + 모든 미사용 이미지)를 쓰지 마라. 이유: 의도치 않은 이미지 삭제. `-f`만 사용해 dangling 이미지만 정리.
- `TODO`, `stub`, "추후 추가" 같은 문구로 DEPLOYMENT.md 섹션을 비워두지 마라. 이유: 매뉴얼이 미완성이면 운영 차단.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.

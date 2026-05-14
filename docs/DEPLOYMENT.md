# 운영 배포 매뉴얼

영어 패턴 학습기(Cozy Cafe)를 사용자 미니PC에 도커로 배포하고 GitHub Actions로 자동 배포 파이프라인을 운영하기 위한 1회성 셋업 + 일상 운영 매뉴얼.

**전체 흐름**: `B → C → D → E → F → G` 순서로 진행한다. A는 phase 8-deploy가 main에 머지되는 시점에 자동으로 완료된다.

**대상 환경**: 미니PC OS는 **Ubuntu/Debian 계열 Linux** 가정. 사용자는 노트북에서 **SSH로 미니PC에 원격 접속**해 작업한다.

**표기 규약**:
- `[local]` = 본인 노트북(개발기)에서 실행
- `[minipc]` = 미니PC에 SSH 접속한 상태에서 실행
- `[github web]` = GitHub 웹 UI에서 클릭으로 작업
- `[cloudflare web]` = Cloudflare 대시보드에서 클릭으로 작업
- 꺾쇠괄호 `<...>`는 사용자가 자기 값으로 치환 (예: `<minipc-user>`, `<your-domain>`)

---

## A. 로컬에서 코드 작업

이 단계는 phase `8-deploy` 브랜치 머지로 완료됨. 리포지토리에 다음 파일이 이미 포함되어 있다:

- `backend/Dockerfile`, `backend/.dockerignore`
- `frontend/Dockerfile`, `frontend/.dockerignore`, `frontend/next.config.ts` (`output: "standalone"`), `frontend/lib/api.ts` (`BASE_URL = "/api"`)
- `backend/src/main/resources/application-prod.yml`
- `docker-compose.prod.yml`, `.env.example`, `.gitignore` (`.env` 추가)
- `nginx/conf.d/default.conf`
- `.github/workflows/deploy.yml`

추가 작업이 필요하다면 새 브랜치에서 PR을 만들고 main에 머지하면 자동으로 CI가 빌드/배포한다. 이번 매뉴얼의 1회성 셋업은 B부터 시작한다.

---

## B. 미니PC 사전 셋업

미니PC에 SSH로 접속한 상태에서 진행. 미니PC가 새 PC라면 OS 설치 + SSH 활성화 + 공유기 80/22 포트포워딩이 선행되어 있어야 한다.

### B-1. Docker + Docker Compose 플러그인 설치

```bash
[minipc] sudo apt update
[minipc] sudo apt install -y ca-certificates curl gnupg
[minipc] sudo install -m 0755 -d /etc/apt/keyrings
[minipc] curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
           sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
[minipc] sudo chmod a+r /etc/apt/keyrings/docker.gpg
[minipc] echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
           https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | \
           sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
[minipc] sudo apt update
[minipc] sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
[minipc] sudo usermod -aG docker $USER     # 그룹 변경 반영 위해 SSH 재접속 1회 필요
```

> Debian 계열이면 `ubuntu`를 `debian`으로 치환. Raspberry Pi/ARM에서도 동일 절차.

SSH 재접속 후 동작 확인:

```bash
[minipc] docker version
[minipc] docker compose version       # v2.x 표시
[minipc] docker run --rm hello-world  # 정상 동작 확인
```

### B-2. 배포 디렉토리 준비

```bash
[minipc] sudo mkdir -p /opt/harness /opt/harness/logs
[minipc] sudo chown -R $USER:$USER /opt/harness
```

`/opt/harness/logs`는 backend 컨테이너의 `/app/logs`로 바인드 마운트되어, 호스트에서 `tail -f`로 로그를 직접 확인할 수 있게 된다.

### B-3. 미니PC에 deploy 전용 SSH 키 등록

GitHub Actions가 22번 포트로 들어올 때 사용할 키를 만든다. 이 키는 **사용자 본인 노트북의 키와 별개**.

로컬에서 키 페어 생성:

```bash
[local] ssh-keygen -t ed25519 -C "gha-deploy" -f ~/.ssh/gha_deploy -N ""
# 만들어지는 파일: ~/.ssh/gha_deploy(개인키), ~/.ssh/gha_deploy.pub(공개키)
```

공개키만 미니PC `authorized_keys`에 추가:

```bash
[local] ssh-copy-id -i ~/.ssh/gha_deploy.pub <minipc-user>@<minipc-host>
# 또는 수동:
# cat ~/.ssh/gha_deploy.pub | ssh <minipc-user>@<minipc-host> "cat >> ~/.ssh/authorized_keys"
```

연결 확인:

```bash
[local] ssh -i ~/.ssh/gha_deploy <minipc-user>@<minipc-host> "echo ok"
# "ok"가 출력되면 통과
```

> 개인키 `~/.ssh/gha_deploy` 내용 전체(맨 위 `-----BEGIN OPENSSH PRIVATE KEY-----`부터 맨 아래 `-----END OPENSSH PRIVATE KEY-----`까지, 마지막 줄바꿈 포함)를 곧 GitHub Secrets에 붙여넣는다. 잠깐 메모장에 복사해 두되, **절대로 깃에 커밋하지 말 것**.

### B-4. 리포지토리 클론

```bash
[minipc] cd /opt/harness
[minipc] git clone https://github.com/<your-org-or-user>/harness_framework.git .
```

리포지토리가 사설이라면 deploy 키 또는 PAT(`read:repo`)로 클론. main에 이미 phase 8-deploy 산출물이 머지되어 있어야 `docker-compose.prod.yml`, `.env.example`이 보인다.

### B-5. `.env` 작성 (시크릿)

```bash
[minipc] cd /opt/harness
[minipc] cp .env.example .env
[minipc] chmod 600 .env
```

`.env`를 편집해 placeholder를 실제 값으로 교체:

```
POSTGRES_DB=english_app
POSTGRES_USER=app
POSTGRES_PASSWORD=<openssl rand -base64 32 결과>
GEMINI_API_KEY=<운영용 새 키>
GEMINI_MODEL=gemini-2.5-flash
JWT_SECRET=<openssl rand -base64 48 결과 (32자 이상)>
```

랜덤 값 생성:

```bash
[minipc] openssl rand -base64 32   # POSTGRES_PASSWORD용
[minipc] openssl rand -base64 48   # JWT_SECRET용 (48바이트 → 64자)
```

> Gemini API 키는 https://aistudio.google.com/apikey 에서 **운영용으로 새로 발급**. 로컬에서 쓰던 키 재사용 금지. `.env` 파일은 `chmod 600`으로 본인만 읽을 수 있게 권한을 좁힌다.

---

## C. GitHub 사전 셋업

`[github web]`에서 진행. 미니PC 셋업이 끝난 뒤 수행한다.

### C-1. Actions Secrets 등록

Repository → Settings → Secrets and variables → Actions → "New repository secret"로 다음 4개 추가:

| Name | Value |
|---|---|
| `SSH_HOST` | 미니PC 공인 IP 또는 미니PC에 연결된 도메인 (`<minipc-host>`) |
| `SSH_USER` | 미니PC 로그인 사용자명 (`<minipc-user>`) |
| `SSH_PORT` | `22` (또는 변경된 SSH 포트) |
| `SSH_PRIVATE_KEY` | B-3에서 만든 `~/.ssh/gha_deploy` 파일 **전체 내용**(헤더/푸터 포함) |

> `SSH_PRIVATE_KEY` 붙여넣기 시 마지막 줄 끝의 줄바꿈도 그대로 포함시켜야 일부 SSH 라이브러리가 정상 파싱한다. 노출이 의심되면 즉시 키 회전(미니PC `authorized_keys`에서 해당 공개키 제거 + GitHub Secrets 갱신).

### C-2. Actions workflow 권한 허용

Settings → Actions → General → Workflow permissions 섹션에서 **"Read and write permissions"** 라디오를 선택 후 Save.

GHCR에 이미지를 push하려면 `packages: write` 권한이 필요하고, 이 토글이 활성화되어 있어야 한다.

### C-3. (첫 배포 후 진행) GHCR 패키지 가시성

이 항목은 첫 빌드가 push된 다음에야 의미가 있다. 일단 표시만 해두고 **E단계에서 처리**.

---

## D. DB 이전

로컬 PostgreSQL의 데이터를 미니PC 운영 DB로 가져오는 1회성 작업. `ddl-auto: validate`이므로 backend 컨테이너가 부팅하기 전에 반드시 복원이 끝나 있어야 한다.

### D-1. 로컬에서 덤프 생성

```bash
[local] pg_dump -h localhost -p 5432 -U app -d english_app -F c -f english_app.dump
# 비밀번호 묻으면 app1234 (로컬 docker-compose.yml 기본값)
```

`-F c`는 custom format(`pg_restore` 전용). 압축률이 높고 선택 복원이 가능하다.

### D-2. 미니PC로 복사

```bash
[local] scp english_app.dump <minipc-user>@<minipc-host>:/opt/harness/
```

### D-3. 미니PC에서 postgres만 먼저 기동

```bash
[minipc] cd /opt/harness
[minipc] git pull --ff-only
[minipc] docker compose -f docker-compose.prod.yml up -d postgres
[minipc] docker compose -f docker-compose.prod.yml ps    # postgres만 running, healthy 확인
```

postgres가 `healthy` 상태가 될 때까지 10초 정도 기다린다.

### D-4. 덤프 복원

```bash
[minipc] docker compose -f docker-compose.prod.yml exec -T postgres \
           pg_restore -U app -d english_app --no-owner --clean --if-exists \
           < /opt/harness/english_app.dump
```

중간에 일부 NOTICE/ERROR가 보일 수 있으나, 본 데이터 객체 복원이 끝나면 정상.

### D-5. 복원 검증

```bash
[minipc] docker compose -f docker-compose.prod.yml exec postgres \
           psql -U app -d english_app -c '\dt'
# 테이블 목록이 출력되어야 함 (users, words, patterns, review_items 등)

[minipc] docker compose -f docker-compose.prod.yml exec postgres \
           psql -U app -d english_app -c 'SELECT count(*) FROM users;'
# 로컬과 동일한 사용자 수가 출력되어야 함
```

### D-6. 덤프 파일 보관

복원 성공 확인 후 `/opt/harness/english_app.dump`는 그대로 두거나 `/opt/harness/backups/initial.dump`로 옮겨 보관(롤백/복구용 1회 백업).

```bash
[minipc] mkdir -p /opt/harness/backups
[minipc] mv /opt/harness/english_app.dump /opt/harness/backups/initial.dump
```

---

## E. 첫 푸시 + 첫 배포

### E-1. main에 push (또는 PR 머지)

phase `8-deploy` 브랜치가 아직 머지 전이라면:

```bash
[local] git push -u origin feat-8-deploy
# GitHub 웹에서 PR 생성 → 리뷰 → main에 머지
```

이미 머지된 상태라면 workflow_dispatch로 수동 트리거하거나 빈 커밋으로 트리거할 수 있다.

### E-2. Actions 진행 추적

Repository → Actions 탭. "Build and Deploy" workflow의 첫 실행이 생성된다.

- `build-backend`, `build-frontend` job이 병렬로 5~10분 정도 빌드(첫 빌드는 의존성 캐시가 없어 오래 걸림. 이후엔 `type=gha` 캐시로 단축)
- 둘 다 성공하면 `deploy` job이 SSH로 미니PC 접속

### E-3. GHCR 패키지 가시성 변경 (또는 PAT 로그인)

미니PC에서 `unauthorized: denied`로 deploy 단계가 실패할 가능성이 높다. 첫 push가 끝나면 GitHub Repository → Packages 탭에 `harness_framework-backend`, `harness_framework-frontend`가 생긴다.

**옵션 A (권장)**: 각 패키지 클릭 → Package settings → Danger Zone → "Change visibility" → **Public**으로 변경.

**옵션 B (private 유지)**: 미니PC에서 PAT로 GHCR에 로그인.

```bash
# [github web] Settings → Developer settings → Personal access tokens → Tokens (classic) →
#   "Generate new token (classic)" → scope: read:packages 만 체크 → 생성
[minipc] echo "<PAT 값>" | docker login ghcr.io -u <your-github-username> --password-stdin
```

### E-4. Actions 재실행

E-3에서 가시성을 public으로 변경했다면, Actions 탭에서 실패한 run을 "Re-run jobs" → "Re-run failed jobs". 또는 main에 빈 커밋 하나 더 push:

```bash
[local] git commit --allow-empty -m "chore: trigger deploy"
[local] git push
```

### E-5. 미니PC 상태 검증

```bash
[minipc] docker compose -f docker-compose.prod.yml ps
# 4개 서비스 모두 running, postgres healthy

[minipc] docker compose -f docker-compose.prod.yml logs --tail=200 backend
# "Started ... in N seconds" 메시지가 보여야 함. Hibernate validate가 통과해야 함.

[minipc] curl -i http://localhost/
# 200 OK + Next.js HTML

[minipc] curl -i http://localhost/api/auth/me
# 401 + {"code":"UNAUTHORIZED",...}
```

---

## F. 외부 접근 검증

### F-1. Cloudflare DNS 확인

`[cloudflare web]` → 도메인 선택 → DNS 메뉴:

- A 레코드: `<subdomain or @>` → 미니PC 공인 IP
- Proxy status: **Proxied(주황색 구름)**
- SSL/TLS → Overview에서 모드가 **Flexible**

> 공인 IP가 동적이라면 DDNS 사용을 강력 권장(공유기 내장 또는 별도 클라이언트). IP가 바뀔 때마다 Cloudflare A 레코드 수동 갱신은 운영상 비현실적.

### F-2. 브라우저 e2e 테스트

브라우저 시크릿 창에서 `https://<your-domain>/` 접속:

1. Next.js 페이지 정상 렌더 확인
2. 회원가입 → 로그인 진행
3. DevTools Network 탭에서 로그인 응답의 `Set-Cookie`로 JWT 쿠키 발급 확인 (HttpOnly, Secure)
4. `/api/auth/me` 200 + 본인 정보 응답
5. 단어 등록 → Gemini 호출 흐름 → DB 저장 e2e 통과
6. 페이지 새로고침 후에도 로그인 유지 확인

### F-3. 로그 확인

```bash
[minipc] docker compose -f docker-compose.prod.yml logs -f backend
# traceId가 포함된 prod JSON 포맷 로그가 흐르는지 확인

[minipc] tail -f /opt/harness/logs/app.log
# 파일에도 동일 로그가 기록되는지 확인 (logback 파일 롤링이 동작)
```

`prod` 프로파일이 활성화되면 logback이 `/app/logs/app.log`(30일 롤링, 3GB 상한) 와 `/app/logs/error.log`(ERROR 이상, 90일 보관)를 생성한다. 컨테이너의 `/app/logs`는 호스트 `/opt/harness/logs`로 바인드되어 있어 직접 tail 가능.

---

## G. 일상 운영

### G-1. 일반 배포 흐름

- 코드 변경 → 브랜치에서 PR → 리뷰 → main 머지
- main push 트리거로 Actions가 자동으로 빌드/배포
- 평균 5~8분 후 미니PC에 새 이미지 반영

### G-2. 수동 재배포

`[github web]` Actions 탭 → "Build and Deploy" → 우상단 "Run workflow" → main 브랜치 선택 → Run.

(workflow의 `workflow_dispatch` 트리거를 사용한다.)

### G-3. 롤백 (이전 SHA로 복귀)

```bash
[minipc] cd /opt/harness

# docker-compose.prod.yml의 image: ...:latest 줄을
# image: ghcr.io/<repo>-backend:<old-sha> 형태로 임시 변경
# (frontend도 동일하게 변경)

[minipc] docker compose -f docker-compose.prod.yml pull
[minipc] docker compose -f docker-compose.prod.yml up -d
```

정상화 후에는 `:latest`로 되돌리고 main에 핫픽스를 머지하는 것이 정석. 임시 변경한 compose 파일은 git에 커밋하지 말 것.

### G-4. 자주 쓰는 docker compose 명령

```bash
[minipc] docker compose -f docker-compose.prod.yml ps             # 4 서비스 상태
[minipc] docker compose -f docker-compose.prod.yml logs -f backend
[minipc] docker compose -f docker-compose.prod.yml restart backend
[minipc] docker compose -f docker-compose.prod.yml down           # 전체 중지(볼륨 유지)
[minipc] docker compose -f docker-compose.prod.yml pull           # 최신 이미지 받기
[minipc] docker stats                                             # 메모리/CPU 실시간
[minipc] df -h /var/lib/docker                                    # 디스크 사용량 (이미지 누적 주의)
[minipc] docker image prune -f                                    # dangling 이미지만 청소
```

> `docker image prune -af`(태그까지 청소)는 의도치 않은 이미지 삭제를 일으킬 수 있어 일상 정리에는 `-f`만 사용한다.

### G-5. 자주 마주칠 문제

| 증상 | 원인 / 대응 |
|------|------------|
| deploy job이 `unauthorized: denied`로 실패 | GHCR 패키지가 private + 미니PC에 docker login 안 됨. E-3 참조 |
| backend 부팅 실패 `relation does not exist` | D단계 pg_restore가 빠졌거나 실패. D-5로 테이블 존재 확인 |
| backend 부팅 실패 `Could not resolve placeholder 'JWT_SECRET'` | `/opt/harness/.env`에 해당 키 누락 또는 compose의 `environment` 매핑 누락 |
| 외부 접근만 502 | Cloudflare가 origin과 통신 못 함. 공유기 80 포트포워딩, 미니PC 방화벽(`sudo ufw status`), `docker compose ps`의 nginx 상태 확인 |
| 외부 접근에서 무한 리다이렉트 | Cloudflare SSL mode가 Full인데 origin이 HTTPS 안 함. Flexible로 변경하거나 후속 권장사항 #1로 Full(strict) + Origin Certificate 셋업 |
| 파일 업로드 413 Request Entity Too Large | nginx `client_max_body_size` 누락. `nginx/conf.d/default.conf`에 `10M`로 설정되어 있어야 한다 |
| Actions가 `secrets.SSH_HOST` 등을 빈 문자열로 인식 | Secrets 등록을 organization-level이 아닌 repository-level로 했는지 확인 |
| `.env` 파일이 `git status`에 보임 | `.gitignore`의 `.env` 누락. 즉시 추가 + `git rm --cached .env` |

---

## 운영 시크릿 회전 절차

운영 시크릿이 노출된 정황이 있거나 주기적으로 회전할 때:

| 변수 | 회전 방법 |
|------|----------|
| `JWT_SECRET` | 미니PC `.env` 수정 → `docker compose -f docker-compose.prod.yml up -d backend` 재시작. **기존 발급된 JWT 모두 무효** → 사용자 재로그인 필요 |
| `POSTGRES_PASSWORD` | postgres 컨테이너 진입해 `ALTER USER app WITH PASSWORD '<new>'` → `.env` 갱신 → backend 재시작 |
| `GEMINI_API_KEY` | Google AI Studio에서 신규 발급 → `.env` 갱신 → backend 재시작. 구 키는 폐기 |
| `SSH_PRIVATE_KEY` | 로컬에서 신규 ed25519 키 생성 → 미니PC `authorized_keys`에 공개키 추가, 구 공개키 줄 삭제 → GitHub Secrets 갱신 |
| GitHub PAT (미니PC GHCR login용) | GitHub Tokens 페이지에서 재발급 → 미니PC에서 `docker login ghcr.io` 재실행 |

---

## 후속 권장사항 (이번 매뉴얼 범위 밖)

매뉴얼대로 운영이 안정화된 후 별도 phase에서 다룰 항목:

1. **Cloudflare Flexible → Full(strict) 전환** (보안 최우선)
   - 대시보드에서 Origin Certificate 발급 → 미니PC `./nginx/certs/`에 배치 → `nginx/conf.d/default.conf`를 `listen 443 ssl;` 형태로 교체 → docker-compose에 `443:443` 포트와 certs 볼륨 추가 → 공유기 443 포트포워딩 → CF SSL mode를 Full(strict)로 변경
   - 또는 **Cloudflare Tunnel**(`cloudflared` 컨테이너 추가)로 전환하면 포트포워딩 자체가 불필요
2. **SSH 강화**: `sshd_config`에서 `PasswordAuthentication no`, `PermitRootLogin no`. `fail2ban` 설치
3. **백업 cron**: 매일 새벽 `pg_dump`로 `/opt/harness/backups/`에 덤프, 14일 보관, 외부 스토리지(R2/S3)로 주기 동기화
4. **Spring Actuator 도입**: `/api/actuator/health`를 nginx upstream health check 또는 외부 헬스체크 도구에 연결
5. **CORS 외부화**: 외부 origin 호출 케이스가 생기면 `SecurityConfig`의 origin 리스트를 `app.cors.allowed-origins` 프로퍼티로 외부화
6. **모니터링/알림**: Slack/PagerDuty 연동, prod 로그를 ELK/CloudWatch로 전송

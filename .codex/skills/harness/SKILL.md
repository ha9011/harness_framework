---
name: harness
description: 이 프로젝트는 Harness 프레임워크를 사용한다. 아래 워크플로우에 따라 작업을 진행하라.
---

이 프로젝트는 Harness 프레임워크를 사용한다. 아래 워크플로우에 따라 작업을 진행하라.

---

## 워크플로우

### 0. 설계 문서 준비 (필수)

**이 단계는 `/prep` 스킬로 대체되었다.**

- `/prep`을 먼저 실행하여 문서를 준비하라.
- 이미 문서가 준비되어 있으면 (`docs/PLAN.md`가 존재하고 내용이 채워져 있으면) 확인만 하고 A단계로 넘어간다.
- `docs/PLAN.md`가 없거나 비어 있으면 → 사용자에게 **"`/prep`을 먼저 실행하세요"** 라고 안내하고 중단한다.

> **참고**: 프로젝트 전체 문서(PRD, ARCHITECTURE, ADR 등)는 작업이 완료되어도 유지된다. PLAN.md만 완료 시 archive로 이동한다.

### A. 탐색

`/docs/` 하위 문서(PRD, ARCHITECTURE, ADR 등)를 읽고 프로젝트의 기획·아키텍처·설계 의도를 파악한다.

### B. 논의

구현을 위해 구체화하거나 기술적으로 결정해야 할 사항이 있으면 사용자에게 제시하고 논의한다.

### C. Step 설계

사용자가 구현 계획 작성을 지시하면 여러 step으로 나뉜 초안을 작성해 피드백을 요청한다.

설계 원칙:

1. **Scope 최소화** — 한 step은 하나의 capability와 하나의 layer만 담당한다. 여러 capability, 여러 도메인, 여러 layer가 섞이면 반드시 step을 쪼갠다.
2. **자기완결성** — 각 step 파일은 독립된 AI 세션에서 실행된다. "이전 대화에서 논의한 바와 같이" 같은 외부 참조는 금지한다. 필요한 정보는 전부 파일 안에 적는다.
3. **사전 준비 강제** — 관련 문서 경로와 이전 step에서 생성/수정된 파일 경로를 명시한다. 세션이 코드를 읽고 맥락을 파악한 뒤 작업하도록 유도한다.
4. **시그니처 수준 지시** — 함수/클래스의 인터페이스만 제시하고 내부 구현은 에이전트 재량에 맡긴다. 단, 설계 의도에서 벗어나면 안 되는 핵심 규칙(멱등성, 보안, 데이터 무결성 등)은 반드시 명시한다.
5. **Layer 명시** — 모든 step은 `domain`, `service`, `controller`, `external-client`, `frontend-view`, `integration-hardening` 중 하나를 `Step Contract`의 `Layer`에 명시한다.
6. **백엔드/프론트 분리** — 한 step에서 `backend/`와 `frontend/`를 동시에 수정하지 않는다. 단, 연결 검증과 최종 조정만 담당하는 `integration-hardening` step은 예외다.
7. **외부 연동 분리** — 외부 API/SDK 연동은 반드시 `external-client` 단독 step으로 분리한다. service, controller, frontend UI와 섞지 않는다.
8. **DB 분리** — DB 스키마, 마이그레이션, repository/persistence 작업은 backend service/controller/frontend UI와 한 step에 섞지 않는다. 해당 capability만 다루는 별도 step으로 둔다.
9. **Critical Gates는 핵심 동작 증명** — 단순 `npm run build`, `npm run test`만 쓰지 말고, 이 step의 핵심 기능이 실제 동작함을 증명하는 테스트/명령을 포함한다.
10. **외부 연동 검증** — `external-client` step의 Critical Gates는 실제 네트워크를 호출하지 않는 fake provider/fake server 테스트로 request 구성, 인증 정보 주입, 응답 파싱, 실패 처리를 검증해야 한다.
11. **AC는 실행 가능한 커맨드** — "~가 동작해야 한다" 같은 추상적 서술이 아닌 실제 실행 가능한 검증 커맨드를 포함한다.
12. **주의사항은 구체적으로** — "조심해라" 대신 "X를 하지 마라. 이유: Y" 형식으로 적는다.
13. **네이밍** — step name은 kebab-case slug로, 해당 step의 핵심 모듈/작업을 한두 단어로 표현한다 (예: `project-setup`, `api-layer`, `auth-flow`).

### D. 파일 생성

사용자가 승인하면 아래 파일들을 생성한다.

#### D-1. `phases/index.json` (전체 현황)

여러 task를 관리하는 top-level 인덱스. 이미 존재하면 `phases` 배열에 새 항목을 추가한다.

```json
{
  "phases": [
    {
      "dir": "0-mvp",
      "status": "pending"
    }
  ]
}
```

- `dir`: task 디렉토리명.
- `status`: `"pending"` | `"completed"` | `"error"` | `"blocked"`. executor가 실행 중 자동으로 업데이트한다.
- 타임스탬프(`completed_at`, `failed_at`, `blocked_at`)는 executor가 상태 변경 시 자동 기록한다. 생성 시 넣지 않는다.

#### D-2. `phases/{task-name}/index.json` (task 상세)

```json
{
  "project": "<프로젝트명>",
  "phase": "<task-name>",
  "steps": [
    { "step": 0, "name": "project-setup", "status": "pending" },
    { "step": 1, "name": "core-types", "status": "pending" },
    { "step": 2, "name": "api-layer", "status": "pending" }
  ]
}
```

필드 규칙:

- `project`: 프로젝트명 (AGENTS.md 참조).
- `phase`: task 이름. 디렉토리명과 일치시킨다.
- `steps[].step`: 0부터 시작하는 순번.
- `steps[].name`: kebab-case slug.
- `steps[].status`: 초기값은 모두 `"pending"`.

상태 전이와 자동 기록 필드:

| 전이 | 기록되는 필드 | 기록 주체 |
|------|-------------|----------|
| → `completed` | `completed_at`, `summary` | AI 세션 (summary), executor (timestamp) |
| → `error` | `failed_at`, `error_message` | AI 세션 (message), executor (timestamp) |
| → `blocked` | `blocked_at`, `blocked_reason` | AI 세션 (reason), executor (timestamp) |

`summary`는 step 완료 시 산출물을 한 줄로 요약한 것으로, executor가 다음 step 프롬프트에 컨텍스트로 누적 전달한다. 따라서 다음 step에 유용한 정보(생성된 파일, 핵심 결정 등)를 담아야 한다.

`created_at`은 executor가 최초 실행 시 task 레벨에 한 번만 기록한다. step 레벨의 `started_at`도 executor가 각 step 시작 시 자동 기록한다. 생성 시 넣지 않는다.

#### D-3. `phases/{task-name}/step{N}.md` (각 step마다 1개)

```markdown
# Step {N}: {이름}

## Step Contract

- Capability: {이 step이 완성할 단일 capability}
- Layer: {domain | service | controller | external-client | frontend-view | integration-hardening 중 하나}
- Write Scope: {수정 가능한 파일/디렉토리. backend와 frontend를 동시에 쓰지 말 것. integration-hardening만 예외}
- Out of Scope: {이 step에서 절대 하지 않을 capability/layer/file}
- Critical Gates: {핵심 기능이 실제 동작함을 증명하는 테스트/명령. 단순 build/test 전체 실행만 금지}

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- {이전 step에서 생성/수정된 파일 경로}

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

{구체적인 구현 지시. 파일 경로, 클래스/함수 시그니처, 로직 설명을 포함.
코드 스니펫은 인터페이스/시그니처 수준만 제시하고, 구현체는 에이전트에게 맡겨라.
단, 설계 의도에서 벗어나면 안 되는 핵심 규칙은 명확히 박아넣어라.}

## Acceptance Criteria

```bash
{Step Contract의 Critical Gates에 적은 핵심 기능 검증 커맨드}
npm run build   # 보조 검증: 컴파일 에러 없음
npm test        # 보조 검증: 전체 테스트 회귀 없음
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Critical Gates가 이 step의 핵심 capability를 실제로 검증했는지 확인한다:
   - 단순 build/test 전체 실행만으로 completed 처리하지 않았는가?
   - `external-client` step이면 fake provider/fake server 테스트로 request 구성, 인증 정보 주입, 응답 파싱, 실패 처리를 검증했는가?
3. 아키텍처 체크리스트를 확인한다:
   - ARCHITECTURE.md 디렉토리 구조를 따르는가?
   - ADR 기술 스택을 벗어나지 않았는가?
   - AGENTS.md CRITICAL 규칙을 위반하지 않았는가?
   - Step Contract의 Write Scope 밖을 수정하지 않았는가?
   - `integration-hardening` 외 step에서 backend와 frontend를 동시에 수정하지 않았는가?
4. 결과에 따라 `phases/{task-name}/index.json`의 해당 step을 업데이트한다:
   - 성공 → `"status": "completed"`, `"summary": "산출물 한 줄 요약"`
   - 수정 3회 시도 후에도 실패 → `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 (API 키, 외부 인증, 수동 설정 등) → `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

## 금지사항

- {이 step에서 하지 말아야 할 것. "X를 하지 마라. 이유: Y" 형식}
- `TODO`, `not implemented`, `stub`, 빈 배열/빈 객체, 고정 더미 반환으로 핵심 기능을 대체하고 completed 처리하지 마라. 이유: 미구현을 완료로 오판하는 것을 방지한다.
- "다음 step에서 연결 예정", "후속 step에서 구현 예정" 같은 문구를 남기고 completed 처리하지 마라. 이유: step은 자기 capability를 완료해야 한다.
- 핵심 adapter/client/service가 실제로 동작하지 않으면 completed가 아니라 `blocked` 또는 `error`로 처리하라. 이유: 다음 step이 깨진 기반 위에서 진행되는 것을 방지한다.
- 기존 테스트의 기대값(expect/assert)을 변경하지 마라. 이유: AI가 버그를 숨기기 위해 테스트를 조작하는 것을 방지한다. 변경이 불가피하면 summary에 "⚠️ 테스트 변경: {사유}"를 반드시 기록하고, 그 사유는 PRD/ADR에 근거해야 한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 하네스가 자동으로 처리한다.
```

### E. 실행

```bash
cd harness && npx tsx src/cli.ts {task-name}              # 순차 실행
cd harness && npx tsx src/cli.ts {task-name} --push       # 실행 후 push
cd harness && npx tsx src/cli.ts {task-name} --dry-run    # 프롬프트만 확인
```

executor가 자동으로 처리하는 것:

- `feat-{task-name}` 브랜치 생성/checkout
- step lint — 모든 step 파일의 `Step Contract` 필수 필드, 허용 Layer, Write Scope, backend/frontend 동시 수정 금지, external-client fake provider/server Critical Gates를 실행 전 검증
- 가드레일 주입 — AGENTS.md + docs/*.md 내용을 매 step 프롬프트에 포함
- 컨텍스트 누적 — 완료된 step의 summary를 다음 step 프롬프트에 전달
- 자가 교정 — 실패 시 최대 3회 재시도하며, 이전 에러 메시지를 프롬프트에 피드백
- 2단계 커밋 — 코드 변경(`feat`)과 메타데이터(`chore`)를 분리 커밋
- 타임스탬프 — started_at, completed_at, failed_at, blocked_at 자동 기록
- Zod 스키마 검증 — index.json, step output 등의 구조를 런타임에서 검증

에러 복구:

- **error 발생 시**: `phases/{task-name}/index.json`에서 해당 step의 `status`를 `"pending"`으로 바꾸고 `error_message`를 삭제한 뒤 재실행한다.
- **blocked 발생 시**: `blocked_reason`에 적힌 사유를 해결한 뒤, `status`를 `"pending"`으로 바꾸고 `blocked_reason`을 삭제한 뒤 재실행한다.

### F. 완료 후 아카이브 (자동)

executor `finalize()`에서 `archivePlan()`을 호출하여 **자동으로 수행**한다. 수동 작업 불필요.

executor가 자동으로 처리하는 것:
1. `docs/archive/` 디렉토리 생성 (없으면)
2. `docs/PLAN.md` → `docs/archive/PLAN_{날짜}_{task-name}.md` 이동
3. "다음 작업은 /prep부터 시작하세요" 안내 출력

※ PRD.md, ARCHITECTURE.md, ADR.md는 이동하지 않는다 (프로젝트 전체 문서).

# 하네스(Harness) 가이드

AI에게 코드 구현을 시킬 때, 작업을 step 단위로 쪼개서 순차 실행하고, 실패하면 자동 재시도하는 프레임워크.

---

## 전체 흐름

```
[사용자]                        [하네스]                         [AI (Claude)]
   │                               │                                │
   │  1. /prep 실행                │                                │
   │  ─────────────────────────>   │                                │
   │  문서 준비 (PRD, PLAN 등)     │                                │
   │                               │                                │
   │  2. /harness 실행             │                                │
   │  ─────────────────────────>   │                                │
   │                               │  step lint (구조 검증)         │
   │                               │  ──────────────────>           │
   │                               │                                │
   │                               │  step 0 프롬프트 전송          │
   │                               │  ─────────────────────────>    │
   │                               │                    코드 작성   │
   │                               │  <─────────────────────────    │
   │                               │  결과 확인 + git commit        │
   │                               │                                │
   │                               │  step 1 프롬프트 전송          │
   │                               │  ─────────────────────────>    │
   │                               │                    코드 작성   │
   │                               │  <─────────────────────────    │
   │                               │  결과 확인 + git commit        │
   │                               │                                │
   │                               │  ... (모든 step 반복)          │
   │                               │                                │
   │                               │  finalize                      │
   │                               │  (DEFERRED.md 저장,            │
   │                               │   PLAN.md 아카이브)            │
   │  완료!                        │                                │
```

---

## 워크플로우 단계

| 단계 | 무엇을 하나 | 누가 하나 |
|------|------------|----------|
| `/prep` | 요구사항을 문서로 정리 (PRD, ARCHITECTURE, ADR, PLAN) | 사용자 + AI |
| `/harness` A~C | 문서 탐색 → 논의 → step 설계 | 사용자 + AI |
| `/harness` D | phase 디렉토리와 step 파일 생성 | AI |
| `/harness` E | executor로 step 순차 실행 | 하네스 (자동) |
| `/harness` F | PLAN.md 아카이브, 미룬 작업 리포트 | 하네스 (자동) |

---

## 핵심 개념

### 1. Step Contract (단계 계약서)

모든 step 파일(`step0.md`, `step1.md`, ...)에 반드시 포함되는 섹션.

```markdown
## Step Contract

- Capability: 이 step이 구현할 기능
- Layer: domain | service | controller | external-client | frontend-view | integration-hardening
- Write Scope: 수정 가능한 파일/폴더
- Out of Scope: 이 step에서 하지 않을 것
- Critical Gates: 핵심 기능이 동작함을 증명하는 테스트/명령
```

**규칙:**
- backend와 frontend를 한 step에서 동시에 수정하지 않는다 (integration-hardening 예외)
- Critical Gates에 단순 `npm test`만 쓰지 않는다 — 핵심 기능을 직접 검증하는 명령을 포함

### 2. 2단계 커밋

step 하나가 완료되면 git commit이 **2번** 일어난다.

```
1단계: feat(mvp): step 0 — project-setup    ← 실제 코드 변경
2단계: chore(mvp): step 0 output            ← index.json, output 파일
```

코드 변경과 메타데이터를 분리해서 git 히스토리를 깔끔하게 유지한다.

### 3. 상태 전이 (FSM)

각 step의 상태는 아래처럼 전이된다.

```
         ┌─────────┐
         │ pending │
         └────┬────┘
              │
     ┌────────┼────────┐
     ▼        ▼        ▼
┌─────────┐ ┌─────┐ ┌────────┐
│completed│ │error│ │blocked │
└─────────┘ └──┬──┘ └────────┘
               │
          (최대 3회)
               │
         ┌─────┴────┐
         │ pending  │  ← 자동 재시도
         └──────────┘
```

- **completed**: 성공. summary에 산출물 요약 기록
- **error**: 실패. 최대 3회 재시도 후에도 실패하면 중단
- **blocked**: 사용자 개입 필요 (API 키, 인증 등). 즉시 중단

---

## 파일 구조

```
프로젝트/
├── CLAUDE.md                          # 프로젝트 규칙
├── docs/
│   ├── PRD.md                         # 제품 요구사항
│   ├── ARCHITECTURE.md                # 아키텍처
│   ├── ADR.md                         # 기술 결정 기록
│   ├── PLAN.md                        # 현재 작업 계획 (완료 후 archive로 이동)
│   ├── DEFERRED.md                    # 미룬 작업 체크리스트
│   └── archive/                       # 완료된 PLAN 보관
├── phases/
│   ├── index.json                     # 전체 phase 목록
│   └── 0-mvp/
│       ├── index.json                 # step 목록 + 상태
│       ├── step0.md                   # step 0 지시문
│       ├── step0-output.json          # step 0 AI 응답
│       ├── step1.md
│       └── step1-output.json
└── harness/
    └── src/
        ├── cli.ts                     # 터미널 진입점
        ├── executor.ts                # 핵심 오케스트레이터
        ├── claude.ts                  # AI 호출
        ├── git.ts                     # 브랜치/커밋/푸시
        ├── guardrails.ts              # 프롬프트 조립
        ├── step-lint.ts               # Step Contract 검증
        └── fsm.ts                     # 상태 전이 검증
```

### 파일별 역할

| 파일 | 한 줄 설명 |
|------|-----------|
| `cli.ts` | 터미널에서 `npx tsx src/cli.ts 0-mvp` 받아서 executor 호출 |
| `executor.ts` | step을 순서대로 꺼내서 AI에게 시키고, 결과 확인하고, 커밋하는 지휘자 |
| `claude.ts` | Claude CLI를 spawn해서 프롬프트 보내고 응답 받는 전화기 |
| `git.ts` | 브랜치 만들기, 2단계 커밋, push 담당 |
| `guardrails.ts` | CLAUDE.md + docs/ 문서를 읽어서 AI 프롬프트에 규칙으로 붙이는 역할 |
| `step-lint.ts` | step 파일에 Step Contract가 있는지, 필수 필드가 채워졌는지 검사 |
| `fsm.ts` | pending→completed 같은 상태 전이가 올바른지 검증 |

---

## 실행 방법

```bash
# 1. 문서 준비
/prep

# 2. step 설계 + 파일 생성
/harness

# 3. 실행
cd harness && npx tsx src/cli.ts 0-mvp              # 순차 실행
cd harness && npx tsx src/cli.ts 0-mvp --push       # 실행 후 push
cd harness && npx tsx src/cli.ts 0-mvp --dry-run    # 프롬프트만 확인 (실행 안 함)
```

---

## 에러 복구

### step이 error로 실패했을 때

1. `phases/{task-name}/index.json` 열기
2. 해당 step의 `status`를 `"pending"`으로 변경
3. `error_message` 필드 삭제
4. 다시 실행

### step이 blocked로 중단됐을 때

1. `blocked_reason`에 적힌 사유 해결 (예: API 키 설정)
2. `status`를 `"pending"`으로 변경
3. `blocked_reason` 필드 삭제
4. 다시 실행

---

## 미룬 작업 관리

step 실행 중 "이건 다음 step에서 해야 한다"는 작업이 생기면, AI가 summary에 `⏳ 미룬 작업: {내용}`을 기록한다.

phase가 끝나면 executor가 자동으로 `docs/DEFERRED.md`에 체크리스트로 모아준다.

```markdown
# 미룬 작업

## Phase: 0-mvp (2026-05-11)
- [ ] Step 2 (gemini-client): WordService에서 enrichWords() 호출
- [ ] Step 3 (word-service): 프론트에서 보강 데이터 표시
```

미룬 작업을 처리하면 `- [x]`로 체크.

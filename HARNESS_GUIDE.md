# 하네스 프레임워크 가이드

## 1. 핵심 철학: "Agent = Model + Harness"

이 프로젝트는 모델의 추론 능력에 의존하는 것을 넘어, 모델을 엄격한 **상태 머신(State Machine)**과 **실행기(Executor)** 환경에 가두어 결과의 신뢰성을 보장하는 '하네스 공학'을 구현하는 것을 목표로 한다.

## 2. 설계 원칙

### PLAN/PRD/EXEC/VERIFY/FIX — step.md에 내재되는 원칙

이 5단계는 executor 코드가 강제하는 파이프라인이 아니라, **step.md를 작성할 때 내재시켜야 하는 설계 원칙**이다.

| 원칙 | step.md에서의 적용 |
| :--- | :--- |
| **PLAN** | "읽어야 할 파일" 섹션 — 작업 전 맥락 파악을 강제 |
| **PRD** | "작업" 섹션 — 요구사항을 구체적 시그니처 수준으로 상세화 |
| **EXEC** | "작업" 섹션 — 실제 구현 지시 |
| **VERIFY** | "Acceptance Criteria" 섹션 — `npm run build && npm test` 같은 실행 가능한 검증 커맨드 |
| **FIX** | executor 자가 교정 루프 — 실패 시 에러 메시지를 프롬프트에 피드백하여 최대 3회 재시도 |

### executor가 코드 레벨에서 강제하는 것

- **FSM 상태 전이**: pending → completed/error/blocked만 허용, 잘못된 전이 시 즉시 에러
- **Zod 스키마 검증**: index.json 구조를 런타임에서 검증
- **자가 교정 루프**: 실패 시 최대 3회 재시도, 이전 에러 메시지를 다음 시도에 피드백
- **2단계 커밋**: 코드 변경(feat)과 메타데이터(chore)를 분리 커밋
- **가드레일 주입**: AGENTS.md + docs/*.md를 매 step 프롬프트에 포함

### 사용자가 프로세스 레벨에서 강제하는 것

- `/prep` → `/harness` 순서: 문서 준비 없이 코드 구현 불가
- 리뷰 루프: `/prep`에서 사용자가 승인할 때까지 문서 수정 반복
- Step 설계 승인: `/harness` C단계에서 사용자가 step 구조를 승인해야 실행 가능

## 3. 실행기 아키텍처 (Executor Pattern)

모든 도구는 `BaseExecutor` 추상 클래스를 상속받아 구현하며, TypeScript를 통해 입출력 규격을 엄격히 제한한다.

* **Type Safety**: `Zod`를 사용하여 AI가 던지는 인자(Arguments)를 런타임에서 검증한다.
* **Error Feedback**: 에러 발생 시 단순 실패 처리가 아니라, AI가 이해하고 스스로 수정할 수 있는 정제된 에러 메시지를 반환한다.
* **Encapsulation**: 모델은 시스템 내부 로직을 알 필요 없이 오직 규격화된 도구만 호출한다.

## 4. 기술 스택

* **Runtime**: Node.js 또는 Bun (TypeScript 환경)
* **Pattern**: OOP 기반 추상 클래스 및 상태 머신(FSM)
* **Task**: "스스로 에러를 고치는 에이전트(Self-Healing Agent)" 루프 구현부터 시작

## 5. 워크플로우

실제 워크플로우는 스킬 파일을 참조하라:

- **문서 준비**: `.codex/skills/prep/SKILL.md` — `/prep` P1~P5
- **코드 구현**: `.codex/skills/harness/SKILL.md` — `/harness` 0~F

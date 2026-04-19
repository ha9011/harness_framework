import { readFileSync, existsSync, readdirSync } from "node:fs";
import { join, basename } from "node:path";
import type { PhaseIndex } from "./types.js";

/**
 * CLAUDE.md + docs/*.md 내용을 병합하여 가드레일 문자열을 반환한다.
 * Python execute.py의 _load_guardrails() 동일 로직.
 */
export function loadGuardrails(rootDir: string): string {
  const sections: string[] = [];

  const claudeMd = join(rootDir, "CLAUDE.md");
  if (existsSync(claudeMd)) {
    const content = readFileSync(claudeMd, "utf-8");
    sections.push(`## 프로젝트 규칙 (CLAUDE.md)\n\n${content}`);
  }

  const docsDir = join(rootDir, "docs");
  if (existsSync(docsDir)) {
    const files = readdirSync(docsDir)
      .filter((f) => f.endsWith(".md"))
      .sort();

    for (const file of files) {
      const content = readFileSync(join(docsDir, file), "utf-8");
      const stem = basename(file, ".md");
      sections.push(`## ${stem}\n\n${content}`);
    }
  }

  return sections.length > 0 ? sections.join("\n\n---\n\n") : "";
}

/**
 * 완료된 step들의 summary를 누적하여 컨텍스트 문자열을 생성한다.
 * Python execute.py의 _build_step_context() 동일 로직.
 */
export function buildStepContext(index: PhaseIndex): string {
  const lines: string[] = [];

  for (const s of index.steps) {
    if (s.status === "completed" && s.summary) {
      lines.push(`- Step ${s.step} (${s.name}): ${s.summary}`);
    }
  }

  if (lines.length === 0) return "";
  return `## 이전 Step 산출물\n\n${lines.join("\n")}\n\n`;
}

export interface PreambleOpts {
  project: string;
  phaseName: string;
  phaseDirName: string;
  guardrails: string;
  stepContext: string;
  maxRetries: number;
  prevError?: string;
}

/**
 * Claude 프롬프트의 프리앰블(규칙 + 컨텍스트 + 재시도 에러)을 조합한다.
 * Python execute.py의 _build_preamble() 동일 로직.
 */
export function buildPreamble(opts: PreambleOpts): string {
  const {
    project,
    phaseName,
    phaseDirName,
    guardrails,
    stepContext,
    maxRetries,
    prevError,
  } = opts;

  let retrySection = "";
  if (prevError) {
    retrySection =
      `\n## ⚠ 이전 시도 실패 — 아래 에러를 반드시 참고하여 수정하라\n\n` +
      `${prevError}\n\n---\n\n`;
  }

  return (
    `당신은 ${project} 프로젝트의 개발자입니다. 아래 step을 수행하세요.\n\n` +
    `${guardrails}\n\n---\n\n` +
    `${stepContext}${retrySection}` +
    `## 작업 규칙\n\n` +
    `1. 이전 step에서 작성된 코드를 확인하고 일관성을 유지하라.\n` +
    `2. 이 step에 명시된 작업만 수행하라. 추가 기능이나 파일을 만들지 마라.\n` +
    `3. 기존 테스트를 깨뜨리지 마라.\n` +
    `4. AC(Acceptance Criteria) 검증을 직접 실행하라.\n` +
    `5. /phases/${phaseDirName}/index.json의 해당 step status를 업데이트하라:\n` +
    `   - AC 통과 → "completed" + "summary" 필드에 이 step의 산출물을 한 줄로 요약\n` +
    `   - ${maxRetries}회 수정 시도 후에도 실패 → "error" + "error_message" 기록\n` +
    `   - 사용자 개입이 필요한 경우 (API 키, 인증, 수동 설정 등) → "blocked" + "blocked_reason" 기록 후 즉시 중단\n` +
    `6. 모든 변경사항을 저장하라. Git 커밋은 하지 마라. 하네스가 자동으로 처리한다.\n\n---\n\n`
  );
}

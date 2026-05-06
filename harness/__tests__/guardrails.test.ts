import { describe, it, expect } from "vitest";
import { join } from "node:path";
import { mkdtempSync, mkdirSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import {
  loadGuardrails,
  buildStepContext,
  buildPreamble,
} from "../src/guardrails.js";
import type { PhaseIndex } from "../src/types.js";

function makeTmpProject(): string {
  const dir = mkdtempSync(join(tmpdir(), "harness-guard-"));

  writeFileSync(join(dir, "AGENTS.md"), "# Rules\n- rule one\n- rule two");

  const docsDir = join(dir, "docs");
  mkdirSync(docsDir);
  writeFileSync(join(docsDir, "arch.md"), "# Architecture\nSome content");
  writeFileSync(join(docsDir, "guide.md"), "# Guide\nAnother doc");

  return dir;
}

// --- loadGuardrails ---

describe("loadGuardrails", () => {
  it("docs/*.md만 로드 (AGENTS.md는 Codex가 자동 주입)", () => {
    const dir = makeTmpProject();
    const result = loadGuardrails(dir);
    expect(result).not.toContain("rule one"); // AGENTS.md 내용 미포함
    expect(result).toContain("# Architecture");
    expect(result).toContain("# Guide");
  });

  it("섹션 사이에 구분선", () => {
    const dir = makeTmpProject();
    const result = loadGuardrails(dir);
    expect(result).toContain("---");
  });

  it("docs 파일 알파벳 순 정렬", () => {
    const dir = makeTmpProject();
    const result = loadGuardrails(dir);
    const archPos = result.indexOf("arch");
    const guidePos = result.indexOf("guide");
    expect(archPos).toBeLessThan(guidePos);
  });

  it("AGENTS.md 없을 때", () => {
    const dir = mkdtempSync(join(tmpdir(), "harness-guard-"));
    const docsDir = join(dir, "docs");
    mkdirSync(docsDir);
    writeFileSync(join(docsDir, "arch.md"), "# Architecture");

    const result = loadGuardrails(dir);
    expect(result).not.toContain("AGENTS.md");
    expect(result).toContain("Architecture");
  });

  it("docs 디렉토리 없을 때 빈 문자열", () => {
    const dir = mkdtempSync(join(tmpdir(), "harness-guard-"));

    const result = loadGuardrails(dir);
    expect(result).toBe("");
  });

  it("빈 프로젝트 (AGENTS.md도 docs도 없음)", () => {

    const dir = mkdtempSync(join(tmpdir(), "harness-guard-"));
    const result = loadGuardrails(dir);
    expect(result).toBe("");
  });
});

// --- buildStepContext ---

describe("buildStepContext", () => {
  const makeIndex = (steps: PhaseIndex["steps"]): PhaseIndex => ({
    project: "T",
    phase: "t",
    steps,
  });

  it("completed + summary 포함", () => {
    const index = makeIndex([
      { step: 0, name: "setup", status: "completed", summary: "프로젝트 초기화 완료" },
      { step: 1, name: "core", status: "completed", summary: "핵심 로직 구현" },
    ]);
    const result = buildStepContext(index);
    expect(result).toContain("Step 0 (setup): 프로젝트 초기화 완료");
    expect(result).toContain("Step 1 (core): 핵심 로직 구현");
  });

  it("pending 제외", () => {
    const index = makeIndex([
      { step: 0, name: "setup", status: "completed", summary: "완료" },
      { step: 1, name: "ui", status: "pending" },
    ]);
    const result = buildStepContext(index);
    expect(result).not.toContain("ui");
  });

  it("completed 없으면 빈 문자열", () => {
    const index = makeIndex([
      { step: 0, name: "a", status: "pending" },
    ]);
    expect(buildStepContext(index)).toBe("");
  });

  it("헤더 포함", () => {
    const index = makeIndex([
      { step: 0, name: "setup", status: "completed", summary: "완료" },
    ]);
    const result = buildStepContext(index);
    expect(result.startsWith("## 이전 Step 산출물")).toBe(true);
  });
});

// --- buildPreamble ---

describe("buildPreamble", () => {
  const defaultOpts = {
    project: "TestProject",
    phaseName: "mvp",
    phaseDirName: "0-mvp",
    guardrails: "",
    stepContext: "",
    maxRetries: 3,
  };

  it("프로젝트명 포함", () => {
    const result = buildPreamble(defaultOpts);
    expect(result).toContain("TestProject");
  });

  it("가드레일 포함", () => {
    const result = buildPreamble({ ...defaultOpts, guardrails: "GUARD_CONTENT" });
    expect(result).toContain("GUARD_CONTENT");
  });

  it("step 컨텍스트 포함", () => {
    const ctx = "## 이전 Step 산출물\n\n- Step 0: done";
    const result = buildPreamble({ ...defaultOpts, stepContext: ctx });
    expect(result).toContain("이전 Step 산출물");
  });

  it("커밋 금지 규칙 포함", () => {
    const result = buildPreamble(defaultOpts);
    expect(result).toContain("Git 커밋은 하지 마라");
  });

  it("작업 규칙 포함", () => {
    const result = buildPreamble(defaultOpts);
    expect(result).toContain("작업 규칙");
    expect(result).toContain("AC");
  });

  it("기본적으로 재시도 섹션 없음", () => {
    const result = buildPreamble(defaultOpts);
    expect(result).not.toContain("이전 시도 실패");
  });

  it("prevError 있으면 재시도 섹션 포함", () => {
    const result = buildPreamble({ ...defaultOpts, prevError: "타입 에러 발생" });
    expect(result).toContain("이전 시도 실패");
    expect(result).toContain("타입 에러 발생");
  });

  it("maxRetries 값 포함", () => {
    const result = buildPreamble(defaultOpts);
    expect(result).toContain("3");
  });

  it("index 경로 포함", () => {
    const result = buildPreamble(defaultOpts);
    expect(result).toContain("/phases/0-mvp/index.json");
  });
});

import { describe, it, expect } from "vitest";
import {
  StepStatusSchema,
  StepSchema,
  PhaseIndexSchema,
  TopIndexSchema,
  StepOutputSchema,
} from "../src/schemas.js";

// --- StepStatus ---

describe("StepStatusSchema", () => {
  it.each(["pending", "completed", "error", "blocked"])("유효: %s", (s) => {
    expect(StepStatusSchema.parse(s)).toBe(s);
  });

  it("무효 상태 거부", () => {
    expect(() => StepStatusSchema.parse("running")).toThrow();
  });
});

// --- StepSchema ---

describe("StepSchema", () => {
  it("pending step 파싱", () => {
    const data = { step: 0, name: "setup", status: "pending" };
    expect(StepSchema.parse(data)).toEqual(data);
  });

  it("pending step + started_at", () => {
    const data = {
      step: 0,
      name: "setup",
      status: "pending",
      started_at: "2025-01-01T00:00:00+0900",
    };
    expect(StepSchema.parse(data)).toEqual(data);
  });

  it("completed step (summary 필수)", () => {
    const data = {
      step: 1,
      name: "core",
      status: "completed",
      summary: "핵심 로직 구현",
    };
    expect(StepSchema.parse(data)).toEqual(data);
  });

  it("completed step — summary 누락 시 실패", () => {
    const data = { step: 1, name: "core", status: "completed" };
    expect(() => StepSchema.parse(data)).toThrow();
  });

  it("completed step — summary 빈 문자열 시 실패", () => {
    const data = { step: 1, name: "core", status: "completed", summary: "" };
    expect(() => StepSchema.parse(data)).toThrow();
  });

  it("error step (error_message 필수)", () => {
    const data = {
      step: 2,
      name: "ui",
      status: "error",
      error_message: "타입 에러",
    };
    expect(StepSchema.parse(data)).toEqual(data);
  });

  it("error step — error_message 누락 시 실패", () => {
    const data = { step: 2, name: "ui", status: "error" };
    expect(() => StepSchema.parse(data)).toThrow();
  });

  it("blocked step (blocked_reason 필수)", () => {
    const data = {
      step: 3,
      name: "deploy",
      status: "blocked",
      blocked_reason: "API 키 필요",
    };
    expect(StepSchema.parse(data)).toEqual(data);
  });

  it("blocked step — blocked_reason 누락 시 실패", () => {
    const data = { step: 3, name: "deploy", status: "blocked" };
    expect(() => StepSchema.parse(data)).toThrow();
  });

  it("step 번호 음수 시 실패", () => {
    const data = { step: -1, name: "bad", status: "pending" };
    expect(() => StepSchema.parse(data)).toThrow();
  });

  it("name 빈 문자열 시 실패", () => {
    const data = { step: 0, name: "", status: "pending" };
    expect(() => StepSchema.parse(data)).toThrow();
  });
});

// --- PhaseIndexSchema ---

describe("PhaseIndexSchema", () => {
  it("유효한 phase index 파싱", () => {
    const data = {
      project: "TestProject",
      phase: "mvp",
      steps: [
        { step: 0, name: "setup", status: "completed", summary: "완료" },
        { step: 1, name: "core", status: "pending" },
      ],
    };
    expect(PhaseIndexSchema.parse(data)).toEqual(data);
  });

  it("project 누락 시 실패", () => {
    const data = {
      phase: "mvp",
      steps: [],
    };
    expect(() => PhaseIndexSchema.parse(data)).toThrow();
  });

  it("빈 steps 배열 허용", () => {
    const data = { project: "P", phase: "p", steps: [] };
    expect(PhaseIndexSchema.parse(data)).toEqual(data);
  });

  it("created_at, completed_at 선택 필드", () => {
    const data = {
      project: "P",
      phase: "p",
      steps: [],
      created_at: "2025-01-01T00:00:00+0900",
      completed_at: "2025-01-02T00:00:00+0900",
    };
    expect(PhaseIndexSchema.parse(data)).toEqual(data);
  });
});

// --- TopIndexSchema ---

describe("TopIndexSchema", () => {
  it("유효한 top index 파싱", () => {
    const data = {
      phases: [
        { dir: "0-mvp", status: "pending" },
        { dir: "1-polish", status: "completed", completed_at: "2025-01-01" },
      ],
    };
    expect(TopIndexSchema.parse(data)).toEqual(data);
  });

  it("빈 phases 배열 허용", () => {
    const data = { phases: [] };
    expect(TopIndexSchema.parse(data)).toEqual(data);
  });

  it("dir 빈 문자열 시 실패", () => {
    const data = { phases: [{ dir: "", status: "pending" }] };
    expect(() => TopIndexSchema.parse(data)).toThrow();
  });
});

// --- StepOutputSchema ---

describe("StepOutputSchema", () => {
  it("유효한 출력 파싱", () => {
    const data = {
      step: 0,
      name: "setup",
      exitCode: 0,
      stdout: '{"result": "ok"}',
      stderr: "",
    };
    expect(StepOutputSchema.parse(data)).toEqual(data);
  });

  it("exitCode 비정상 허용", () => {
    const data = {
      step: 0,
      name: "setup",
      exitCode: 1,
      stdout: "",
      stderr: "error occurred",
    };
    expect(StepOutputSchema.parse(data)).toEqual(data);
  });
});

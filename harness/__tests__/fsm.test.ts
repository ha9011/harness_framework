import { describe, it, expect } from "vitest";
import {
  StepFSM,
  InvalidTransitionError,
} from "../src/fsm.js";

// --- StepFSM ---

describe("StepFSM", () => {
  // 유효 전이
  describe("유효 전이", () => {
    it("pending → completed (summary 있음)", () => {
      const fsm = new StepFSM("pending");
      expect(fsm.transition("complete", { summary: "완료" })).toBe("completed");
      expect(fsm.current).toBe("completed");
    });

    it("pending → error (error_message 있음)", () => {
      const fsm = new StepFSM("pending");
      expect(fsm.transition("fail", { error_message: "에러" })).toBe("error");
      expect(fsm.current).toBe("error");
    });

    it("pending → blocked (blocked_reason 있음)", () => {
      const fsm = new StepFSM("pending");
      expect(fsm.transition("block", { blocked_reason: "API 키" })).toBe("blocked");
      expect(fsm.current).toBe("blocked");
    });

    it("error → pending (retry)", () => {
      const fsm = new StepFSM("error");
      expect(fsm.transition("retry")).toBe("pending");
      expect(fsm.current).toBe("pending");
    });

    it("blocked → pending (retry)", () => {
      const fsm = new StepFSM("blocked");
      expect(fsm.transition("retry")).toBe("pending");
      expect(fsm.current).toBe("pending");
    });
  });

  // Guard 검증
  describe("guard 조건", () => {
    it("complete 시 summary 없으면 에러", () => {
      const fsm = new StepFSM("pending");
      expect(() => fsm.transition("complete")).toThrow("summary");
    });

    it("complete 시 빈 summary면 에러", () => {
      const fsm = new StepFSM("pending");
      expect(() => fsm.transition("complete", { summary: "" })).toThrow("summary");
    });

    it("fail 시 error_message 없으면 에러", () => {
      const fsm = new StepFSM("pending");
      expect(() => fsm.transition("fail")).toThrow("error_message");
    });

    it("block 시 blocked_reason 없으면 에러", () => {
      const fsm = new StepFSM("pending");
      expect(() => fsm.transition("block")).toThrow("blocked_reason");
    });
  });

  // 무효 전이
  describe("무효 전이", () => {
    it("completed → complete 불가", () => {
      const fsm = new StepFSM("completed");
      expect(() => fsm.transition("complete", { summary: "x" })).toThrow(InvalidTransitionError);
    });

    it("completed → retry 불가", () => {
      const fsm = new StepFSM("completed");
      expect(() => fsm.transition("retry")).toThrow(InvalidTransitionError);
    });

    it("pending → retry 불가", () => {
      const fsm = new StepFSM("pending");
      expect(() => fsm.transition("retry")).toThrow(InvalidTransitionError);
    });

    it("error → complete 불가", () => {
      const fsm = new StepFSM("error");
      expect(() => fsm.transition("complete", { summary: "x" })).toThrow(InvalidTransitionError);
    });

    it("error → block 불가", () => {
      const fsm = new StepFSM("error");
      expect(() => fsm.transition("block", { blocked_reason: "x" })).toThrow(InvalidTransitionError);
    });

    it("blocked → complete 불가", () => {
      const fsm = new StepFSM("blocked");
      expect(() => fsm.transition("complete", { summary: "x" })).toThrow(InvalidTransitionError);
    });

    it("blocked → fail 불가", () => {
      const fsm = new StepFSM("blocked");
      expect(() => fsm.transition("fail", { error_message: "x" })).toThrow(InvalidTransitionError);
    });
  });

  // 연쇄 전이
  describe("연쇄 전이", () => {
    it("pending → error → retry → completed", () => {
      const fsm = new StepFSM("pending");
      fsm.transition("fail", { error_message: "실패" });
      expect(fsm.current).toBe("error");

      fsm.transition("retry");
      expect(fsm.current).toBe("pending");

      fsm.transition("complete", { summary: "성공" });
      expect(fsm.current).toBe("completed");
    });

    it("pending → blocked → retry → completed", () => {
      const fsm = new StepFSM("pending");
      fsm.transition("block", { blocked_reason: "키 필요" });
      fsm.transition("retry");
      fsm.transition("complete", { summary: "해결" });
      expect(fsm.current).toBe("completed");
    });
  });
});


import { describe, it, expect, vi, beforeEach } from "vitest";
import { ClaudeClient } from "../src/claude.js";
import * as fs from "node:fs";
import { join } from "node:path";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { EventEmitter } from "node:events";
import type { ChildProcess } from "node:child_process";

// spawn을 mock
vi.mock("node:child_process", () => ({
  spawn: vi.fn(),
}));

import { spawn } from "node:child_process";
const mockSpawn = vi.mocked(spawn);

function tmpFile(name: string): string {
  const dir = mkdtempSync(join(tmpdir(), "harness-claude-"));
  return join(dir, name);
}

/** mock stdin 스트림을 생성한다 */
function createMockStdin() {
  return { write: vi.fn(), end: vi.fn() };
}

/** mock ChildProcess를 생성한다 */
function createMockChild(exitCode: number, stdout: string, stderr: string) {
  const child = new EventEmitter() as ChildProcess & {
    stdin: { write: ReturnType<typeof vi.fn>; end: ReturnType<typeof vi.fn> };
    stdout: EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
    stderr: EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
    kill: ReturnType<typeof vi.fn>;
  };
  const stdoutEmitter = new EventEmitter() as EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
  const stderrEmitter = new EventEmitter() as EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
  stdoutEmitter.setEncoding = vi.fn();
  stderrEmitter.setEncoding = vi.fn();
  child.stdin = createMockStdin();
  child.stdout = stdoutEmitter;
  child.stderr = stderrEmitter;
  child.kill = vi.fn();

  // 비동기로 데이터를 emit
  setTimeout(() => {
    if (stdout) child.stdout.emit("data", stdout);
    if (stderr) child.stderr.emit("data", stderr);
    child.emit("close", exitCode);
  }, 0);

  return child;
}

describe("ClaudeClient", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("invoke — 일반 모드", () => {
    it("claude CLI를 올바른 인자로 호출", async () => {
      mockSpawn.mockReturnValue(createMockChild(0, '{"result": "ok"}', "") as unknown as ChildProcess);
      const outputPath = tmpFile("output.json");
      const client = new ClaudeClient({ cwd: "/test" });

      const result = await client.invoke({
        step: 0,
        name: "setup",
        prompt: "PREAMBLE\nDo something",
        outputPath,
      });

      expect(mockSpawn).toHaveBeenCalledWith(
        "claude",
        ["-p", "--dangerously-skip-permissions", "--output-format", "json"],
        expect.objectContaining({ cwd: "/test" }),
      );
      expect(result.exitCode).toBe(0);
      expect(result.step).toBe(0);
      expect(result.name).toBe("setup");
    });

    it("output JSON 파일 저장", async () => {
      mockSpawn.mockReturnValue(createMockChild(0, '{"ok": true}', "") as unknown as ChildProcess);
      const outputPath = tmpFile("output.json");
      const client = new ClaudeClient({ cwd: "/test" });

      await client.invoke({ step: 2, name: "ui", prompt: "test", outputPath });

      const saved = JSON.parse(fs.readFileSync(outputPath, "utf-8"));
      expect(saved.step).toBe(2);
      expect(saved.name).toBe("ui");
      expect(saved.exitCode).toBe(0);
    });

    it("Claude 비정상 종료 시 에러 정보 반환", async () => {
      mockSpawn.mockReturnValue(createMockChild(1, "", "something went wrong") as unknown as ChildProcess);
      const outputPath = tmpFile("output.json");
      const client = new ClaudeClient({ cwd: "/test" });

      const result = await client.invoke({
        step: 0,
        name: "setup",
        prompt: "test",
        outputPath,
      });

      expect(result.exitCode).toBe(1);
      expect(result.stderr).toContain("something went wrong");
    });
  });

  describe("invoke — 에러 처리", () => {
    it("spawn error 발생 시 정상 resolve", async () => {
      const child = new EventEmitter() as ChildProcess & {
        stdin: { write: ReturnType<typeof vi.fn>; end: ReturnType<typeof vi.fn> };
        stdout: EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
        stderr: EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
        kill: ReturnType<typeof vi.fn>;
      };
      const stdoutEmitter = new EventEmitter() as EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
      const stderrEmitter = new EventEmitter() as EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
      stdoutEmitter.setEncoding = vi.fn();
      stderrEmitter.setEncoding = vi.fn();
      child.stdin = createMockStdin();
      child.stdout = stdoutEmitter;
      child.stderr = stderrEmitter;
      child.kill = vi.fn();

      setTimeout(() => {
        child.emit("error", new Error("spawn ENOENT"));
      }, 0);

      mockSpawn.mockReturnValue(child as unknown as ChildProcess);
      const outputPath = tmpFile("output.json");
      const client = new ClaudeClient({ cwd: "/test" });

      const result = await client.invoke({
        step: 0,
        name: "setup",
        prompt: "test",
        outputPath,
      });

      expect(result.exitCode).toBe(1);
      expect(result.stderr).toContain("spawn ENOENT");
    });

    it("timeout 시 SIGTERM 호출", async () => {
      const child = new EventEmitter() as ChildProcess & {
        stdin: { write: ReturnType<typeof vi.fn>; end: ReturnType<typeof vi.fn> };
        stdout: EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
        stderr: EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
        kill: ReturnType<typeof vi.fn>;
      };
      const stdoutEmitter = new EventEmitter() as EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
      const stderrEmitter = new EventEmitter() as EventEmitter & { setEncoding: ReturnType<typeof vi.fn> };
      stdoutEmitter.setEncoding = vi.fn();
      stderrEmitter.setEncoding = vi.fn();
      child.stdin = createMockStdin();
      child.stdout = stdoutEmitter;
      child.stderr = stderrEmitter;
      child.kill = vi.fn().mockImplementation(() => {
        // kill 후 close 이벤트 발생
        setTimeout(() => child.emit("close", null), 0);
      });

      mockSpawn.mockReturnValue(child as unknown as ChildProcess);
      const outputPath = tmpFile("output.json");
      // 타임아웃을 50ms로 설정
      const client = new ClaudeClient({ cwd: "/test", timeoutMs: 50 });

      const result = await client.invoke({
        step: 0,
        name: "setup",
        prompt: "test",
        outputPath,
      });

      expect(child.kill).toHaveBeenCalledWith("SIGTERM");
      expect(result.exitCode).toBe(1);
    });
  });

  describe("invoke — dry-run 모드", () => {
    it("subprocess를 호출하지 않음", async () => {
      const outputPath = tmpFile("output.json");
      const client = new ClaudeClient({ cwd: "/test", dryRun: true });

      const result = await client.invoke({
        step: 0,
        name: "setup",
        prompt: "test prompt",
        outputPath,
      });

      expect(mockSpawn).not.toHaveBeenCalled();
      expect(result.exitCode).toBe(0);
      expect(result.stdout).toContain("dry-run");
    });

    it("output 파일은 저장됨", async () => {
      const outputPath = tmpFile("output.json");
      const client = new ClaudeClient({ cwd: "/test", dryRun: true });

      await client.invoke({ step: 1, name: "core", prompt: "x", outputPath });

      const saved = JSON.parse(fs.readFileSync(outputPath, "utf-8"));
      expect(saved.step).toBe(1);
      expect(saved.name).toBe("core");
    });
  });
});

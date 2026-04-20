/**
 * @file claude.ts
 * @description
 *   Claude AI를 호출하는 "전화기" 역할.
 *   프롬프트(질문)를 Claude CLI에 보내고, 응답을 JSON으로 받아온다.
 *   하네스가 AI와 소통하는 유일한 창구이다.
 *
 * @see executor.ts - 이 파일을 사용해 각 스텝마다 Claude를 호출한다
 */
import { spawn } from "node:child_process";
import { writeFileSync } from "node:fs";
import type { StepOutput } from "./types.js";

export interface ClaudeClientOpts {
  cwd: string;
  dryRun?: boolean;
  timeoutMs?: number;
}

export interface ClaudeInvokeOpts {
  step: number;
  name: string;
  prompt: string;
  outputPath: string; // stepN-output.json 경로
}

export class ClaudeClient {
  private readonly cwd: string;
  private readonly dryRun: boolean;
  private readonly timeoutMs: number;

  constructor(opts: ClaudeClientOpts) {
    this.cwd = opts.cwd;
    this.dryRun = opts.dryRun ?? false;
    this.timeoutMs = opts.timeoutMs ?? 1_800_000; // 30분
  }

  /**
   * Claude CLI를 비동기로 호출하고 결과를 반환한다.
   * dry-run 모드에서는 프롬프트만 출력하고 mock 결과를 반환한다.
   */
  async invoke(opts: ClaudeInvokeOpts): Promise<StepOutput> {
    const { step, name, prompt, outputPath } = opts;

    if (this.dryRun) {
      console.log(`\n${"=".repeat(60)}`);
      console.log(`  [DRY-RUN] Step ${step}: ${name}`);
      console.log(`${"=".repeat(60)}`);
      console.log(prompt);
      console.log(`${"=".repeat(60)}\n`);

      const output: StepOutput = {
        step,
        name,
        exitCode: 0,
        stdout: "[dry-run] 실행하지 않음",
        stderr: "",
      };
      writeFileSync(outputPath, JSON.stringify(output, null, 2), "utf-8");
      return output;
    }

    const { exitCode, stdout, stderr } = await this._spawn(prompt);

    if (exitCode !== 0) {
      console.log(`\n  WARN: Claude가 비정상 종료됨 (code ${exitCode})`);
      if (stderr) {
        console.log(`  stderr: ${stderr.slice(0, 500)}`);
      }
    }

    const output: StepOutput = { step, name, exitCode, stdout, stderr };
    writeFileSync(outputPath, JSON.stringify(output, null, 2), "utf-8");
    return output;
  }

  /** spawn으로 Claude CLI를 비동기 실행한다. 프롬프트는 stdin으로 전달한다. */
  private _spawn(prompt: string): Promise<{ exitCode: number; stdout: string; stderr: string }> {
    return new Promise((resolve) => {
      const child = spawn(
        "claude",
        ["-p", "--dangerously-skip-permissions", "--output-format", "json"],
        { cwd: this.cwd, stdio: ["pipe", "pipe", "pipe"] },
      );

      // 프롬프트를 stdin으로 전달 (ARG_MAX 제한 회피)
      child.stdin.write(prompt);
      child.stdin.end();

      const stdoutChunks: string[] = [];
      const stderrChunks: string[] = [];

      child.stdout.setEncoding("utf-8");
      child.stderr.setEncoding("utf-8");
      child.stdout.on("data", (chunk: string) => stdoutChunks.push(chunk));
      child.stderr.on("data", (chunk: string) => stderrChunks.push(chunk));

      const SIGKILL_DELAY_MS = 10_000;
      let killTimer: ReturnType<typeof setTimeout> | undefined;
      const timer = setTimeout(() => {
        child.kill("SIGTERM");
        // SIGTERM 무시 시 SIGKILL로 강제 종료
        killTimer = setTimeout(() => {
          if (!child.killed) child.kill("SIGKILL");
        }, SIGKILL_DELAY_MS);
      }, this.timeoutMs);

      const cleanup = () => {
        clearTimeout(timer);
        if (killTimer) clearTimeout(killTimer);
      };

      child.on("error", (err) => {
        cleanup();
        resolve({
          exitCode: 1,
          stdout: "",
          stderr: err.message,
        });
      });

      child.on("close", (code) => {
        cleanup();
        resolve({
          exitCode: code ?? 1,
          stdout: stdoutChunks.join(""),
          stderr: stderrChunks.join(""),
        });
      });
    });
  }
}

/**
 * @file codex.ts
 * @description
 *   Codex CLI를 호출하는 "전화기" 역할.
 *   프롬프트(질문)를 Codex CLI에 보내고, 응답을 받아온다.
 *   하네스가 AI와 소통하는 유일한 창구이다.
 *
 * @see executor.ts - 이 파일을 사용해 각 스텝마다 Codex를 호출한다
 */
import { spawn } from "node:child_process";
import { writeFileSync } from "node:fs";
import type { StepOutput } from "./types.js";

export interface CodexClientOpts {
  cwd: string;
  dryRun?: boolean;
  timeoutMs?: number;
  model?: string;
}

export interface CodexInvokeOpts {
  step: number;
  name: string;
  prompt: string;
  outputPath: string; // stepN-output.json 경로
}

export class CodexClient {
  private readonly cwd: string;
  private readonly dryRun: boolean;
  private readonly timeoutMs: number;
  private readonly model: string;

  constructor(opts: CodexClientOpts) {
    this.cwd = opts.cwd;
    this.dryRun = opts.dryRun ?? false;
    this.timeoutMs = opts.timeoutMs ?? 1_800_000; // 30분
    this.model = opts.model ?? "o3";
  }

  /**
   * Codex CLI를 비동기로 호출하고 결과를 반환한다.
   * dry-run 모드에서는 프롬프트만 출력하고 mock 결과를 반환한다.
   */
  async invoke(opts: CodexInvokeOpts): Promise<StepOutput> {
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
      console.log(`\n  WARN: Codex가 비정상 종료됨 (code ${exitCode})`);
      if (stderr) {
        console.log(`  stderr: ${stderr.slice(0, 500)}`);
      }
    }

    const output: StepOutput = { step, name, exitCode, stdout, stderr };
    writeFileSync(outputPath, JSON.stringify(output, null, 2), "utf-8");
    return output;
  }

  /**
   * spawn으로 Codex CLI를 비동기 실행한다.
   * 프롬프트는 stdin pipe로 전달한다 (ARG_MAX 회피 + Shell Injection 방지).
   *
   * Codex exec 명령:
   *   codex exec --json -c approval_policy=never -c sandbox_mode=danger-full-access -c model=<model> -
   *   (마지막 "-"는 stdin에서 프롬프트를 읽으라는 sentinel)
   *
   * 출력: JSONL (줄 단위 JSON 이벤트 스트림)
   * → 최종 AgentMessage에서 응답 텍스트를 추출한다.
   */
  private _spawn(prompt: string): Promise<{ exitCode: number; stdout: string; stderr: string }> {
    return new Promise((resolve) => {
      const args = [
        "exec",
        "--json",
        "-c", "approval_policy=never",
        "-c", "sandbox_mode=danger-full-access",
        "-c", `model=${this.model}`,
        "-",  // stdin에서 프롬프트 읽기
      ];

      const child = spawn("codex", args, {
        cwd: this.cwd,
        stdio: ["pipe", "pipe", "pipe"],
      });

      // 프롬프트를 stdin pipe로 전달 (shell 해석 없이 안전하게 전달)
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
        const rawStdout = stdoutChunks.join("");
        // JSONL 스트림에서 최종 응답 텍스트를 추출한다
        const finalMessage = this._extractFinalMessage(rawStdout);
        resolve({
          exitCode: code ?? 1,
          stdout: finalMessage ?? rawStdout,
          stderr: stderrChunks.join(""),
        });
      });
    });
  }

  /**
   * Codex exec --json 출력(JSONL 스트림)에서 최종 AI 응답 텍스트를 추출한다.
   *
   * JSONL 이벤트 형태 예시:
   *   {"type":"ItemCompleted","item":{"type":"AgentMessage","content":[{"type":"output_text","text":"..."}]}}
   *   {"type":"TaskCompleted","last_agent_message":"..."}
   *
   * 최종 메시지를 찾지 못하면 null을 반환한다.
   */
  private _extractFinalMessage(rawStdout: string): string | null {
    const lines = rawStdout.split("\n").filter(Boolean);
    let lastMessage: string | null = null;

    for (const line of lines) {
      try {
        const event = JSON.parse(line);

        // 패턴 1: TaskCompleted 이벤트의 last_agent_message
        if (event.type === "TaskCompleted" && event.last_agent_message) {
          return event.last_agent_message;
        }

        // 패턴 2: ItemCompleted → AgentMessage → content[].text
        if (event.type === "ItemCompleted" && event.item?.type === "AgentMessage") {
          const contents = event.item.content;
          if (Array.isArray(contents)) {
            const texts = contents
              .filter((c: { type: string }) => c.type === "output_text")
              .map((c: { text: string }) => c.text);
            if (texts.length > 0) {
              lastMessage = texts.join("\n");
            }
          }
        }

        // 패턴 3: 단순 message 필드 (Codex 버전에 따라 다를 수 있음)
        if (event.message && typeof event.message === "string") {
          lastMessage = event.message;
        }
      } catch {
        // JSON 파싱 실패 — 비JSON 라인 무시
      }
    }

    return lastMessage;
  }
}

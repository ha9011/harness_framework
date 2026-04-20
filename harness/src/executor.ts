/**
 * @file executor.ts
 * @description
 *   전체 작업을 순서대로 진행하는 "지휘자" (오케스트레이터).
 *   스텝을 하나씩 꺼내서 Claude에게 시키고, 결과를 저장하고,
 *   실패하면 최대 3번까지 재시도한다.
 *   이 파일이 하네스의 핵심 두뇌 역할을 한다.
 *
 * @see cli.ts       - 사용자 명령을 받아 이 파일의 StepExecutor를 실행한다
 * @see claude.ts    - 각 스텝에서 Claude AI를 호출할 때 사용
 * @see git.ts       - 스텝 완료 후 자동 커밋할 때 사용
 * @see guardrails.ts - Claude에게 보낼 프롬프트를 조립할 때 사용
 * @see fsm.ts       - 스텝 상태 전이가 올바른지 검증할 때 사용
 */
import { existsSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { GitClient } from "./git.js";
import { ClaudeClient } from "./claude.js";
import { readJson, writeJson } from "./json-io.js";
import { kstNow } from "./timestamp.js";
import { withProgress } from "./progress.js";
import { loadGuardrails, buildStepContext, buildPreamble } from "./guardrails.js";
import { PhaseIndexSchema, MutablePhaseIndexSchema, TopIndexSchema } from "./schemas.js";
import { StepFSM } from "./fsm.js";
import type { PhaseIndex, MutablePhaseIndex, Step, TopIndex } from "./types.js";

export interface ExecutorOpts {
  phaseDirName: string;
  autoPush?: boolean;
  dryRun?: boolean;
  rootDir?: string;
}

export interface ExecutorDeps {
  git: GitClient;
  claude: ClaudeClient;
}

const MAX_RETRIES = 3;

export class StepExecutor {
  private readonly root: string;
  private readonly phasesDir: string;
  private readonly phaseDir: string;
  private readonly phaseDirName: string;
  private readonly topIndexFile: string;
  private readonly indexFile: string;
  private readonly autoPush: boolean;
  private readonly git: GitClient;
  private readonly claude: ClaudeClient;

  private project: string;
  private phaseName: string;
  private total: number;

  constructor(opts: ExecutorOpts, deps?: Partial<ExecutorDeps>) {
    this.root = opts.rootDir ?? resolve(join(import.meta.dirname, "..", ".."));
    this.phasesDir = join(this.root, "phases");
    this.phaseDir = join(this.phasesDir, opts.phaseDirName);
    this.phaseDirName = opts.phaseDirName;
    this.topIndexFile = join(this.phasesDir, "index.json");
    this.indexFile = join(this.phaseDir, "index.json");
    this.autoPush = opts.autoPush ?? false;

    this.git = deps?.git ?? new GitClient(this.root);
    this.claude = deps?.claude ?? new ClaudeClient({
      cwd: this.root,
      dryRun: opts.dryRun ?? false,
    });

    if (!existsSync(this.phaseDir)) {
      console.log(`ERROR: ${this.phaseDir} not found`);
      process.exit(1);
    }

    if (!existsSync(this.indexFile)) {
      console.log(`ERROR: ${this.indexFile} not found`);
      process.exit(1);
    }

    const index = readJson(this.indexFile, PhaseIndexSchema);
    this.project = index.project;
    this.phaseName = index.phase;
    this.total = index.steps.length;
  }

  async run(): Promise<void> {
    this.printHeader();
    this.checkBlockers();
    await this.git.checkoutBranch(this.phaseName);
    const guardrails = loadGuardrails(this.root);
    this.ensureCreatedAt();
    await this.executeAllSteps(guardrails);
    await this.finalize();
  }

  // --- 헤더 ---

  private printHeader(): void {
    console.log(`\n${"=".repeat(60)}`);
    console.log(`  Harness Step Executor`);
    console.log(`  Phase: ${this.phaseName} | Steps: ${this.total}`);
    if (this.autoPush) console.log(`  Auto-push: enabled`);
    console.log(`${"=".repeat(60)}`);
  }

  // --- 진행 상태 테이블 ---

  private printProgressTable(): void {
    const index = this.readMutable();
    const steps = index.steps;

    // 컬럼 너비 계산
    const nameWidth = Math.max(4, ...steps.map((s) => s.name.length));
    const statusWidth = 9; // "🔄 진행 중" 등 (이모지 폭 보정)
    const summaryWidth = 50;

    const pad = (str: string, width: number) => {
      // 이모지/한글 폭 보정: 이모지 1개=2칸, 한글 1자=2칸
      const displayWidth = [...str].reduce((w, ch) => {
        const code = ch.codePointAt(0) ?? 0;
        if (code > 0x1f000) return w + 2; // 이모지
        if ((code >= 0xac00 && code <= 0xd7a3) || (code >= 0x3000 && code <= 0x9fff)) return w + 2; // 한글/CJK
        return w + 1;
      }, 0);
      return str + " ".repeat(Math.max(0, width - displayWidth));
    };

    const getStatus = (s: { status: string; started_at?: string }): string => {
      if (s.status === "completed") return "✅ 완료";
      if (s.status === "error") return "❌ 실패";
      if (s.status === "blocked") return "⏸ 차단";
      if (s.started_at) return "🔄 진행 중";
      return "⏳ 대기";
    };

    const getSummary = (s: { summary?: string }): string => {
      const summary = s.summary ?? "";
      if (summary.length <= summaryWidth) return summary;
      return summary.slice(0, summaryWidth - 1) + "…";
    };

    const hr = (left: string, mid: string, right: string, fill = "─") =>
      `${left}${fill.repeat(6)}${mid}${fill.repeat(nameWidth + 2)}${mid}${fill.repeat(statusWidth + 2)}${mid}${fill.repeat(summaryWidth + 2)}${right}`;

    const row = (step: string, name: string, status: string, summary: string) =>
      `│ ${pad(step, 4)} │ ${pad(name, nameWidth)} │ ${pad(status, statusWidth)} │ ${pad(summary, summaryWidth)} │`;

    console.log("");
    console.log(hr("┌", "┬", "┐"));
    console.log(row("Step", "Name", "상태", "Summary"));
    console.log(hr("├", "┼", "┤"));
    for (const s of steps) {
      console.log(row(
        String(s.step),
        s.name,
        getStatus(s),
        getSummary(s),
      ));
    }
    console.log(hr("└", "┴", "┘"));
    console.log("");
  }

  // --- blocker 체크 ---

  checkBlockers(): void {
    const index = readJson(this.indexFile, PhaseIndexSchema);
    const steps = [...index.steps].reverse();

    for (const s of steps) {
      if (s.status === "error") {
        console.log(`\n  ✗ Step ${s.step} (${s.name}) failed.`);
        console.log(`  Error: ${s.error_message}`);
        console.log(`  Fix and reset status to 'pending' to retry.`);
        process.exit(1);
      }
      if (s.status === "blocked") {
        console.log(`\n  ⏸ Step ${s.step} (${s.name}) blocked.`);
        console.log(`  Reason: ${s.blocked_reason}`);
        console.log(`  Resolve and reset status to 'pending' to retry.`);
        process.exit(2);
      }
      if (s.status !== "pending") break;
    }
  }

  // --- 타임스탬프 초기화 ---

  private ensureCreatedAt(): void {
    const index = this.readMutable();
    if (!index.created_at) {
      index.created_at = kstNow();
      writeJson(this.indexFile, index);
    }
  }

  // --- 느슨한 스키마로 읽기 (쓰기용) ---

  private readMutable(): MutablePhaseIndex {
    return readJson(this.indexFile, MutablePhaseIndexSchema);
  }

  // --- index.json 강제 복원 (JSON이 완전히 깨진 경우 최후의 방어선) ---

  private restoreStepToPending(stepNum: number): void {
    const raw = readFileSync(this.indexFile, "utf-8");
    try {
      const data = JSON.parse(raw);
      const s = data.steps?.find((s: Record<string, unknown>) => s.step === stepNum);
      if (s) {
        s.status = "pending";
        delete s.error_message;
        delete s.summary;
        delete s.blocked_reason;
      }
      writeJson(this.indexFile, data);
    } catch {
      // JSON 자체가 깨진 경우 — 생성자에서 읽은 원본으로 복원
      const index = { project: this.project, phase: this.phaseName, steps: [] as Record<string, unknown>[] };
      for (let i = 0; i < this.total; i++) {
        index.steps.push({ step: i, name: `step-${i}`, status: i === stepNum ? "pending" : "pending" });
      }
      writeJson(this.indexFile, index);
      console.log(`  WARN: index.json이 심각하게 손상되어 최소 구조로 복원했습니다.`);
    }
  }

  private restoreStepToError(stepNum: number, errMsg: string, ts: string): void {
    try {
      const raw = readFileSync(this.indexFile, "utf-8");
      const data = JSON.parse(raw);
      const s = data.steps?.find((s: Record<string, unknown>) => s.step === stepNum);
      if (s) {
        s.status = "error";
        s.error_message = errMsg;
        s.failed_at = ts;
      }
      writeJson(this.indexFile, data);
    } catch {
      // 복원 불가 — 에러 메시지만 로깅
      console.log(`  WARN: index.json 복원 실패. 에러: ${errMsg}`);
    }
  }

  // --- 단일 step 실행 ---

  private async executeSingleStep(step: Step, guardrails: string): Promise<boolean> {
    const { step: stepNum, name: stepName } = step;
    const index = readJson(this.indexFile, PhaseIndexSchema);
    const doneCount = index.steps.filter((s) => s.status === "completed").length;
    let prevError: string | undefined;

    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      const currentIndex = readJson(this.indexFile, PhaseIndexSchema);
      const stepContext = buildStepContext(currentIndex);
      const preamble = buildPreamble({
        project: this.project,
        phaseName: this.phaseName,
        phaseDirName: this.phaseDirName,
        guardrails,
        stepContext,
        maxRetries: MAX_RETRIES,
        prevError,
      });

      let tag = `Step ${stepNum}/${this.total - 1} (${doneCount} done): ${stepName}`;
      if (attempt > 1) tag += ` [retry ${attempt}/${MAX_RETRIES}]`;

      // step 파일 읽기
      const stepFile = join(this.phaseDir, `step${stepNum}.md`);
      if (!existsSync(stepFile)) {
        console.log(`  ERROR: ${stepFile} not found`);
        process.exit(1);
      }
      const stepContent = readFileSync(stepFile, "utf-8");
      const prompt = preamble + stepContent;
      const outputPath = join(this.phaseDir, `step${stepNum}-output.json`);

      // Claude 호출
      const { elapsed } = await withProgress(tag, async () => {
        await this.claude.invoke({ step: stepNum, name: stepName, prompt, outputPath });
      });
      const elapsedSec = Math.floor(elapsed);

      // 결과 확인 — 파싱 에러 시 재시도 루프로 넘긴다
      let status: string = "pending";
      let currentStep: Step | undefined;
      let parseError: string | undefined;

      try {
        const updatedIndex = readJson(this.indexFile, PhaseIndexSchema);
        currentStep = updatedIndex.steps.find((s) => s.step === stepNum);
        status = currentStep?.status ?? "pending";
      } catch (err) {
        // Claude가 index.json을 잘못 수정한 경우 (JSON 깨짐, 필수 필드 누락 등)
        parseError = err instanceof Error ? err.message : String(err);
        status = "error";
        console.log(`  WARN: index.json 파싱 실패 — ${parseError.slice(0, 200)}`);
      }

      const ts = kstNow();

      if (status === "completed" && currentStep?.status === "completed") {
        // FSM으로 전이 검증
        const fsm = new StepFSM("pending");
        fsm.transition("complete", { summary: currentStep.summary });

        // 타임스탬프 기록
        const mutable = this.readMutable();
        const mStep = mutable.steps.find((s) => s.step === stepNum);
        if (mStep) mStep.completed_at = ts;
        writeJson(this.indexFile, mutable);

        await this.git.commitStep({
          phaseDir: this.phaseDirName,
          phaseName: this.phaseName,
          stepNum,
          stepName,
        });
        console.log(`  ✓ Step ${stepNum}: ${stepName} [${elapsedSec}s]`);
        this.printProgressTable();
        return true;
      }

      if (status === "blocked" && currentStep?.status === "blocked") {
        // FSM으로 전이 검증
        const fsm = new StepFSM("pending");
        fsm.transition("block", { blocked_reason: currentStep.blocked_reason });

        const mutable = this.readMutable();
        const mStep = mutable.steps.find((s) => s.step === stepNum);
        if (mStep) mStep.blocked_at = ts;
        writeJson(this.indexFile, mutable);

        console.log(`  ⏸ Step ${stepNum}: ${stepName} blocked [${elapsedSec}s]`);
        console.log(`    Reason: ${currentStep.blocked_reason}`);
        this.printProgressTable();
        this.updateTopIndex("blocked");
        process.exit(2);
      }

      // error, 미업데이트, 또는 파싱 실패
      const errMsg = parseError
        ? `index.json 파싱 실패: ${parseError}`
        : currentStep?.status === "error"
          ? (currentStep as { error_message: string }).error_message
          : "Step did not update status";

      if (attempt < MAX_RETRIES) {
        // 재시도를 위해 index.json을 pending으로 복원
        try {
          const mutable = this.readMutable();
          const mStep = mutable.steps.find((s) => s.step === stepNum);
          if (mStep) {
            mStep.status = "pending";
            delete mStep.error_message;
          }
          writeJson(this.indexFile, mutable);
        } catch {
          // MutablePhaseIndexSchema로도 파싱 불가 시 원본 복원
          this.restoreStepToPending(stepNum);
        }
        prevError = errMsg;
        console.log(`  ↻ Step ${stepNum}: retry ${attempt}/${MAX_RETRIES} — ${errMsg}`);
      } else {
        // 최종 실패 기록
        try {
          const mutable = this.readMutable();
          const mStep = mutable.steps.find((s) => s.step === stepNum);
          if (mStep) {
            mStep.status = "error";
            mStep.error_message = `[${MAX_RETRIES}회 시도 후 실패] ${errMsg}`;
            mStep.failed_at = ts;
          }
          writeJson(this.indexFile, mutable);
        } catch {
          // 최종 실패 시에도 파싱 불가면 강제 복원 후 에러 기록
          this.restoreStepToError(stepNum, `[${MAX_RETRIES}회 시도 후 실패] ${errMsg}`, ts);
        }

        await this.git.commitStep({
          phaseDir: this.phaseDirName,
          phaseName: this.phaseName,
          stepNum,
          stepName,
        });
        console.log(`  ✗ Step ${stepNum}: ${stepName} failed after ${MAX_RETRIES} attempts [${elapsedSec}s]`);
        console.log(`    Error: ${errMsg}`);
        this.printProgressTable();
        this.updateTopIndex("error");
        process.exit(1);
      }
    }

    return false;
  }

  // --- 전체 step 실행 ---

  private async executeAllSteps(guardrails: string): Promise<void> {
    while (true) {
      const index = readJson(this.indexFile, PhaseIndexSchema);
      const pending = index.steps.find((s) => s.status === "pending");
      if (!pending) {
        console.log("\n  All steps completed!");
        return;
      }

      // started_at 기록
      const mutable = this.readMutable();
      const mStep = mutable.steps.find((s) => s.step === pending.step);
      if (mStep && !mStep.started_at) {
        mStep.started_at = kstNow();
        writeJson(this.indexFile, mutable);
      }

      this.printProgressTable();
      await this.executeSingleStep(pending, guardrails);
    }
  }

  // --- top index 업데이트 ---

  private updateTopIndex(status: string): void {
    if (!existsSync(this.topIndexFile)) return;

    const topIndex = readJson(this.topIndexFile, TopIndexSchema);
    const ts = kstNow();

    const updated: TopIndex = {
      phases: topIndex.phases.map((phase) => {
        if (phase.dir !== this.phaseDirName) return phase;

        const tsKey: Record<string, string> = {
          completed: "completed_at",
          error: "failed_at",
          blocked: "blocked_at",
        };
        return {
          ...phase,
          status: status as TopIndex["phases"][number]["status"],
          ...(tsKey[status] ? { [tsKey[status]]: ts } : {}),
        };
      }),
    };

    writeJson(this.topIndexFile, updated);
  }

  // --- 완료 처리 ---

  private async finalize(): Promise<void> {
    // phase 완료 기록
    const mutable = this.readMutable();
    mutable.completed_at = kstNow();
    writeJson(this.indexFile, mutable);
    this.updateTopIndex("completed");

    // 최종 커밋
    await this.git.run("add", "-A");
    if ((await this.git.run("diff", "--cached", "--quiet")).returncode !== 0) {
      const msg = `chore(${this.phaseName}): mark phase completed`;
      const r = await this.git.run("commit", "-m", msg);
      if (r.returncode === 0) console.log(`  ✓ ${msg}`);
    }

    // auto push
    if (this.autoPush) {
      await this.git.push(`feat-${this.phaseName}`);
    }

    console.log(`\n${"=".repeat(60)}`);
    console.log(`  Phase '${this.phaseName}' completed!`);
    console.log(`${"=".repeat(60)}`);
  }
}

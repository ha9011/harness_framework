import { describe, it, expect, vi, beforeEach } from "vitest";
import { join } from "node:path";
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { StepExecutor } from "../src/executor.js";
import type { GitClient } from "../src/git.js";
import type { ClaudeClient } from "../src/claude.js";

// --- 헬퍼 ---

function makeTmpProject(steps: Array<Record<string, unknown>>): string {
  const root = mkdtempSync(join(tmpdir(), "harness-exec-"));

  // CLAUDE.md
  writeFileSync(join(root, "CLAUDE.md"), "# Rules\n- rule one");

  // docs/
  mkdirSync(join(root, "docs"));
  writeFileSync(join(root, "docs", "arch.md"), "# Arch");

  // phases/
  mkdirSync(join(root, "phases"));
  mkdirSync(join(root, "phases", "0-mvp"));

  const index = {
    project: "TestProject",
    phase: "mvp",
    steps,
  };
  writeFileSync(
    join(root, "phases", "0-mvp", "index.json"),
    JSON.stringify(index, null, 2),
  );

  // step 파일 생성
  for (const s of steps) {
    writeFileSync(
      join(root, "phases", "0-mvp", `step${s.step}.md`),
      `# Step ${s.step}: ${s.name}\n\n작업을 수행하세요.`,
    );
  }

  return root;
}

function makeTopIndex(root: string): void {
  const top = {
    phases: [
      { dir: "0-mvp", status: "pending" },
      { dir: "1-polish", status: "pending" },
    ],
  };
  writeFileSync(join(root, "phases", "index.json"), JSON.stringify(top, null, 2));
}

function mockGit(): GitClient {
  return {
    run: vi.fn().mockResolvedValue({ returncode: 0, stdout: "", stderr: "" }),
    checkoutBranch: vi.fn().mockResolvedValue(undefined),
    commitStep: vi.fn().mockResolvedValue(undefined),
    push: vi.fn().mockResolvedValue(undefined),
  } as unknown as GitClient;
}

function mockClaude(onInvoke?: (opts: { step: number }) => void): ClaudeClient {
  return {
    invoke: vi.fn().mockImplementation(async (opts: { step: number; outputPath: string }) => {
      onInvoke?.(opts);
      return { step: opts.step, name: "", exitCode: 0, stdout: "", stderr: "" };
    }),
  } as unknown as ClaudeClient;
}

// --- checkBlockers ---

describe("StepExecutor — checkBlockers", () => {
  it("error step 있으면 exit(1)", () => {
    const root = makeTmpProject([
      { step: 0, name: "ok", status: "completed", summary: "완료" },
      { step: 1, name: "bad", status: "error", error_message: "실패함" },
    ]);

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", rootDir: root },
      { git: mockGit(), claude: mockClaude() },
    );

    expect(() => executor.checkBlockers()).toThrow("exit");
    expect(mockExit).toHaveBeenCalledWith(1);
    mockExit.mockRestore();
  });

  it("blocked step 있으면 exit(2)", () => {
    const root = makeTmpProject([
      { step: 0, name: "ok", status: "completed", summary: "완료" },
      { step: 1, name: "stuck", status: "blocked", blocked_reason: "API 키 필요" },
    ]);

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", rootDir: root },
      { git: mockGit(), claude: mockClaude() },
    );

    expect(() => executor.checkBlockers()).toThrow("exit");
    expect(mockExit).toHaveBeenCalledWith(2);
    mockExit.mockRestore();
  });

  it("pending만 있으면 정상 통과", () => {
    const root = makeTmpProject([
      { step: 0, name: "setup", status: "pending" },
    ]);

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", rootDir: root },
      { git: mockGit(), claude: mockClaude() },
    );

    expect(() => executor.checkBlockers()).not.toThrow();
  });
});

// --- 생성자 검증 ---

describe("StepExecutor — 생성자", () => {
  it("존재하지 않는 phase 디렉토리면 exit(1)", () => {
    const root = mkdtempSync(join(tmpdir(), "harness-exec-"));
    mkdirSync(join(root, "phases"));

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    expect(() =>
      new StepExecutor(
        { phaseDirName: "nonexistent", rootDir: root },
        { git: mockGit(), claude: mockClaude() },
      ),
    ).toThrow("exit");
    expect(mockExit).toHaveBeenCalledWith(1);
    mockExit.mockRestore();
  });

  it("index.json 없으면 exit(1)", () => {
    const root = mkdtempSync(join(tmpdir(), "harness-exec-"));
    mkdirSync(join(root, "phases", "empty"), { recursive: true });

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    expect(() =>
      new StepExecutor(
        { phaseDirName: "empty", rootDir: root },
        { git: mockGit(), claude: mockClaude() },
      ),
    ).toThrow("exit");
    expect(mockExit).toHaveBeenCalledWith(1);
    mockExit.mockRestore();
  });
});

// --- run 통합 ---

describe("StepExecutor — run (통합)", () => {
  it("pending step을 실행하고 completed 처리", async () => {
    const root = makeTmpProject([
      { step: 0, name: "setup", status: "pending" },
    ]);
    makeTopIndex(root);

    const git = mockGit();
    // Claude가 호출되면 index.json을 completed로 업데이트
    const claude = mockClaude(() => {
      const indexPath = join(root, "phases", "0-mvp", "index.json");
      const idx = JSON.parse(readFileSync(indexPath, "utf-8"));
      idx.steps[0].status = "completed";
      idx.steps[0].summary = "프로젝트 초기화 완료";
      writeFileSync(indexPath, JSON.stringify(idx, null, 2));
    });

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", rootDir: root },
      { git, claude },
    );

    await executor.run();

    // Claude 호출 확인
    expect(claude.invoke).toHaveBeenCalledTimes(1);
    // git commit 호출 확인
    expect(git.commitStep).toHaveBeenCalledTimes(1);
    // checkoutBranch 호출 확인
    expect(git.checkoutBranch).toHaveBeenCalledWith("mvp");

    mockExit.mockRestore();
  });

  it("이미 완료된 step은 건너뜀", async () => {
    const root = makeTmpProject([
      { step: 0, name: "setup", status: "completed", summary: "완료" },
      { step: 1, name: "core", status: "pending" },
    ]);
    makeTopIndex(root);

    const git = mockGit();
    const claude = mockClaude(() => {
      const indexPath = join(root, "phases", "0-mvp", "index.json");
      const idx = JSON.parse(readFileSync(indexPath, "utf-8"));
      idx.steps[1].status = "completed";
      idx.steps[1].summary = "핵심 로직 구현";
      writeFileSync(indexPath, JSON.stringify(idx, null, 2));
    });

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", rootDir: root },
      { git, claude },
    );

    await executor.run();

    // step 1만 실행
    expect(claude.invoke).toHaveBeenCalledTimes(1);
    const invokeCall = (claude.invoke as ReturnType<typeof vi.fn>).mock.calls[0][0];
    expect(invokeCall.step).toBe(1);

    mockExit.mockRestore();
  });
});

// --- 재시도 exhaustion ---

describe("StepExecutor — 재시도 exhaustion", () => {
  it("3회 재시도 후 error 상태로 exit(1)", async () => {
    const root = makeTmpProject([
      { step: 0, name: "flaky", status: "pending" },
    ]);
    makeTopIndex(root);

    let invokeCount = 0;
    const git = mockGit();
    const claude = mockClaude(() => {
      invokeCount++;
      const indexPath = join(root, "phases", "0-mvp", "index.json");
      const idx = JSON.parse(readFileSync(indexPath, "utf-8"));
      idx.steps[0].status = "error";
      idx.steps[0].error_message = `실패 ${invokeCount}`;
      writeFileSync(indexPath, JSON.stringify(idx, null, 2));
    });

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", rootDir: root },
      { git, claude },
    );

    await expect(executor.run()).rejects.toThrow("exit");

    expect(invokeCount).toBe(3);
    expect(mockExit).toHaveBeenCalledWith(1);

    const finalIndex = JSON.parse(
      readFileSync(join(root, "phases", "0-mvp", "index.json"), "utf-8"),
    );
    expect(finalIndex.steps[0].status).toBe("error");
    expect(finalIndex.steps[0].error_message).toContain("3회 시도 후 실패");

    mockExit.mockRestore();
  });
});

// --- dry-run ---

describe("StepExecutor — dry-run", () => {
  it("브랜치 checkout, created_at 기록, Claude 호출 없이 프롬프트만 출력", async () => {
    const root = makeTmpProject([
      { step: 0, name: "setup", status: "pending" },
    ]);

    const git = mockGit();
    const claude = mockClaude();

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", dryRun: true, rootDir: root },
      { git, claude },
    );

    await executor.run();

    // Claude를 호출하지 않음
    expect(claude.invoke).not.toHaveBeenCalled();
    // 브랜치 checkout 하지 않음
    expect(git.checkoutBranch).not.toHaveBeenCalled();
    // created_at 기록하지 않음
    const idx = JSON.parse(readFileSync(join(root, "phases", "0-mvp", "index.json"), "utf-8"));
    expect(idx.created_at).toBeUndefined();

    mockExit.mockRestore();
  });

  it("error step이 있으면 dry-run도 exit(1)", async () => {
    const root = makeTmpProject([
      { step: 0, name: "ok", status: "completed", summary: "완료" },
      { step: 1, name: "bad", status: "error", error_message: "실패함" },
    ]);

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", dryRun: true, rootDir: root },
      { git: mockGit(), claude: mockClaude() },
    );

    await expect(executor.run()).rejects.toThrow("exit");
    expect(mockExit).toHaveBeenCalledWith(1);

    mockExit.mockRestore();
  });

  it("step 파일 누락 시 exit(1)", async () => {
    const root = makeTmpProject([
      { step: 0, name: "setup", status: "pending" },
    ]);
    // step 파일 삭제
    const stepFile = join(root, "phases", "0-mvp", "step0.md");
    require("node:fs").unlinkSync(stepFile);

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", dryRun: true, rootDir: root },
      { git: mockGit(), claude: mockClaude() },
    );

    await expect(executor.run()).rejects.toThrow("exit");
    expect(mockExit).toHaveBeenCalledWith(1);

    mockExit.mockRestore();
  });
});

// --- archivePlan ---

describe("StepExecutor — archivePlan", () => {
  it("완료 시 PLAN.md가 docs/archive/로 이동되고 커밋에 포함됨", async () => {
    const root = makeTmpProject([
      { step: 0, name: "setup", status: "pending" },
    ]);
    makeTopIndex(root);

    // PLAN.md 생성
    writeFileSync(join(root, "docs", "PLAN.md"), "# Plan\n기능 구현 계획");

    const git = mockGit();
    const claude = mockClaude(() => {
      const indexPath = join(root, "phases", "0-mvp", "index.json");
      const idx = JSON.parse(readFileSync(indexPath, "utf-8"));
      idx.steps[0].status = "completed";
      idx.steps[0].summary = "완료";
      writeFileSync(indexPath, JSON.stringify(idx, null, 2));
    });

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", rootDir: root },
      { git, claude },
    );

    await executor.run();

    // PLAN.md가 사라짐
    expect(existsSync(join(root, "docs", "PLAN.md"))).toBe(false);
    // archive 디렉토리에 이동됨
    expect(existsSync(join(root, "docs", "archive"))).toBe(true);
    const archiveFiles = require("node:fs").readdirSync(join(root, "docs", "archive"));
    expect(archiveFiles.length).toBe(1);
    expect(archiveFiles[0]).toMatch(/^PLAN_\d{8}_\d{6}_0-mvp\.md$/);

    // git add가 archive 파일을 stage했는지 확인
    const addCalls = (git.run as ReturnType<typeof vi.fn>).mock.calls;
    const addedPaths = addCalls
      .filter((c: string[]) => c[0] === "add")
      .map((c: string[]) => c[1]);
    // archive 파일이 stage됨 (docs/archive/PLAN_...md 경로)
    expect(addedPaths.some((p: string) => p.includes("docs/archive/PLAN_"))).toBe(true);

    mockExit.mockRestore();
  });

  it("PLAN.md가 없으면 아카이브 건너뜀", async () => {
    const root = makeTmpProject([
      { step: 0, name: "setup", status: "pending" },
    ]);
    makeTopIndex(root);
    // PLAN.md 없음

    const git = mockGit();
    const claude = mockClaude(() => {
      const indexPath = join(root, "phases", "0-mvp", "index.json");
      const idx = JSON.parse(readFileSync(indexPath, "utf-8"));
      idx.steps[0].status = "completed";
      idx.steps[0].summary = "완료";
      writeFileSync(indexPath, JSON.stringify(idx, null, 2));
    });

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", rootDir: root },
      { git, claude },
    );

    await executor.run();

    // archive 디렉토리가 생성되지 않음
    expect(existsSync(join(root, "docs", "archive"))).toBe(false);

    // git add에 archive 관련 호출 없음
    const addCalls = (git.run as ReturnType<typeof vi.fn>).mock.calls;
    const addedPaths = addCalls
      .filter((c: string[]) => c[0] === "add")
      .map((c: string[]) => c[1]);
    expect(addedPaths.some((p: string) => typeof p === "string" && p.includes("archive"))).toBe(false);

    mockExit.mockRestore();
  });
});

// --- top index 업데이트 ---

describe("StepExecutor — top index 업데이트", () => {
  it("완료 시 top index의 해당 phase가 completed로 변경", async () => {
    const root = makeTmpProject([
      { step: 0, name: "setup", status: "pending" },
    ]);
    makeTopIndex(root);

    const git = mockGit();
    const claude = mockClaude(() => {
      const indexPath = join(root, "phases", "0-mvp", "index.json");
      const idx = JSON.parse(readFileSync(indexPath, "utf-8"));
      idx.steps[0].status = "completed";
      idx.steps[0].summary = "완료";
      writeFileSync(indexPath, JSON.stringify(idx, null, 2));
    });

    const mockExit = vi.spyOn(process, "exit").mockImplementation(() => {
      throw new Error("exit");
    });

    const executor = new StepExecutor(
      { phaseDirName: "0-mvp", rootDir: root },
      { git, claude },
    );

    await executor.run();

    const topIndex = JSON.parse(
      readFileSync(join(root, "phases", "index.json"), "utf-8"),
    );
    const mvp = topIndex.phases.find((p: Record<string, unknown>) => p.dir === "0-mvp");
    expect(mvp.status).toBe("completed");
    expect(mvp.completed_at).toBeDefined();

    // 다른 phase는 변경 없음
    const polish = topIndex.phases.find((p: Record<string, unknown>) => p.dir === "1-polish");
    expect(polish.status).toBe("pending");

    mockExit.mockRestore();
  });
});

import { describe, it, expect, vi, beforeEach } from "vitest";
import { GitClient } from "../src/git.js";
import * as childProcess from "node:child_process";

vi.mock("node:child_process", () => ({
  execFile: vi.fn(),
}));

// promisify가 mock된 execFile을 감싸도록 util도 mock
vi.mock("node:util", () => ({
  promisify: (fn: Function) => {
    return (...args: unknown[]) => {
      return new Promise((resolve, reject) => {
        (fn as Function)(...args, (err: Error | null, stdout?: string, stderr?: string) => {
          if (err) reject(err);
          else resolve({ stdout: stdout ?? "", stderr: stderr ?? "" });
        });
      });
    };
  },
}));

const mockExecFile = vi.mocked(childProcess.execFile);

/** 성공 콜백 mock */
function mockSuccess(stdout = "") {
  return (_cmd: string, _args: string[], _opts: unknown, cb: Function) => {
    cb(null, stdout, "");
  };
}

/** 실패 콜백 mock */
function mockFailure(code = 1, stderr = "") {
  return (_cmd: string, _args: string[], _opts: unknown, cb: Function) => {
    const err = Object.assign(new Error("fail"), { code, stdout: "", stderr });
    cb(err);
  };
}

describe("GitClient", () => {
  let git: GitClient;

  beforeEach(() => {
    git = new GitClient("/test/repo");
    vi.clearAllMocks();
  });

  describe("run", () => {
    it("성공 시 returncode 0", async () => {
      mockExecFile.mockImplementation(mockSuccess("output\n") as any);
      const result = await git.run("status");
      expect(result.returncode).toBe(0);
      expect(result.stdout).toBe("output\n");
    });

    it("실패 시 returncode와 stderr 반환", async () => {
      mockExecFile.mockImplementation(mockFailure(128, "fatal: not a git repo") as any);
      const result = await git.run("status");
      expect(result.returncode).toBe(128);
      expect(result.stderr).toContain("not a git repo");
    });
  });

  describe("checkoutBranch", () => {
    it("이미 해당 브랜치면 아무것도 하지 않음", async () => {
      mockExecFile.mockImplementation(mockSuccess("feat-mvp\n") as any);
      await git.checkoutBranch("mvp");
      // branch --show-current 1회만
      expect(mockExecFile).toHaveBeenCalledTimes(1);
    });

    it("브랜치 존재 시 checkout", async () => {
      let callIdx = 0;
      mockExecFile.mockImplementation((_cmd: any, _args: any, _opts: any, cb: any) => {
        callIdx++;
        if (callIdx === 1) cb(null, "main\n", "");   // branch --show-current
        else if (callIdx === 2) cb(null, "", "");     // rev-parse --verify (exists)
        else cb(null, "", "");                         // checkout
      });
      await git.checkoutBranch("mvp");
      expect(mockExecFile).toHaveBeenCalledTimes(3);
    });

    it("브랜치 미존재 시 생성", async () => {
      let callIdx = 0;
      mockExecFile.mockImplementation((_cmd: any, _args: any, _opts: any, cb: any) => {
        callIdx++;
        if (callIdx === 1) cb(null, "main\n", "");
        else if (callIdx === 2) cb(Object.assign(new Error(), { code: 1, stdout: "", stderr: "" }));
        else cb(null, "", "");  // checkout -b
      });
      await git.checkoutBranch("mvp");
      expect(mockExecFile).toHaveBeenCalledTimes(3);
    });

    it("빈 저장소 감지 시 exit(1)", async () => {
      let callIdx = 0;
      mockExecFile.mockImplementation((_cmd: any, _args: any, _opts: any, cb: any) => {
        callIdx++;
        if (callIdx === 1) cb(null, "", "");  // branch --show-current → 빈 문자열
        else if (callIdx === 2) cb(Object.assign(new Error(), { code: 128, stdout: "", stderr: "HEAD not found" }));
        else cb(null, "", "");
      });

      const mockExit = vi.spyOn(process, "exit").mockImplementation(() => { throw new Error("exit"); });
      await expect(git.checkoutBranch("mvp")).rejects.toThrow("exit");
      expect(mockExit).toHaveBeenCalledWith(1);
      mockExit.mockRestore();
    });
  });

  describe("commitStep", () => {
    it("2단계 커밋 실행", async () => {
      const calls: string[][] = [];
      mockExecFile.mockImplementation((_cmd: any, args: any, _opts: any, cb: any) => {
        calls.push(args);
        if (args[0] === "diff" && args[1] === "--cached") {
          cb(Object.assign(new Error(), { code: 1, stdout: "", stderr: "" })); // 변경 있음
        } else {
          cb(null, "", "");
        }
      });

      await git.commitStep({
        phaseDir: "0-mvp",
        phaseName: "mvp",
        stepNum: 1,
        stepName: "core",
      });

      const commitCalls = calls.filter((a) => a[0] === "commit");
      expect(commitCalls).toHaveLength(2);
      expect(commitCalls[0][2]).toContain("feat(mvp):");
      expect(commitCalls[1][2]).toContain("chore(mvp):");
    });
  });
});

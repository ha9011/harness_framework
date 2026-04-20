/**
 * @file git.ts
 * @description
 *   Git 명령어를 대신 실행해주는 "도우미".
 *   브랜치 생성/전환, 코드 커밋, 원격 저장소 푸시 등을 담당한다.
 *   스텝이 완료될 때마다 자동으로 커밋을 만들어준다.
 *
 * @see executor.ts - 스텝 완료 시 이 파일의 GitClient로 커밋/푸시한다
 */
import { execFile } from "node:child_process";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

export interface GitResult {
  returncode: number;
  stdout: string;
  stderr: string;
}

export interface CommitStepOpts {
  phaseDir: string;      // 예: "0-mvp"
  phaseName: string;     // 예: "mvp"
  stepNum: number;
  stepName: string;
}

export class GitClient {
  constructor(private readonly cwd: string) {}

  /** git 명령을 비동기로 실행하고 결과를 반환한다. 실패해도 throw하지 않는다. */
  async run(...args: string[]): Promise<GitResult> {
    try {
      const { stdout, stderr } = await execFileAsync("git", args, {
        cwd: this.cwd,
        encoding: "utf-8",
      });
      return { returncode: 0, stdout, stderr };
    } catch (err: unknown) {
      const e = err as { code?: number; stdout?: string; stderr?: string };
      return {
        returncode: e.code ?? 1,
        stdout: e.stdout ?? "",
        stderr: e.stderr ?? "",
      };
    }
  }

  /**
   * feat-{phaseName} 브랜치로 checkout한다.
   * 이미 해당 브랜치면 아무것도 하지 않는다.
   * 브랜치가 없으면 새로 생성한다.
   * 빈 저장소(최초 커밋 없음)일 경우 안내 메시지를 출력한다.
   */
  async checkoutBranch(phaseName: string): Promise<void> {
    const branch = `feat-${phaseName}`;

    // git branch --show-current는 빈 저장소에서도 빈 문자열을 반환 (크래시 없음)
    const current = await this.run("branch", "--show-current");
    if (current.returncode !== 0) {
      console.log(`  ERROR: git을 사용할 수 없거나 git repo가 아닙니다.`);
      console.log(`  ${current.stderr.trim()}`);
      process.exit(1);
    }

    const currentBranch = current.stdout.trim();

    // 빈 저장소 (HEAD가 없음) 감지
    if (!currentBranch) {
      const headCheck = await this.run("rev-parse", "HEAD");
      if (headCheck.returncode !== 0) {
        console.log(`  ERROR: 최초 커밋이 없는 빈 저장소입니다.`);
        console.log(`  Hint: 최소 한 번의 커밋을 한 후 다시 시도하세요.`);
        console.log(`        git add . && git commit -m "initial commit"`);
        process.exit(1);
      }
    }

    if (currentBranch === branch) return;

    const exists = await this.run("rev-parse", "--verify", branch);
    const checkout =
      exists.returncode === 0
        ? await this.run("checkout", branch)
        : await this.run("checkout", "-b", branch);

    if (checkout.returncode !== 0) {
      console.log(`  ERROR: 브랜치 '${branch}' checkout 실패.`);
      console.log(`  ${checkout.stderr.trim()}`);
      console.log(`  Hint: 변경사항을 stash하거나 commit한 후 다시 시도하세요.`);
      process.exit(1);
    }

    console.log(`  Branch: ${branch}`);
  }

  /**
   * 2단계 커밋: 코드 변경(feat) → 메타데이터(chore) 분리.
   * output/index 파일은 첫 번째 커밋에서 제외한다.
   */
  async commitStep(opts: CommitStepOpts): Promise<void> {
    const { phaseDir, phaseName, stepNum, stepName } = opts;
    const outputRel = `phases/${phaseDir}/step${stepNum}-output.json`;
    const indexRel = `phases/${phaseDir}/index.json`;

    // 1단계: 코드 변경 커밋 (output, index 제외)
    // phases/ 와 docs/ 이외의 프로젝트 파일만 스테이징
    await this.run("add", "-A", "--", ".", `:!${outputRel}`, `:!${indexRel}`);

    if ((await this.run("diff", "--cached", "--quiet")).returncode !== 0) {
      const msg = `feat(${phaseName}): step ${stepNum} — ${stepName}`;
      const r = await this.run("commit", "-m", msg);
      if (r.returncode === 0) {
        console.log(`  Commit: ${msg}`);
      } else {
        console.log(`  WARN: 코드 커밋 실패: ${r.stderr.trim()}`);
      }
    }

    // 2단계: 메타데이터 커밋 (output, index만 스테이징)
    await this.run("add", outputRel, indexRel);
    if ((await this.run("diff", "--cached", "--quiet")).returncode !== 0) {
      const msg = `chore(${phaseName}): step ${stepNum} output`;
      const r = await this.run("commit", "-m", msg);
      if (r.returncode !== 0) {
        console.log(`  WARN: housekeeping 커밋 실패: ${r.stderr.trim()}`);
      }
    }
  }

  /** 브랜치를 원격에 push한다. */
  async push(branch: string): Promise<void> {
    const r = await this.run("push", "-u", "origin", branch);
    if (r.returncode !== 0) {
      console.log(`\n  ERROR: git push 실패: ${r.stderr.trim()}`);
      process.exit(1);
    }
    console.log(`  ✓ Pushed to origin/${branch}`);
  }
}

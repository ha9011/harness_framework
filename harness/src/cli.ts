/**
 * @file cli.ts
 * @description
 *   터미널에서 명령어를 입력하면 가장 먼저 실행되는 "현관문".
 *   사용자가 입력한 옵션(phase 경로, --push, --dry-run 등)을 해석해서
 *   executor에게 "이 설정으로 실행해줘"라고 전달한다.
 *
 * @see executor.ts - CLI가 파싱한 옵션을 받아 실제 작업을 수행한다
 */
import { Command } from "commander";
import { StepExecutor } from "./executor.js";

const program = new Command();

program
  .name("harness")
  .description("Harness Step Executor — phase 내 step을 순차 실행하고 자가 교정한다.")
  .argument("<phase-dir>", "Phase 디렉토리명 (예: 0-mvp)")
  .option("--push", "완료 후 브랜치를 원격에 push", false)
  .option("--dry-run", "Codex를 호출하지 않고 프롬프트만 출력", false)
  .option("--model <model>", "Codex 모델 지정 (기본: o3)")
  .action(async (phaseDir: string, options: { push: boolean; dryRun: boolean; model?: string }) => {
    const executor = new StepExecutor({
      phaseDirName: phaseDir,
      autoPush: options.push,
      dryRun: options.dryRun,
      model: options.model,
    });

    await executor.run();
  });

program.parse();

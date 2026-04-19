import { Command } from "commander";
import { StepExecutor } from "./executor.js";

const program = new Command();

program
  .name("harness")
  .description("Harness Step Executor — phase 내 step을 순차 실행하고 자가 교정한다.")
  .argument("<phase-dir>", "Phase 디렉토리명 (예: 0-mvp)")
  .option("--push", "완료 후 브랜치를 원격에 push", false)
  .option("--dry-run", "Claude를 호출하지 않고 프롬프트만 출력", false)
  .action(async (phaseDir: string, options: { push: boolean; dryRun: boolean }) => {
    const executor = new StepExecutor({
      phaseDirName: phaseDir,
      autoPush: options.push,
      dryRun: options.dryRun,
    });

    await executor.run();
  });

program.parse();

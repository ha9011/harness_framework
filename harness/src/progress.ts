/**
 * @file progress.ts
 * @description
 *   작업이 진행 중일 때 터미널에 스피너를 보여주는 "로딩 표시기".
 *   AI 호출처럼 시간이 오래 걸리는 작업 중에
 *   ◐◓◑◒ 같은 애니메이션과 경과 시간을 표시해서
 *   "아직 작업 중이에요"라고 사용자에게 알려준다.
 *
 * @see executor.ts - Codex 호출 시 이 스피너를 감싸서 사용한다
 */
const FRAMES = "◐◓◑◒";
const FRAME_INTERVAL_MS = 120;

export interface ProgressResult<T> {
  result: T;
  elapsed: number; // 초 단위
}

/**
 * 비동기 함수를 실행하면서 터미널 진행 표시기를 보여준다.
 * 완료 후 결과와 경과 시간을 반환한다.
 */
export async function withProgress<T>(
  label: string,
  fn: () => Promise<T>,
): Promise<ProgressResult<T>> {
  const t0 = performance.now();
  let idx = 0;
  let lastLineLen = 0;

  const timer = setInterval(() => {
    const sec = Math.floor((performance.now() - t0) / 1000);
    const frame = FRAMES[idx % FRAMES.length];
    const line = `${frame} ${label} [${sec}s]`;
    lastLineLen = Math.max(lastLineLen, line.length);
    process.stdout.write(`\r${line}`);
    idx++;
  }, FRAME_INTERVAL_MS);

  try {
    const result = await fn();
    const elapsed = (performance.now() - t0) / 1000;
    return { result, elapsed };
  } finally {
    clearInterval(timer);
    // 라인 지우기 — 마지막으로 쓴 줄 길이 + 여유분으로 확실히 덮기
    const clearLen = Math.max(lastLineLen + 10, 120);
    process.stdout.write("\r" + " ".repeat(clearLen) + "\r");
  }
}

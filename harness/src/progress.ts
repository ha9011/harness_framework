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

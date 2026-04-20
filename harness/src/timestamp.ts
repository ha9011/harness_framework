/**
 * @file timestamp.ts
 * @description
 *   한국 시간(KST, UTC+9) 기준 타임스탬프를 만들어주는 "시계".
 *   스텝이 시작/완료/실패한 시각을 기록할 때 사용된다.
 *   예: "2025-04-19T17:30:00+0900"
 *
 * @see executor.ts - 스텝 상태 변경 시 이 파일로 시각을 기록한다
 */
const KST_OFFSET = 9 * 60; // UTC+9 (분 단위)

/**
 * 현재 시각을 KST ISO 8601 문자열로 반환한다.
 * 예: "2025-04-19T17:30:00+0900"
 */
export function kstNow(): string {
  const now = new Date();
  const kst = new Date(now.getTime() + (KST_OFFSET + now.getTimezoneOffset()) * 60_000);

  const pad = (n: number) => String(n).padStart(2, "0");

  const y = kst.getFullYear();
  const mo = pad(kst.getMonth() + 1);
  const d = pad(kst.getDate());
  const h = pad(kst.getHours());
  const mi = pad(kst.getMinutes());
  const s = pad(kst.getSeconds());

  return `${y}-${mo}-${d}T${h}:${mi}:${s}+0900`;
}

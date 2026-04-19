import { describe, it, expect } from "vitest";
import { kstNow } from "../src/timestamp.js";

describe("kstNow", () => {
  it("+0900 타임존 포함", () => {
    const result = kstNow();
    expect(result).toContain("+0900");
  });

  it("ISO 8601 형식", () => {
    const result = kstNow();
    expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+0900$/);
  });

  it("현재 시각과 1초 이내 차이", () => {
    const before = Date.now();
    const result = kstNow();
    const after = Date.now();

    // KST 문자열을 파싱하여 UTC 밀리초로 변환
    const parsed = new Date(result).getTime();
    expect(parsed).toBeGreaterThanOrEqual(before - 1000);
    expect(parsed).toBeLessThanOrEqual(after + 1000);
  });
});

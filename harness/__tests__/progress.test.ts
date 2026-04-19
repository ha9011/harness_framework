import { describe, it, expect } from "vitest";
import { withProgress } from "../src/progress.js";

describe("withProgress", () => {
  it("결과와 경과 시간 반환", async () => {
    const { result, elapsed } = await withProgress("test", async () => {
      await new Promise((r) => setTimeout(r, 150));
      return 42;
    });
    expect(result).toBe(42);
    expect(elapsed).toBeGreaterThan(0.1);
  });

  it("에러 발생 시 전파", async () => {
    await expect(
      withProgress("fail", async () => {
        throw new Error("boom");
      }),
    ).rejects.toThrow("boom");
  });
});

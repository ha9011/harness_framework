import { describe, it, expect } from "vitest";
import { join } from "node:path";
import { mkdtempSync, readFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { z } from "zod";
import { readJson, writeJson } from "../src/json-io.js";

function tmpFile(name: string): string {
  const dir = mkdtempSync(join(tmpdir(), "harness-test-"));
  return join(dir, name);
}

describe("writeJson + readJson", () => {
  it("라운드트립", () => {
    const path = tmpFile("test.json");
    const data = { key: "값", nested: [1, 2, 3] };
    const schema = z.object({ key: z.string(), nested: z.array(z.number()) });

    writeJson(path, data);
    const loaded = readJson(path, schema);
    expect(loaded).toEqual(data);
  });

  it("한글 유지 (ASCII 이스케이프 없음)", () => {
    const path = tmpFile("korean.json");
    writeJson(path, { 한글: "테스트" });
    const raw = readFileSync(path, "utf-8");
    expect(raw).toContain("한글");
    expect(raw).not.toContain("\\u");
  });

  it("indent 적용", () => {
    const path = tmpFile("indent.json");
    writeJson(path, { a: 1 });
    const raw = readFileSync(path, "utf-8");
    expect(raw).toContain("\n");
  });
});

describe("readJson — 스키마 검증", () => {
  it("유효 데이터 통과", () => {
    const path = tmpFile("valid.json");
    writeJson(path, { name: "test", count: 5 });
    const schema = z.object({ name: z.string(), count: z.number() });
    expect(readJson(path, schema)).toEqual({ name: "test", count: 5 });
  });

  it("무효 데이터 시 ZodError", () => {
    const path = tmpFile("invalid.json");
    writeJson(path, { name: 123 });
    const schema = z.object({ name: z.string() });
    expect(() => readJson(path, schema)).toThrow();
  });

  it("존재하지 않는 파일 시 에러", () => {
    const schema = z.object({});
    expect(() => readJson("/tmp/nonexistent_harness.json", schema)).toThrow();
  });
});


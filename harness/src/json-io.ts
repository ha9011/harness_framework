import { readFileSync, writeFileSync } from "node:fs";
import type { ZodType } from "zod";

/**
 * JSON 파일을 읽고 Zod 스키마로 검증한다.
 * 검증 실패 시 ZodError를 throw한다.
 */
export function readJson<T>(filePath: string, schema: ZodType<T>): T {
  const raw = readFileSync(filePath, "utf-8");
  const data = JSON.parse(raw);
  return schema.parse(data);
}

/**
 * 데이터를 JSON 파일로 저장한다.
 * indent=2, 한글 유지 (ensure_ascii=false 동등).
 */
export function writeJson(filePath: string, data: unknown): void {
  const content = JSON.stringify(data, null, 2);
  writeFileSync(filePath, content, "utf-8");
}

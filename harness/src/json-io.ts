/**
 * @file json-io.ts
 * @description
 *   JSON 파일을 안전하게 읽고 쓰는 "파일 관리인".
 *   읽을 때 Zod 스키마로 데이터 형식을 자동 검증해서,
 *   잘못된 데이터가 들어오면 바로 에러를 던진다.
 *   index.json이나 step-output.json을 다룰 때 사용된다.
 *
 * @see schemas.ts  - 검증에 사용하는 Zod 스키마 정의
 * @see executor.ts - 스텝 실행 중 index.json 읽기/쓰기에 사용
 */
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

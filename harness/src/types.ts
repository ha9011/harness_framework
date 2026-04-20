/**
 * @file types.ts
 * @description
 *   schemas.ts의 설계도에서 TypeScript 타입을 자동 생성하는 "타입 공장".
 *   z.infer<>를 사용해 Zod 스키마 → TS 타입으로 변환한다.
 *   이렇게 하면 런타임 검증(schemas.ts)과 컴파일 타임 타입이
 *   항상 같은 모양을 유지하게 된다 (한 곳만 수정하면 양쪽 다 반영).
 *
 * @see schemas.ts - 이 파일의 원본이 되는 Zod 스키마 정의
 */
import type { z } from "zod";
import type {
  StepStatusSchema,
  StepSchema,
  PendingStepSchema,
  CompletedStepSchema,
  ErrorStepSchema,
  BlockedStepSchema,
  PhaseIndexSchema,
  MutablePhaseIndexSchema,
  TopPhaseEntrySchema,
  TopIndexSchema,
  StepOutputSchema,
} from "./schemas.js";

export type StepStatus = z.infer<typeof StepStatusSchema>;
export type Step = z.infer<typeof StepSchema>;
export type PendingStep = z.infer<typeof PendingStepSchema>;
export type CompletedStep = z.infer<typeof CompletedStepSchema>;
export type ErrorStep = z.infer<typeof ErrorStepSchema>;
export type BlockedStep = z.infer<typeof BlockedStepSchema>;
export type PhaseIndex = z.infer<typeof PhaseIndexSchema>;
export type MutablePhaseIndex = z.infer<typeof MutablePhaseIndexSchema>;
export type TopPhaseEntry = z.infer<typeof TopPhaseEntrySchema>;
export type TopIndex = z.infer<typeof TopIndexSchema>;
export type StepOutput = z.infer<typeof StepOutputSchema>;

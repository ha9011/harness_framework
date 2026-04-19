import { z } from "zod";

// --- Step 상태 ---

export const StepStatusSchema = z.enum([
  "pending",
  "completed",
  "error",
  "blocked",
]);

// --- Step 객체 (discriminatedUnion) ---

const StepBaseFields = {
  step: z.number().int().nonnegative(),
  name: z.string().min(1),
  started_at: z.string().optional(),
};

export const PendingStepSchema = z.object({
  ...StepBaseFields,
  status: z.literal("pending"),
});

export const CompletedStepSchema = z.object({
  ...StepBaseFields,
  status: z.literal("completed"),
  summary: z.string().min(1),
  completed_at: z.string().optional(),
});

export const ErrorStepSchema = z.object({
  ...StepBaseFields,
  status: z.literal("error"),
  error_message: z.string().min(1),
  failed_at: z.string().optional(),
});

export const BlockedStepSchema = z.object({
  ...StepBaseFields,
  status: z.literal("blocked"),
  blocked_reason: z.string().min(1),
  blocked_at: z.string().optional(),
});

export const StepSchema = z.discriminatedUnion("status", [
  PendingStepSchema,
  CompletedStepSchema,
  ErrorStepSchema,
  BlockedStepSchema,
]);

// --- Phase index.json ---

export const PhaseIndexSchema = z.object({
  project: z.string().min(1),
  phase: z.string().min(1),
  steps: z.array(StepSchema),
  created_at: z.string().optional(),
  completed_at: z.string().optional(),
});

// --- Top-level phases/index.json ---

export const TopPhaseEntrySchema = z.object({
  dir: z.string().min(1),
  status: z.enum(["pending", "completed", "error", "blocked"]),
  completed_at: z.string().optional(),
  failed_at: z.string().optional(),
  blocked_at: z.string().optional(),
});

export const TopIndexSchema = z.object({
  phases: z.array(TopPhaseEntrySchema),
});

// --- 쓰기용 느슨한 Phase index (status 전환 중간 상태 허용) ---

export const MutablePhaseIndexSchema = z.object({
  project: z.string().min(1),
  phase: z.string().min(1),
  steps: z.array(z.object({
    step: z.number().int().nonnegative(),
    name: z.string().min(1),
    status: StepStatusSchema,
    started_at: z.string().optional(),
    summary: z.string().optional(),
    completed_at: z.string().optional(),
    error_message: z.string().optional(),
    failed_at: z.string().optional(),
    blocked_reason: z.string().optional(),
    blocked_at: z.string().optional(),
  })),
  created_at: z.string().optional(),
  completed_at: z.string().optional(),
});

// --- Claude 실행 출력 ---

export const StepOutputSchema = z.object({
  step: z.number().int().nonnegative(),
  name: z.string().min(1),
  exitCode: z.number().int(),
  stdout: z.string(),
  stderr: z.string(),
});

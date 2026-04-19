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

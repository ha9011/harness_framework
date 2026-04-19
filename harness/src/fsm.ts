import type { StepStatus } from "./types.js";

// --- 이벤트 타입 ---

export type StepEvent = "complete" | "fail" | "block" | "retry";

// --- 전이 페이로드 ---

export interface TransitionPayload {
  summary?: string;
  error_message?: string;
  blocked_reason?: string;
}

// --- 에러 ---

export class InvalidTransitionError extends Error {
  constructor(
    public readonly from: string,
    public readonly event: string,
  ) {
    super(`잘못된 상태 전이: ${from} --[${event}]--> ???`);
    this.name = "InvalidTransitionError";
  }
}

// --- Step 상태 머신 ---

const STEP_TRANSITIONS: Record<StepStatus, Partial<Record<StepEvent, StepStatus>>> = {
  pending: { complete: "completed", fail: "error", block: "blocked" },
  completed: {},
  error: { retry: "pending" },
  blocked: { retry: "pending" },
};

export class StepFSM {
  private _current: StepStatus;

  constructor(initial: StepStatus) {
    this._current = initial;
  }

  get current(): StepStatus {
    return this._current;
  }

  transition(event: StepEvent, payload?: TransitionPayload): StepStatus {
    const next = STEP_TRANSITIONS[this._current]?.[event];
    if (!next) {
      throw new InvalidTransitionError(this._current, event);
    }
    this._runGuards(event, payload);
    this._current = next;
    return next;
  }

  private _runGuards(event: StepEvent, payload?: TransitionPayload): void {
    if (event === "complete" && !payload?.summary) {
      throw new Error("complete 전이에는 summary가 필요합니다");
    }
    if (event === "fail" && !payload?.error_message) {
      throw new Error("fail 전이에는 error_message가 필요합니다");
    }
    if (event === "block" && !payload?.blocked_reason) {
      throw new Error("block 전이에는 blocked_reason이 필요합니다");
    }
  }
}


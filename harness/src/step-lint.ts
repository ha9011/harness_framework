export const STEP_LAYERS = [
  "domain",
  "service",
  "controller",
  "external-client",
  "frontend-view",
  "integration-hardening",
] as const;

const REQUIRED_FIELDS = [
  "Capability",
  "Layer",
  "Write Scope",
  "Out of Scope",
  "Critical Gates",
] as const;

type StepLayer = typeof STEP_LAYERS[number];
type StepContractField = typeof REQUIRED_FIELDS[number];

export interface StepLintInput {
  stepNum: number;
  content: string;
}

export interface StepLintResult {
  ok: boolean;
  errors: string[];
}

export function lintStepContract(input: StepLintInput): StepLintResult {
  const errors: string[] = [];
  const contract = parseStepContract(input.content);

  if (!contract) {
    return {
      ok: false,
      errors: [`Step ${input.stepNum}: missing Step Contract section`],
    };
  }

  for (const field of REQUIRED_FIELDS) {
    if (!contract[field]) {
      errors.push(`Step ${input.stepNum}: Step Contract missing required field: ${field}`);
    }
  }

  const layer = contract.Layer;
  if (layer && !isAllowedLayer(layer)) {
    errors.push(`Step ${input.stepNum}: Layer must be one of ${STEP_LAYERS.join(", ")}`);
  }

  const writeScope = contract["Write Scope"];
  if (writeScope !== undefined && writeScope.trim().length === 0) {
    errors.push(`Step ${input.stepNum}: Write Scope must not be empty`);
  }

  if (writeScope && layer) {
    const hasBackend = containsPathSegment(writeScope, "backend");
    const hasFrontend = containsPathSegment(writeScope, "frontend");
    if (hasBackend && hasFrontend && layer !== "integration-hardening") {
      errors.push(
        `Step ${input.stepNum}: Write Scope must not include backend and frontend together unless Layer is integration-hardening`,
      );
    }
  }

  const criticalGates = contract["Critical Gates"];
  if (layer === "external-client" && criticalGates && !hasFakeProviderGate(criticalGates)) {
    errors.push(
      `Step ${input.stepNum}: external-client Critical Gates must include a fake provider or fake server test`,
    );
  }

  return { ok: errors.length === 0, errors };
}

function parseStepContract(content: string): Partial<Record<StepContractField, string>> | undefined {
  const lines = content.split(/\r?\n/);
  const start = lines.findIndex((line) => line.trim() === "## Step Contract");
  if (start === -1) return undefined;

  const bodyLines: string[] = [];
  for (const line of lines.slice(start + 1)) {
    if (line.startsWith("## ")) break;
    bodyLines.push(line);
  }

  const body = bodyLines.join("\n");

  const contract: Partial<Record<StepContractField, string>> = {};
  for (const field of REQUIRED_FIELDS) {
    const escapedField = field.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const fieldMatch = body.match(new RegExp(`^[ \\t]*(?:[-*][ \\t]*)?${escapedField}[ \\t]*:[ \\t]*(.*)$`, "im"));
    if (fieldMatch) {
      contract[field] = fieldMatch[1]?.trim() ?? "";
    }
  }

  return contract;
}

function isAllowedLayer(layer: string): layer is StepLayer {
  return STEP_LAYERS.includes(layer as StepLayer);
}

function containsPathSegment(value: string, segment: "backend" | "frontend"): boolean {
  return new RegExp(`(^|[\\s,./])${segment}([\\s,./]|$)`, "i").test(value);
}

function hasFakeProviderGate(value: string): boolean {
  const normalized = value.toLowerCase();
  const mentionsFakeProvider = /\bfake\s+(provider|server)\b/.test(normalized);
  const mentionsTest = /\b(test|spec|vitest|jest|pytest|fake)\b/.test(normalized);
  return mentionsFakeProvider && mentionsTest;
}

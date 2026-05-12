import { describe, it, expect } from "vitest";
import { lintStepContract } from "../src/step-lint.js";

const validStep = `# Step 0: weather-client

## Step Contract

- Capability: weather forecast client
- Layer: external-client
- Write Scope: backend/weather/client.ts, backend/weather/client.test.ts
- Out of Scope: controllers, frontend UI, database schema
- Critical Gates: npm test -- backend/weather/client.test.ts fake provider request auth response parsing failure handling

## 작업

Implement the client.
`;

describe("lintStepContract", () => {
  it("accepts a valid external-client contract with fake provider gates", () => {
    const result = lintStepContract({ stepNum: 0, content: validStep });

    expect(result.ok).toBe(true);
  });

  it("rejects missing Step Contract fields", () => {
    const result = lintStepContract({
      stepNum: 0,
      content: validStep.replace("- Out of Scope: controllers, frontend UI, database schema\n", ""),
    });

    expect(result.ok).toBe(false);
    expect(result.errors).toContain("Step 0: Step Contract missing required field: Out of Scope");
  });

  it("rejects unknown layer values", () => {
    const result = lintStepContract({
      stepNum: 0,
      content: validStep.replace("Layer: external-client", "Layer: api"),
    });

    expect(result.ok).toBe(false);
    expect(result.errors).toContain(
      "Step 0: Layer must be one of domain, service, controller, external-client, frontend-view, integration-hardening",
    );
  });

  it("rejects empty write scope", () => {
    const result = lintStepContract({
      stepNum: 0,
      content: validStep.replace(
        "Write Scope: backend/weather/client.ts, backend/weather/client.test.ts",
        "Write Scope:",
      ),
    });

    expect(result.ok).toBe(false);
    expect(result.errors).toContain("Step 0: Write Scope must not be empty");
  });

  it("rejects backend and frontend write scope outside integration-hardening", () => {
    const result = lintStepContract({
      stepNum: 0,
      content: validStep.replace(
        "Write Scope: backend/weather/client.ts, backend/weather/client.test.ts",
        "Write Scope: backend/weather/service.ts, frontend/src/WeatherView.tsx",
      ),
    });

    expect(result.ok).toBe(false);
    expect(result.errors).toContain(
      "Step 0: Write Scope must not include backend and frontend together unless Layer is integration-hardening",
    );
  });

  it("allows backend and frontend write scope for integration-hardening", () => {
    const result = lintStepContract({
      stepNum: 0,
      content: validStep
        .replace("Layer: external-client", "Layer: integration-hardening")
        .replace(
          "Write Scope: backend/weather/client.ts, backend/weather/client.test.ts",
          "Write Scope: backend/weather/service.ts, frontend/src/WeatherView.tsx",
        ),
    });

    expect(result.ok).toBe(true);
  });

  it("rejects external-client gates without fake provider or fake server tests", () => {
    const result = lintStepContract({
      stepNum: 0,
      content: validStep.replace(
        "Critical Gates: npm test -- backend/weather/client.test.ts fake provider request auth response parsing failure handling",
        "Critical Gates: npm test -- backend/weather/client.test.ts",
      ),
    });

    expect(result.ok).toBe(false);
    expect(result.errors).toContain(
      "Step 0: external-client Critical Gates must include a fake provider or fake server test",
    );
  });
});

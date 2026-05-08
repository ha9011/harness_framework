import { render, screen } from "@testing-library/react";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { Surface } from "@/app/components/ui/surface";
import { cafeColors } from "@/app/lib/design-tokens";

describe("Cozy Cafe UI foundation", () => {
  it("exports design tokens used by shared surfaces", () => {
    render(
      <Surface data-testid="surface">
        <Chip variant="sage">Sage</Chip>
        <Button>Start</Button>
      </Surface>,
    );

    expect(cafeColors.bg).toBe("#FAF6F0");
    expect(screen.getByTestId("surface")).toHaveStyle({ borderColor: cafeColors.hairline });
    expect(screen.getByRole("button", { name: "Start" })).toHaveClass("bg-cafe-latte");
    expect(screen.getByText("Sage")).toHaveClass("bg-cafe-sage-bg");
  });
});

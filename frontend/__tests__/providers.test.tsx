import { useQueryClient } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { AppProviders } from "@/app/components/providers/app-providers";
import { useShell } from "@/app/components/providers/shell-provider";

function ProviderProbe() {
  const queryClient = useQueryClient();
  const shell = useShell();

  return (
    <div>
      <span data-testid="query-client">{String(Boolean(queryClient))}</span>
      <span data-testid="app-name">{shell.appName}</span>
      <span data-testid="nav-count">{shell.navItems.length}</span>
    </div>
  );
}

describe("AppProviders", () => {
  it("connects TanStack Query and the shell context", () => {
    render(
      <AppProviders>
        <ProviderProbe />
      </AppProviders>,
    );

    expect(screen.getByTestId("query-client")).toHaveTextContent("true");
    expect(screen.getByTestId("app-name")).toHaveTextContent("영어 패턴 학습기");
    expect(screen.getByTestId("nav-count")).toHaveTextContent("5");
  });
});

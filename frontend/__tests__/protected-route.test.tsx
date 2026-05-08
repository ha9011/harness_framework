import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { AppShell } from "@/app/components/app-shell";
import { AppProviders } from "@/app/components/providers/app-providers";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

const navigation = vi.hoisted(() => ({
  pathname: "/",
  replace: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  usePathname: () => navigation.pathname,
  useRouter: () => ({
    replace: navigation.replace,
  }),
}));

describe("protected-route", () => {
  beforeEach(() => {
    navigation.pathname = "/";
    navigation.replace.mockReset();
  });

  it("redirects unauthenticated users away from protected screens", async () => {
    render(
      <AppProviders>
        <AppShell>
          <div>보호된 홈</div>
        </AppShell>
      </AppProviders>,
    );

    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith("/login"));
    expect(screen.queryByText("보호된 홈")).not.toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "주요 화면" })).not.toBeInTheDocument();
  });

  it("shows the Cozy Cafe shell and bottom navigation after authentication", async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/auth/me`, () => {
        return HttpResponse.json({
          id: 1,
          email: "hyejin@example.com",
          nickname: "혜진",
        });
      }),
    );

    render(
      <AppProviders>
        <AppShell>
          <div>보호된 홈</div>
        </AppShell>
      </AppProviders>,
    );

    expect(await screen.findByText("보호된 홈")).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "주요 화면" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "홈" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "복습" })).toHaveAttribute("href", "/review");
  });

  it("logs out from the protected shell and redirects to login", async () => {
    const user = userEvent.setup();
    let logoutCalled = false;

    server.use(
      http.get(`${API_BASE_URL}/api/auth/me`, () => {
        return HttpResponse.json({
          id: 1,
          email: "hyejin@example.com",
          nickname: "혜진",
        });
      }),
      http.post(`${API_BASE_URL}/api/auth/logout`, () => {
        logoutCalled = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    render(
      <AppProviders>
        <AppShell>
          <div>보호된 홈</div>
        </AppShell>
      </AppProviders>,
    );

    await screen.findByText("보호된 홈");
    await user.click(screen.getByRole("button", { name: "로그아웃" }));

    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith("/login"));
    expect(logoutCalled).toBe(true);
  });

  it("keeps auth pages outside of the protected app shell", async () => {
    navigation.pathname = "/login";

    render(
      <AppProviders>
        <AppShell>
          <div>로그인 화면</div>
        </AppShell>
      </AppProviders>,
    );

    expect(await screen.findByText("로그인 화면")).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "주요 화면" })).not.toBeInTheDocument();
    expect(navigation.replace).not.toHaveBeenCalledWith("/login");
  });
});

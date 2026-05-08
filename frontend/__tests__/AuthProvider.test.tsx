import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { AppProviders } from "@/app/components/providers/app-providers";
import { useAuth } from "@/app/components/providers/auth-provider";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

const currentUser = {
  id: 1,
  email: "hyejin@example.com",
  nickname: "혜진",
};

function AuthProbe() {
  const { logout, status, user } = useAuth();

  return (
    <div>
      <span data-testid="auth-status">{status}</span>
      <span data-testid="auth-nickname">{user?.nickname ?? "none"}</span>
      <button type="button" onClick={() => void logout()}>
        로그아웃
      </button>
    </div>
  );
}

describe("AuthProvider", () => {
  it("checks the current user on startup with cookie credentials", async () => {
    let credentials: RequestCredentials | undefined;

    server.use(
      http.get(`${API_BASE_URL}/api/auth/me`, ({ request }) => {
        credentials = request.credentials;
        return HttpResponse.json(currentUser);
      }),
    );

    render(
      <AppProviders>
        <AuthProbe />
      </AppProviders>,
    );

    await waitFor(() => expect(screen.getByTestId("auth-status")).toHaveTextContent("authenticated"));
    expect(screen.getByTestId("auth-nickname")).toHaveTextContent("혜진");
    expect(credentials).toBe("include");
  });

  it("logs out through the backend and clears the authenticated user", async () => {
    const user = userEvent.setup();
    let logoutCalled = false;

    server.use(
      http.get(`${API_BASE_URL}/api/auth/me`, () => {
        return HttpResponse.json(currentUser);
      }),
      http.post(`${API_BASE_URL}/api/auth/logout`, () => {
        logoutCalled = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    render(
      <AppProviders>
        <AuthProbe />
      </AppProviders>,
    );

    await waitFor(() => expect(screen.getByTestId("auth-status")).toHaveTextContent("authenticated"));

    await user.click(screen.getByRole("button", { name: "로그아웃" }));

    await waitFor(() => expect(screen.getByTestId("auth-status")).toHaveTextContent("unauthenticated"));
    expect(screen.getByTestId("auth-nickname")).toHaveTextContent("none");
    expect(logoutCalled).toBe(true);
  });
});

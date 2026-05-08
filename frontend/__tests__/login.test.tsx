import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import LoginPage from "@/app/login/page";
import { AppProviders } from "@/app/components/providers/app-providers";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

const navigation = vi.hoisted(() => ({
  push: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: navigation.push,
  }),
}));

describe("login", () => {
  beforeEach(() => {
    navigation.push.mockReset();
  });

  it("submits email and password, then redirects home", async () => {
    const user = userEvent.setup();
    let credentials: RequestCredentials | undefined;
    let requestBody: unknown;

    server.use(
      http.post(`${API_BASE_URL}/api/auth/login`, async ({ request }) => {
        credentials = request.credentials;
        requestBody = await request.json();

        return HttpResponse.json({
          id: 1,
          email: "hyejin@example.com",
          nickname: "혜진",
        });
      }),
    );

    render(
      <AppProviders>
        <LoginPage />
      </AppProviders>,
    );

    await user.type(screen.getByLabelText("이메일"), "hyejin@example.com");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => expect(navigation.push).toHaveBeenCalledWith("/"));
    expect(credentials).toBe("include");
    expect(requestBody).toEqual({
      email: "hyejin@example.com",
      password: "password123",
    });
  });

  it("shows backend login failures without exposing field-specific hints", async () => {
    const user = userEvent.setup();

    server.use(
      http.post(`${API_BASE_URL}/api/auth/login`, () => {
        return HttpResponse.json(
          { error: "UNAUTHORIZED", message: "이메일 또는 비밀번호가 올바르지 않습니다" },
          { status: 401 },
        );
      }),
    );

    render(
      <AppProviders>
        <LoginPage />
      </AppProviders>,
    );

    await user.type(screen.getByLabelText("이메일"), "hyejin@example.com");
    await user.type(screen.getByLabelText("비밀번호"), "wrong-password");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    expect(await screen.findByText("이메일 또는 비밀번호가 올바르지 않습니다")).toBeInTheDocument();
    expect(navigation.push).not.toHaveBeenCalled();
  });

  it("validates required form fields before calling the server", async () => {
    const user = userEvent.setup();
    let loginCalled = false;

    server.use(
      http.post(`${API_BASE_URL}/api/auth/login`, () => {
        loginCalled = true;
        return HttpResponse.json({ id: 1, email: "hyejin@example.com", nickname: "혜진" });
      }),
    );

    render(
      <AppProviders>
        <LoginPage />
      </AppProviders>,
    );

    await user.click(screen.getByRole("button", { name: "로그인" }));

    expect(await screen.findByText("이메일을 입력해주세요")).toBeInTheDocument();
    expect(screen.getByText("비밀번호를 입력해주세요")).toBeInTheDocument();
    expect(loginCalled).toBe(false);
  });
});

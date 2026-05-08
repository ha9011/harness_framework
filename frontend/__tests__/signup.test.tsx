import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import SignupPage from "@/app/signup/page";
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

describe("signup", () => {
  beforeEach(() => {
    navigation.push.mockReset();
  });

  it("validates the minimum password length before signup", async () => {
    const user = userEvent.setup();
    let signupCalled = false;

    server.use(
      http.post(`${API_BASE_URL}/api/auth/signup`, () => {
        signupCalled = true;
        return HttpResponse.json({ id: 1, email: "hyejin@example.com", nickname: "혜진" }, { status: 201 });
      }),
    );

    render(
      <AppProviders>
        <SignupPage />
      </AppProviders>,
    );

    await user.type(screen.getByLabelText("이메일"), "hyejin@example.com");
    await user.type(screen.getByLabelText("닉네임"), "혜진");
    await user.type(screen.getByLabelText("비밀번호"), "short");
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    expect(await screen.findByText("비밀번호는 8자 이상이어야 합니다")).toBeInTheDocument();
    expect(signupCalled).toBe(false);
  });

  it("submits signup and redirects home", async () => {
    const user = userEvent.setup();
    let requestBody: unknown;

    server.use(
      http.post(`${API_BASE_URL}/api/auth/signup`, async ({ request }) => {
        requestBody = await request.json();

        return HttpResponse.json(
          {
            id: 1,
            email: "hyejin@example.com",
            nickname: "혜진",
          },
          { status: 201 },
        );
      }),
    );

    render(
      <AppProviders>
        <SignupPage />
      </AppProviders>,
    );

    await user.type(screen.getByLabelText("이메일"), "hyejin@example.com");
    await user.type(screen.getByLabelText("닉네임"), "혜진");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(() => expect(navigation.push).toHaveBeenCalledWith("/"));
    expect(requestBody).toEqual({
      email: "hyejin@example.com",
      nickname: "혜진",
      password: "password123",
    });
  });

  it("shows backend signup errors", async () => {
    const user = userEvent.setup();

    server.use(
      http.post(`${API_BASE_URL}/api/auth/signup`, () => {
        return HttpResponse.json(
          { error: "DUPLICATE", message: "이미 가입된 이메일입니다" },
          { status: 409 },
        );
      }),
    );

    render(
      <AppProviders>
        <SignupPage />
      </AppProviders>,
    );

    await user.type(screen.getByLabelText("이메일"), "hyejin@example.com");
    await user.type(screen.getByLabelText("닉네임"), "혜진");
    await user.type(screen.getByLabelText("비밀번호"), "password123");
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    expect(await screen.findByText("이미 가입된 이메일입니다")).toBeInTheDocument();
    expect(navigation.push).not.toHaveBeenCalled();
  });
});

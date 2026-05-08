import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import SettingsPage from "@/app/settings/page";
import { AppProviders } from "@/app/components/providers/app-providers";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

const navigation = vi.hoisted(() => ({
  replace: vi.fn(),
}));

const paragraphContaining = (text: string) => (_content: string, element: Element | null) =>
  element?.tagName === "P" && (element.textContent?.includes(text) ?? false);

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    replace: navigation.replace,
  }),
}));

describe("settings", () => {
  beforeEach(() => {
    navigation.replace.mockReset();
  });

  it("loads settings and saves only selectable review counts", async () => {
    const user = userEvent.setup();
    let requestBody: unknown;

    server.use(
      http.get(`${API_BASE_URL}/api/settings`, () => HttpResponse.json({ dailyReviewCount: 20 })),
      http.put(`${API_BASE_URL}/api/settings/daily_review_count`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({ dailyReviewCount: 30 });
      }),
    );

    render(
      <AppProviders>
        <SettingsPage />
      </AppProviders>,
    );

    expect(await screen.findByText(paragraphContaining("하루 총 60장"))).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "30" }));
    expect(screen.getByText(paragraphContaining("하루 총 90장"))).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => expect(requestBody).toEqual({ value: "30" }));
    expect(await screen.findByText("설정을 저장했습니다")).toBeInTheDocument();
  });
});

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import HistoryPage from "@/app/history/page";
import { AppProviders } from "@/app/components/providers/app-providers";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

describe("history", () => {
  it("renders study records and requests the next page", async () => {
    const user = userEvent.setup();
    const requestedPages: string[] = [];

    server.use(
      http.get(`${API_BASE_URL}/api/study-records`, ({ request }) => {
        const url = new URL(request.url);
        requestedPages.push(url.searchParams.get("page") ?? "0");
        return HttpResponse.json({
          content: [
            {
              id: Number(url.searchParams.get("page") ?? 0) + 1,
              studyDate: "2026-05-09",
              dayNumber: Number(url.searchParams.get("page") ?? 0) + 1,
              items: [{ type: "PATTERN", id: 1, name: "I'm afraid that..." }],
            },
          ],
          totalElements: 2,
          totalPages: 2,
          page: Number(url.searchParams.get("page") ?? 0),
          size: 10,
        });
      }),
    );

    render(
      <AppProviders>
        <HistoryPage />
      </AppProviders>,
    );

    expect(await screen.findByText("Day 1")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "더 보기" }));
    expect(await screen.findByText("Day 2")).toBeInTheDocument();
    expect(requestedPages).toContain("1");
  });
});

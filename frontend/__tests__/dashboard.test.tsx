import { render, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import HomePage from "@/app/page";
import { AppProviders } from "@/app/components/providers/app-providers";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

describe("dashboard", () => {
  it("renders dashboard counts, recent records, and review CTA", async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/auth/me`, () => HttpResponse.json({ id: 1, email: "hyejin@example.com", nickname: "혜진" })),
      http.get(`${API_BASE_URL}/api/dashboard`, () =>
        HttpResponse.json({
          wordCount: 12,
          patternCount: 4,
          sentenceCount: 30,
          streak: 5,
          todayReviewRemaining: { word: 3, pattern: 2, sentence: 1 },
          recentStudyRecords: [
            {
              id: 1,
              studyDate: "2026-05-09",
              dayNumber: 7,
              items: [{ type: "WORD", id: 10, name: "make a bed" }],
            },
          ],
        }),
      ),
    );

    render(
      <AppProviders>
        <HomePage />
      </AppProviders>,
    );

    expect(await screen.findByText(/혜진님/)).toBeInTheDocument();
    expect(screen.getByText("6장")).toBeInTheDocument();
    expect(screen.getByText("make a bed")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /오늘의 복습 시작/ })).toHaveAttribute("href", "/review");
  });
});

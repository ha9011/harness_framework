import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import ReviewPage from "@/app/review/page";
import { AppProviders } from "@/app/components/providers/app-providers";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

describe("review", () => {
  it("loads a deck, flips a card, records a result, and requests additional review with exclude", async () => {
    const user = userEvent.setup();
    let recordBody: unknown;
    const requestedExcludes: string[] = [];

    server.use(
      http.get(`${API_BASE_URL}/api/reviews/today`, ({ request }) => {
        const url = new URL(request.url);
        requestedExcludes.push(url.searchParams.get("exclude") ?? "");
        if (url.searchParams.get("exclude") === "1") {
          return HttpResponse.json([]);
        }
        return HttpResponse.json([
          {
            reviewItemId: 1,
            itemType: "WORD",
            direction: "RECOGNITION",
            front: { text: "make a bed" },
            back: {
              meaning: "침대를 정리하다",
              pronunciation: "/meik/",
              tip: "make the bed도 같은 의미",
              examples: ["She makes her bed every morning."],
            },
          },
        ]);
      }),
      http.post(`${API_BASE_URL}/api/reviews/1`, async ({ request }) => {
        recordBody = await request.json();
        return HttpResponse.json({ nextReviewDate: "2026-05-10", intervalDays: 1 });
      }),
    );

    render(
      <AppProviders>
        <ReviewPage />
      </AppProviders>,
    );

    expect(await screen.findByText("make a bed")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "복습 카드 뒤집기" }));
    expect(await screen.findByText("침대를 정리하다")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Easy" }));

    await waitFor(() => expect(recordBody).toEqual({ result: "EASY" }));
    expect(await screen.findByText("이 덱을 완료했습니다")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "추가 복습" }));
    await waitFor(() => expect(requestedExcludes).toContain("1"));
  });

  it("replays completed cards without recording another SM-2 result", async () => {
    const user = userEvent.setup();
    let recordCount = 0;

    server.use(
      http.get(`${API_BASE_URL}/api/reviews/today`, () =>
        HttpResponse.json([
          {
            reviewItemId: 1,
            itemType: "SENTENCE",
            direction: "RECOGNITION",
            front: { text: "I'm afraid that I forgot to make my bed.", situation: "엄마에게 전화가 온 상황" },
            back: { translation: "침대 정리를 깜빡했어요.", pattern: "I'm afraid that...", words: ["make a bed"] },
          },
        ]),
      ),
      http.post(`${API_BASE_URL}/api/reviews/1`, () => {
        recordCount += 1;
        return HttpResponse.json({ nextReviewDate: "2026-05-10", intervalDays: 1 });
      }),
    );

    render(
      <AppProviders>
        <ReviewPage />
      </AppProviders>,
    );

    expect(await screen.findByText("I'm afraid that I forgot to make my bed.")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Easy" }));
    await screen.findByText("이 덱을 완료했습니다");
    await user.click(screen.getByRole("button", { name: /처음부터 다시/ }));
    await user.click(screen.getByRole("button", { name: "다음 카드" }));

    expect(recordCount).toBe(1);
  });
});

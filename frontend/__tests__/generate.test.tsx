import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import GeneratePage from "@/app/generate/page";
import { AppProviders } from "@/app/components/providers/app-providers";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

describe("generate", () => {
  it("submits selected level and count and renders generated sentences", async () => {
    const user = userEvent.setup();
    let requestBody: unknown;

    server.use(
      http.post(`${API_BASE_URL}/api/generate`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          {
            generationId: 1,
            sentences: [
              {
                id: 1,
                sentence: "I'm afraid that I forgot to make my bed.",
                translation: "유감스럽게도 침대 정리를 깜빡했어요.",
                situations: ["아침에 급하게 나온 상황"],
                level: "고등",
                pattern: { id: 1, template: "I'm afraid that..." },
                words: [{ id: 3, word: "make a bed" }],
              },
            ],
          },
          { status: 201 },
        );
      }),
    );

    render(
      <AppProviders>
        <GeneratePage />
      </AppProviders>,
    );

    await user.click(screen.getByRole("button", { name: "고등" }));
    await user.click(screen.getByRole("button", { name: "20" }));
    await user.click(screen.getByRole("button", { name: /예문 생성/ }));

    await waitFor(() => expect(requestBody).toEqual({ level: "고등", count: 20 }));
    expect(await screen.findByText("I'm afraid that I forgot to make my bed.")).toBeInTheDocument();
    expect(screen.getByText("상황: 아침에 급하게 나온 상황")).toBeInTheDocument();
  });

  it("shows mapped API errors", async () => {
    const user = userEvent.setup();

    server.use(
      http.post(`${API_BASE_URL}/api/generate`, () =>
        HttpResponse.json({ error: "NO_WORDS", message: "예문 생성에 사용할 단어가 없습니다" }, { status: 400 }),
      ),
    );

    render(
      <AppProviders>
        <GeneratePage />
      </AppProviders>,
    );

    await user.click(screen.getByRole("button", { name: /예문 생성/ }));
    expect(await screen.findByText("예문 생성에 사용할 단어가 없습니다. 먼저 단어를 등록하세요.")).toBeInTheDocument();
  });
});

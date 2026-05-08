import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import WordsPage from "@/app/words/page";
import { AppProviders } from "@/app/components/providers/app-providers";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

const word = {
  id: 1,
  word: "make a bed",
  meaning: "침대를 정리하다",
  partOfSpeech: "phrase",
  pronunciation: "/meik/",
  synonyms: null,
  tip: null,
  isImportant: false,
};

describe("words", () => {
  it("lists words, creates a single word, and toggles important", async () => {
    const user = userEvent.setup();
    let createBody: unknown;
    let importantCalled = false;

    server.use(
      http.get(`${API_BASE_URL}/api/words`, () =>
        HttpResponse.json({ content: [word], totalElements: 1, totalPages: 1, page: 0, size: 10 }),
      ),
      http.post(`${API_BASE_URL}/api/words`, async ({ request }) => {
        createBody = await request.json();
        return HttpResponse.json({ ...word, id: 2, word: "brew coffee", meaning: "커피를 내리다" }, { status: 201 });
      }),
      http.patch(`${API_BASE_URL}/api/words/1/important`, () => {
        importantCalled = true;
        return HttpResponse.json({ ...word, isImportant: true });
      }),
    );

    render(
      <AppProviders>
        <WordsPage />
      </AppProviders>,
    );

    expect(await screen.findByText("make a bed")).toBeInTheDocument();
    await user.type(screen.getByLabelText("단어"), "brew coffee");
    await user.type(screen.getByLabelText("뜻"), "커피를 내리다");
    await user.click(screen.getByRole("button", { name: "단어 저장" }));
    await waitFor(() => expect(createBody).toEqual(expect.objectContaining({ word: "brew coffee", meaning: "커피를 내리다" })));

    await user.click(screen.getByRole("button", { name: "make a bed 중요 토글" }));
    await waitFor(() => expect(importantCalled).toBe(true));
  });

  it("extracts image words and saves the confirmation list through bulk API", async () => {
    const user = userEvent.setup();
    let bulkBody: unknown;

    server.use(
      http.get(`${API_BASE_URL}/api/words`, () =>
        HttpResponse.json({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 10 }),
      ),
      http.post(`${API_BASE_URL}/api/words/extract`, () => HttpResponse.json([{ word: "drink coffee", meaning: "커피를 마시다" }])),
      http.post(`${API_BASE_URL}/api/words/bulk`, async ({ request }) => {
        bulkBody = await request.json();
        return HttpResponse.json({ saved: [word], skipped: [], enrichmentFailed: [] }, { status: 201 });
      }),
    );

    render(
      <AppProviders>
        <WordsPage />
      </AppProviders>,
    );

    await user.click(screen.getByRole("tab", { name: "이미지" }));
    await user.upload(screen.getByLabelText("단어 이미지"), new File(["image"], "words.png", { type: "image/png" }));
    expect(await screen.findByDisplayValue("drink coffee")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "추출 결과 저장" }));
    await waitFor(() => expect(bulkBody).toEqual([{ word: "drink coffee", meaning: "커피를 마시다" }]));
  });
});
